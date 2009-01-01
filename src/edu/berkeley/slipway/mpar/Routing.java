package edu.berkeley.slipway.mpar;
import com.atmel.fpslic.*;
import java.awt.*;
import byucc.edif.tools.merge.*;
import byucc.edif.*;
import java.io.*;
import java.util.*;
import edu.berkeley.slipway.*;
import edu.berkeley.abits.*;
import com.atmel.fpslic.*;
import static com.atmel.fpslic.FpslicConstants.*;
import static edu.berkeley.slipway.mpar.PhysicalDevice.*;
import static edu.berkeley.slipway.mpar.NetList.*;


// FIXME: make sure everything is O(design), not O(device)
public class Routing {

    public final Placement placement;
    public final NetList netlist;
    public final PhysicalDevice pd;

    private HashSet<Route> removed = new HashSet<Route>();
    private HashSet<Route> added   = new HashSet<Route>();
    public void checkpoint() {
        removed.clear();
        added.clear();
    }
    public void rollback() {
        for (Route r : added) r.remove(false);
        for (Route r : removed) r.add();
    }

    public Routing(Placement p) {
        this.placement = p;
        this.netlist = p.netlist;
        this.pd = p.pd;
        congestion = new double[pd.getNumPhysicalNets()];
        load       = new int[pd.getNumPhysicalNets()];
    }

    private double[] congestion;
    private int[]    load;
    public HashMap<NetList.LogicalNet, Route> routes =
        new HashMap<NetList.LogicalNet, Route>();

    private class Route {
        private NetList.LogicalNet logicalNet;
        private HashSet<PhysicalDevice.PhysicalNet> nets;
        private HashSet<PhysicalDevice.PhysicalPip> pips;
        double timingpenalty;
        public Route(NetList.LogicalNet logicalNet,
                     HashSet<PhysicalDevice.PhysicalNet> nets,
                     HashSet<PhysicalDevice.PhysicalPip> pips
                     ) {
            this.logicalNet = logicalNet;
            this.nets = nets;
            this.pips = pips;
        }
        public void add() {
            for(PhysicalDevice.PhysicalNet net : nets)
                load[net.idx]++;
            added.add(this);
            routes.put(logicalNet, this);
        }
        public void remove() { remove(true); }
        public void remove(boolean note) {
            for(PhysicalDevice.PhysicalNet net : nets)
                load[net.idx]--;
            if (note) removed.add(this);
            routes.remove(logicalNet);
        }
    }

    public void setPips(boolean on) {
        for (Route r : routes.values())
            for (PhysicalDevice.PhysicalPip pip : r.pips)
                pip.set(on);
    }

    public int getLoad(PhysicalDevice.PhysicalNet pn) { return load[pn.idx]; }
    public double getCongestion(PhysicalDevice.PhysicalNet pn) { return congestion[pn.idx]; }

    public void routeAll() throws RoutingFailedException {
        for(NetList.LogicalNet net : netlist.nets)
            route(net);
    }
    public void reRouteAll() throws RoutingFailedException {
        for(NetList.LogicalNet net : netlist.nets) {
            unroute(net);
            route(net);
        }
    }
    public void unRouteAll() {
        for(NetList.LogicalNet signal : netlist.getLogicalNets())
            unroute(signal);
    }


    public void unroute(NetList.Node node) {
        if (node==null) return;
        for(NetList.Node.Port p : node)
            unroute(p.net);
    }
    /*
    public void unroute(PhysicalDevice.PhysicalNet net) {
        while(netToSignals.size(net) > 0)
            unroute(netToSignals.getAll(net).iterator().next());
    }
    */
    public void unroute(NetList.LogicalNet signal) {
        if (signal==null) return;
        Route r = routes.get(signal);
        if (r != null) r.remove();
    }

    public void unrouteOverloaded() {
        /*
          FIXME
        for(PhysicalDevice.PhysicalNet pn : pd)
            if (getLoad(pn) > 1)
                unroute(pn);
        */
    }

    public void updateCongestion(double alphaParameter, double betaParameter) {
        for(PhysicalDevice.PhysicalNet net : pd) {
            double c = getCongestion(net);
            c = c * alphaParameter;
            if (getLoad(net) > 1) c += betaParameter;
            congestion[net.idx] = c;
        }
    }

    public static class RoutingFailedException extends Exception { }

    public static int iteration = 1;

    private HashSet<PhysicalDevice.PhysicalNet> remainingDests = new HashSet<PhysicalDevice.PhysicalNet>();
    private PriorityQueue<PhysicalDevice.PhysicalNet> pq = new PriorityQueue<PhysicalDevice.PhysicalNet>();
    public void route(NetList.LogicalNet logicalNet) throws RoutingFailedException {
        double maxDelay = 10;
        boolean tryHard = true;
        double ts = 0;
        if (logicalNet.driver == null) return;
        if (logicalNet.getSize() <= 1) return;
        if (isRouted(logicalNet)) return;

        int remaining = 0;
        for(NetList.Node.Port p : logicalNet.ports)
            if (p != logicalNet.driver) {
                PhysicalDevice.PhysicalNet dest;
                switch(p.index) {
                    case 0: dest = placement.nodeToCell(p.getNode()).getNet("xi"); break;
                    case 1: dest = placement.nodeToCell(p.getNode()).getNet("yi"); break;
                    default: throw new Error();
                }
                dest.remaining = true;
                remaining++;
            }
        iteration++;

        PhysicalDevice.PhysicalNet source = placement.nodeToCell(logicalNet.driver.getNode()).getNet("out");
        pq.clear();
        source.distance = -1 * maxDelay;
        source.depth = 0;
        source.iteration = iteration;
        pq.add(source);

        HashSet<PhysicalDevice.PhysicalNet> nets = new HashSet<PhysicalDevice.PhysicalNet>();
        HashSet<PhysicalDevice.PhysicalPip> pips = new HashSet<PhysicalDevice.PhysicalPip>();
        OUTER: while(true) {
            PhysicalDevice.PhysicalNet pn = pq.poll();
            if (pn==null) throw new RoutingFailedException();
            double frontier = pn.distance;
            for(PhysicalDevice.PhysicalPip pip : pn) {
                for(PhysicalDevice.PhysicalNet net : pip.getDrivenNets()) {
                    if (net.iteration != iteration) {
                        net.iteration = iteration;
                        net.distance = Double.MAX_VALUE;
                    }
                    double newfrontier = frontier + pip.getCost(pn, net);
                    newfrontier += getCongestion(net);
                    if (getLoad(net) > 0) newfrontier += 1000;

                    // penalty for using any net already routed in this iteration (makes routing order-sensitive)
                    //if (net.load >= 1) newfrontier = newfrontier + 20;

                    if (net.distance <= newfrontier) continue;
                    net.distance = newfrontier;
                    pq.add(net);
                    net.backpointer = pn;
                    net.depth = pn.depth+1;

                    if (!net.remaining) continue;
                    remaining--;
                    net.remaining = false;
                    // Vaughn Betz style multiterminal routing: once we reach one sink, make every node on the path
                    // "distance zero" from the source.
                    nets.add(source);
                    if (newfrontier > 0) ts += newfrontier;
                    for(PhysicalDevice.PhysicalNet p = net; p != source; p = p.backpointer) {
                        PhysicalDevice.PhysicalPip pipx = p.getPipFrom(p.backpointer);  // FIXME: this call is actually slow
                        pips.add(pipx);
                        if (pipx.driver != null) nets.add(pipx.driver);
                        for(PhysicalDevice.PhysicalNet n : pipx.driven) nets.add(n);
                        p.distance = source.distance;
                        pq.add(p);
                        nets.add(p);
                    }
                    if (remaining==0) break OUTER;
                }
            }
        }
        Route r = new Route(logicalNet, nets, pips);
        r.add();
        r.timingpenalty = ts;
    }


    public boolean isRouted(NetList.LogicalNet n) { return routes.get(n) != null; }
    //public boolean isRouted(PhysicalDevice.PhysicalNet pn) { return netToSignal.get(pn) != null; }
    //public PhysicalDevice.PhysicalNet signalToNet(NetList.LogicalNet n) { return signalToNets.get(n); }
    //public NetList.LogicalNet netToSignal(PhysicalDevice.PhysicalNet pn) { return netToSignals.get(pn); }

    public double measureCongestion() {
        double congestion = 0;
        for(PhysicalDevice.PhysicalNet pn : pd)
            if (getLoad(pn) > 1)
                congestion += getLoad(pn)-1;
        for(NetList.LogicalNet ln : netlist.nets)
            if (!isRouted(ln))
                congestion = Double.MAX_VALUE;
        return congestion;
    }

    public double measureWireCost() {
        double cong = 0;
        for(PhysicalDevice.PhysicalNet pn : pd)
            cong += getLoad(pn);
        return cong;
    }

    public double measureTimingpenalty() {
        double ret = 0;
        for(Route r : routes.values())
            ret += r.timingpenalty;
        return ret;
    }

    public int measureOverloaded() {
        int ret = 0;
        for(PhysicalDevice.PhysicalNet pn : pd)
            if (getLoad(pn) > 1)
                ret++;
        return ret;
    }

    public double measureWireUtilization() {
        int numwires = 0;
        int used = 0;
        for(PhysicalDevice.PhysicalNet pn : pd) {
            numwires++;
            used += getLoad(pn);
        }
        return ((double)used)/((double)numwires);
    }

    public void draw(Graphics2D g) {
        for(NetList.LogicalNet signal : netlist.nets)
            draw(g, signal);
    }
    public void draw(Graphics2D g, NetList.LogicalNet signal) {
        g.setColor(/*getLoad(pip1.driver) >= 2 ? Color.red :*/ Color.blue);
        for(Route r : routes.values()) {
            for(PhysicalDevice.PhysicalNet net : r.nets) {
                for(PhysicalDevice.PhysicalPip pip1 : net) {
                    if (!r.pips.contains(pip1)) continue;
                    for(PhysicalDevice.PhysicalPip pip2 : net) {
                        if (!r.pips.contains(pip2)) continue;

                        g.drawLine(pip1.getX(),
                                   pip1.getY(),
                                   pip2.getX(),
                                   pip2.getY());

                    }
                }
            }
        }
    }
}