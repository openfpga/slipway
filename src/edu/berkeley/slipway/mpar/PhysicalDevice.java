package edu.berkeley.slipway.mpar;
import byucc.edif.tools.merge.*;
import byucc.edif.*;
import java.io.*;
import java.util.*;
import edu.berkeley.slipway.*;
import static edu.berkeley.slipway.mpar.MPARDemo.*;

public abstract class PhysicalDevice implements Iterable<PhysicalDevice.PhysicalNet> {

    public abstract PhysicalCell getCell(int col, int row);

    private HashSet<PhysicalNet> allPhysicalNets = new HashSet<PhysicalNet>();
    public Iterator<PhysicalNet> iterator() { return allPhysicalNets.iterator(); }

    public abstract class PhysicalCell {
        public abstract PhysicalNet getNet(String name);
        public abstract void setFunction(String type);
        public abstract void place(NetList.Node n);
    }

    public class PhysicalNet implements Iterable<PhysicalPip>, Comparable<PhysicalNet> {

        // per-par-iteration variables
        private  double      congestion = 0;
        private  int         load = 0;

        // temporary variables used during route searches
        private  double      distance = Double.MAX_VALUE;
        private  PhysicalNet backpointer = null;

        // adjacent pips
        private final HashSet<PhysicalPip> pips = new HashSet<PhysicalPip>();

        private String name;

        // logical nets currently mapped onto this physical net
        private HashSet<NetList.LogicalNet> logicalNets = new HashSet<NetList.LogicalNet>();

        public double getCongestion() { return congestion; }
        public boolean isCongested() { return load >= 2; }
        public void updateCongestion() {
            congestion = congestion * alphaParameter;
            if (isCongested()) congestion += betaParameter;
        }

        public Iterable<NetList.LogicalNet> getLogicalNets() { return logicalNets; }
        public void addLogicalNet(NetList.LogicalNet net) {
            if (logicalNets.contains(net)) return;
            logicalNets.add(net);
            load++;
            if (load >= 2) congestion += betaParameter;
            net.addPhysicalNet(this);
        }
        public void removeLogicalNet(NetList.LogicalNet net) {
            if (!logicalNets.contains(net)) return;
            logicalNets.remove(net);
            load--;
            net.removePhysicalNet(this);
        }

        /** ordering is based on distance so we can use the Java PriorityQueue class */
        public int compareTo(PhysicalNet pn) {
            double x = distance - pn.distance;
            return distance > pn.distance
                ? 1
                : distance < pn.distance
                ? -1
                : 0;
        }

        public Iterator<PhysicalPip> iterator() { return pips.iterator(); }
        public PhysicalNet(String name) {
            this.name = name;
            allPhysicalNets.add(this);
        }
        public String toString() { return name; }
        private void addPip(PhysicalPip pip) { pips.add(pip); }
        public PhysicalPip getPipFrom(PhysicalNet pn) {
            for(PhysicalPip pip : pn)
                for(PhysicalNet pn2 : pip.getDrivenNets())
                    if (pn2==this)
                        return pip;
            return null;
        }
        public void route(PhysicalNet[] dests, NetList.LogicalNet logicalNet) {
            HashSet<PhysicalNet> remainingDests = new HashSet<PhysicalNet>();
            for(PhysicalNet dest : dests) remainingDests.add(dest);

            HashSet<PhysicalNet> needsReset = new HashSet<PhysicalNet>();
            PriorityQueue<PhysicalNet> pq = new PriorityQueue<PhysicalNet>();
            needsReset.add(this);
            this.distance = 0;
            pq.add(this);

            OUTER: while(true) {
                PhysicalNet pn = pq.poll();
                if (pn==null) throw new Error("unroutable! " + this + " -> " + dests[0]);
                double frontier = pn.distance;
                for(PhysicalPip pip : pn)
                    for(PhysicalNet net : pip.getDrivenNets()) {
                        double newfrontier = frontier + pip.getCost(pn, net) + net.getCongestion();

                        // penalty for using any net already routed in this iteration (makes routing order-sensitive)
                        if (net.load >= 1) newfrontier = newfrontier + 20;

                        if (net.distance <= newfrontier) continue;
                        pq.remove(net);  // if already in there
                        net.distance = newfrontier;
                        pq.add(net);
                        needsReset.add(net);
                        net.backpointer = pn;

                        if (remainingDests.contains(net)) {
                            remainingDests.remove(net);
                            if (remainingDests.size()==0) break OUTER;
                            // Vaughn Betz style multiterminal routing: once we reach one sink, make every node on the path
                            // "distance zero" from the source.
                            for(PhysicalNet pnx = net; pnx != null; pnx = pnx.backpointer) {
                                pnx.distance = 0;
                                pq.add(pnx);
                            }
                            break;
                        }
                    }
            }

            for(PhysicalNet dest : dests)
                for(PhysicalNet pn = dest; pn != null && pn.backpointer != null; pn = pn.backpointer) {
                    pn.addLogicalNet(logicalNet);
                    pn.distance = Double.MAX_VALUE;
                    PhysicalPip pip = pn.getPipFrom(pn.backpointer);
                    pip.set(true);
                    logicalNet.addPhysicalPip(pip);
                }

            for(PhysicalNet pn : needsReset) {
                pn.distance    = Double.MAX_VALUE;
                pn.backpointer = null;
            }
        }
    }
        
    public abstract class PhysicalPip {
        private PhysicalNet   driver;
        private PhysicalNet[] driven;
        private String name;
        private double defaultCost;
        public String toString() { return name; }
        public PhysicalNet   getDriverNet()  { return driver; }
        public PhysicalNet[] getDrivenNets() { return driven; }
        public double        getCost(PhysicalNet in, PhysicalNet out) { return defaultCost; }
        public PhysicalPip(String name, PhysicalNet driver, PhysicalNet[] driven) { this(name, driver, driven, 0.05); }
        public PhysicalPip(String name, PhysicalNet driver, PhysicalNet[] driven, double defaultCost) {
            this.name = name;
            this.driver = driver;
            this.driven = driven;
            this.defaultCost = defaultCost;
            if (driver != null) driver.addPip(this);
            for(PhysicalNet pn : driven) pn.addPip(this);
        }
        public abstract void set(boolean connected);
    }
        
}
