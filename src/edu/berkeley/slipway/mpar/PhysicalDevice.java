package edu.berkeley.slipway.mpar;
import byucc.edif.tools.merge.*;
import byucc.edif.*;
import java.io.*;
import java.util.*;
import edu.berkeley.slipway.*;
import static edu.berkeley.slipway.mpar.MPARDemo.*;

public abstract class PhysicalDevice implements Iterable<PhysicalDevice.PhysicalNet> {

    public abstract PhysicalCell getCell(int col, int row);
    public abstract PhysicalCell randomCell(Random rand);

    private HashSet<PhysicalNet> allPhysicalNets = new HashSet<PhysicalNet>();
    public Iterator<PhysicalNet> iterator() { return allPhysicalNets.iterator(); }

    public abstract class PhysicalCell {
        public abstract PhysicalNet getNet(String name);
        public abstract void setFunction(String type);
        public abstract void place(NetList.Node n);
        public abstract PhysicalCell randomCellWithin(Random rand, double percentOfDevice);
    }

    private int master_idx = 0;

    public int getNumPhysicalNets() { return master_idx; }
    public class PhysicalNet implements Iterable<PhysicalPip>, Comparable<PhysicalNet> {

        public final int idx = master_idx++;

        // temporary variables used during route searches
        double      distance = Double.MAX_VALUE;
        double      delay = Double.MAX_VALUE;
        PhysicalNet backpointer = null;
        int iteration = 0;
        int depth = 0;
        boolean remaining = false;

        // adjacent pips
        public final HashSet<PhysicalPip> pips = new HashSet<PhysicalPip>();

        private String name;

        /** ordering is based on distance so we can use the Java PriorityQueue class */
        public int compareTo(PhysicalNet pn) {
            double x = distance - pn.distance;
            return x>0
                ? 1
                : x<0
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
    }
        
    public abstract class PhysicalPip {
        public PhysicalNet   driver;
        public PhysicalNet[] driven;
        private String name;
        double defaultCost;
        double defaultDelay = 1;
        public abstract boolean prohibited();
        public String toString() { return name; }
        public PhysicalNet   getDriverNet()  { return driver; }
        public PhysicalNet[] getDrivenNets() { return driven; }
        public double        getDelay(PhysicalNet in, PhysicalNet out) { return defaultDelay; }
        public double        getCost(PhysicalNet in, PhysicalNet out) { return defaultCost; }
        public abstract int getX();
        public abstract int getY();
        public PhysicalPip(String name, PhysicalNet driver, PhysicalNet[] driven) { this(name, driver, driven, wireCost); }
        public PhysicalPip(String name, PhysicalNet driver, PhysicalNet[] driven, double defaultCost) {
            this.name = name;
            this.driver = driver;
            this.driven = driven;
            this.defaultCost = 1;
            if (driver != null) driver.addPip(this);
            for(PhysicalNet pn : driven) pn.addPip(this);
        }
        public abstract void set(boolean connected);
    }
        
}
