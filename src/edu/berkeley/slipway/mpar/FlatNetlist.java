package edu.berkeley.slipway.mpar;
import com.atmel.fpslic.*;
import byucc.edif.tools.merge.*;
import byucc.edif.*;
import java.io.*;
import java.util.*;
import edu.berkeley.slipway.*;
import com.atmel.fpslic.*;
import static com.atmel.fpslic.FpslicConstants.*;
import static edu.berkeley.slipway.mpar.MPARDemo.*;

public class FlatNetlist {

    private HashMap<String,Integer> ids = new HashMap<String,Integer>();

    public HashSet<Node> nodes = new HashSet<Node>();
    public HashSet<Net>  nets  = new HashSet<Net>();

    /** a node is some primitive element; a potential configuration of a CLB */
    public class Node {
        public PhysicalDevice.PhysicalCell physicalCell = null;
        private final String type;
        private final int    id;

        public int x = -1;
        public int y = -1;

        private HashMap<String,Port> ports = new HashMap<String,Port>();

        public Node(String type) {
            nodes.add(this);
            this.type = type.toLowerCase();
            Integer num = ids.get(type);
            this.id = num == null ? 0 : num.intValue();
            ids.put(type, this.id+1);
        }
        public String getType() { return type; }
        public String toString() {
            if (x==-1 || y==-1)
                return type + "["+id+"]";
            return type + "@("+x+","+y+")";
        }
        public Port getPort(String name, boolean driver) {
            Port p = ports.get(name);
            if (p==null) ports.put(name, p = new Port(name, driver));
            return p;
        }

        public Fpslic.Cell getPlacement(Fpslic fpslic) { return fpslic.cell(x, y); }
        public void place(Fpslic fpslic) {
            Fpslic.Cell cell = fpslic.cell(x,y);
            cell.c(XLUT);
            cell.b(false);
            cell.f(false);
            cell.xi(NW);
            cell.yi(EAST);
            if      (type.equals("and2"))    cell.xlut(LUT_SELF & LUT_OTHER);
            else if (type.equals("or2"))     cell.xlut(LUT_SELF | LUT_OTHER);
            else if (type.equals("xor2"))    cell.xlut(LUT_SELF ^ LUT_OTHER);
            else if (type.equals("buf"))     cell.xlut(LUT_SELF);
            else if (type.equals("inv"))     cell.xlut(~LUT_SELF);
            else if (type.equals("cell0"))   return;
        }

        private int portIndex = 0;

        /** a port is an input or output to a Node */
        public class Port {
            private final String name;
            private final boolean driver;
            Net    net;
            public final int index;
            public Port(String name, boolean driver) {
                this.name = name;
                this.driver = driver;
                this.index = driver ? 0 : portIndex++;
            }
            public String toString() { return Node.this + "." + name; }
            public Node getNode() { return Node.this; }
            public void connect(Port p) {
                if (net != null)          { net.add(p);
                } else if (p.net != null) { p.net.add(this);
                } else {
                    new Net().add(this);
                    this.net.add(p);
                }
            }
            public void route(Fpslic fpslic, Port[] dests, PhysicalDevice pd, FlatNetlist.Net owner) {
                PhysicalDevice.PhysicalNet[] destsp = new PhysicalDevice.PhysicalNet[dests.length];
                for(int i=0; i<dests.length; i++) {
                    Port dest = dests[i];
                    switch(dest.index) {
                        case 0: destsp[i] = dest.getNode().physicalCell.getNet("xi"); break;
                        case 1: destsp[i] = dest.getNode().physicalCell.getNet("yi"); break;
                        default: throw new Error();
                    }
                }
                //System.out.println(physicalCell.getNet("out"));
                //System.out.println(destsp[0]);
                pd.route(physicalCell.getNet("out"), destsp, owner);

                /*
                  Fpslic.Cell driverCell = fpslic.cell(getNode().x,getNode().y);
                  Fpslic.Cell destCell   = fpslic.cell(dest.getNode().x,dest.getNode().y);
                  boolean[] hblocked = new boolean[5];
                  boolean[] vblocked = new boolean[5];
                  hblocked[3] = true;
                  vblocked[3] = true;
                  int minx = Math.min(getNode().x, dest.getNode().x);
                  int miny = Math.min(getNode().y, dest.getNode().y);
                  int maxx = Math.max(getNode().x, dest.getNode().x);
                  int maxy = Math.max(getNode().y, dest.getNode().y);
                  for(int cx = 0; cx <= 3; cx++) {
                  Fpslic.Cell c = fpslic.cell(cx, getNode().y);
                  for(int i=0; i<5; i++)
                  hblocked[i] |= (c.hx(i) && !c.equals(driverCell));
                  }
                  for(int cy = 0; cy <= 3; cy++) {
                  Fpslic.Cell c = fpslic.cell(dest.getNode().x, cy);
                  for(int i=0; i<5; i++)
                  vblocked[i] |= (c.vx(i) && !c.equals(driverCell));
                  }
                  int free = 0;
                  for(; free < 5; free++) if (!hblocked[free]) break;
                  for(; free < 5; free++) if (!vblocked[free]) break;
                  if (free >= 5) throw new RuntimeException("unroutable!");
                  Fpslic.Cell turnCell = fpslic.cell(dest.getNode().x, getNode().y);
                  driverCell.out(free, true);
                  driverCell.h(free, true);
                  turnCell.h(free, true);
                  turnCell.v(free, true);
                  switch(dest.index) {
                  case 0: destCell.xi(L0 + free); break;
                  case 1: destCell.yi(L0 + free); break;
                  case 2: destCell.wi(L0 + free); break;
                  case 3: destCell.zi(L0 + free); break;
                  default: throw new RuntimeException("error");
                  }
                  destCell.v(free, true);
                  System.out.println("route " + this + " -> " + dest + " on planes " + free);
                */
            }
        }
    }

    /** a Net is a collection of ports which are wired together */
    public class Net implements Iterable<Node.Port> {
        private Node.Port driver = null;
        private HashSet<Node.Port> ports = new HashSet<Node.Port>();
        public Net() { nets.add(this); }
        public Iterator<Node.Port> iterator() { return ports.iterator(); }
        public int getSize() { return ports.size(); }
        public HashSet<PhysicalDevice.PhysicalPip> pips = new HashSet<PhysicalDevice.PhysicalPip>();
        public HashSet<PhysicalDevice.PhysicalNet> pns = new HashSet<PhysicalDevice.PhysicalNet>();
        public boolean routed = false;
        public void unroute() {
            for(PhysicalDevice.PhysicalPip pip : pips)
                pip.set(false);
            for(PhysicalDevice.PhysicalNet net : pns) {
                net.owners.remove(this);
                net.load--;
            }
            pips.clear();
            pns.clear();
            routed = false;
        }
        public void route(Fpslic fpslic, PhysicalDevice pd) {
            if (driver == null) return;
            if (routed) return;
            //System.out.println();
            //System.out.println("routing " + this);
            Node.Port[] dests = new Node.Port[ports.size() - (ports.contains(driver) ? 1 : 0)];
            int i = 0;
            for(Node.Port p : ports)
                if (p != driver)
                    dests[i++] = p;
            driver.route(fpslic, dests, pd, this);
            routed = true;
        }
        public void add(Node.Port p) {
            if (p.driver) {
                if (driver != null && driver != p)
                    throw new RuntimeException("two drivers on a port!\n  "+driver+"\n  "+p);
                driver = p;
            }
            if (p.net==this || ports.contains(p)) return;
            ports.add(p);
            add(p.net);
            p.net = this;
        }
        public void add(Net n) {
            if (n==this || n==null) return;
            for(Node.Port p : n) add(p);
            nets.remove(n);
        }
        public String toString() {
            StringBuffer ret = new StringBuffer();
            ret.append(driver==null ? "()" : driver.toString());
            ret.append(" -> ");
            for(Node.Port p : this)
                if (p!=driver)
                    ret.append(p+" ");
            return ret.toString();
        }
    }


    public HashMap<EdifCellInstance,FlatNetlist.Node> cache =
        new HashMap<EdifCellInstance,FlatNetlist.Node>();
    public HashMap<String,FlatNetlist.Node> top =
        new HashMap<String,FlatNetlist.Node>();
       
    public FlatNetlist.Node createNode(EdifCellInstance eci, String portName) {
        FlatNetlist.Node n = eci==null ? top.get(portName) : cache.get(eci);
        if (n != null) return n;
        if (eci==null) {
            n = new FlatNetlist.Node("top_"+portName);
            top.put(portName, n);
            return n;
        } else {
            n = new FlatNetlist.Node(eci.getType());
            cache.put(eci,n);
        }
        for(EdifPortRef epr : eci.getAllEPRs()) {
            EdifPort ep = epr.getPort();
            EdifNet  en = epr.getNet();
            String name = ep.getOldName();
            boolean driver = ep.getDirection()==ep.OUT;
            if (eci==null) driver = !driver;
            if (eci==null) name = driver ? "out" : "xi";
            FlatNetlist.Node.Port p = n.getPort(name, driver);
            for(EdifPortRef epr2 : en.getConnectedPortRefs()) {
                EdifCellInstance eci2 = epr2.getCellInstance();
                EdifPort ep2 = epr2.getPort();
                Node n2 = createNode(eci2, ep2.getOldName());
                driver = ep2.getDirection()==ep.OUT;
                name = ep2.getOldName();
                if (eci2==null) driver = !driver;
                if (eci2==null) name = driver ? "out" : "xi";
                FlatNetlist.Node.Port p2 = n2.getPort(name, driver);
                p.connect(p2);
            }
        }
        return n;
    }
}
