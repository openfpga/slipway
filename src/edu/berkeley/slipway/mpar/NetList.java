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

public class NetList {

    private HashMap<String,Integer> ids = new HashMap<String,Integer>();

    public HashSet<Node>        nodes = new HashSet<Node>();
    public HashSet<LogicalNet>  nets  = new HashSet<LogicalNet>();

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

        private int portIndex = 0;

        /** a port is an input or output to a Node */
        public class Port {
            private final String name;
            private final boolean driver;
            LogicalNet    net;
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
                    new LogicalNet().add(this);
                    this.net.add(p);
                }
            }
        }
    }

    /** a Net is a collection of ports which are wired together */
    public class LogicalNet implements Iterable<Node.Port> {
        private Node.Port driver = null;
        private HashSet<Node.Port> ports = new HashSet<Node.Port>();
        private HashSet<PhysicalDevice.PhysicalPip> pips = new HashSet<PhysicalDevice.PhysicalPip>();
        private HashSet<PhysicalDevice.PhysicalNet> pns = new HashSet<PhysicalDevice.PhysicalNet>();

        public void addPhysicalNet(PhysicalDevice.PhysicalNet pn) { pns.add(pn); }
        public void removePhysicalNet(PhysicalDevice.PhysicalNet pn) { pns.remove(pn); }
        public void addPhysicalPip(PhysicalDevice.PhysicalPip pip) { pips.add(pip); }

        public LogicalNet() { nets.add(this); }
        public Iterator<Node.Port> iterator() { return ports.iterator(); }
        public int getSize() { return ports.size(); }
        public boolean routed = false;
        public void unroute() {
            for(PhysicalDevice.PhysicalPip pip : pips) pip.set(false);
            while(pns.size() > 0) pns.iterator().next().removeLogicalNet(this);
            pips.clear();
            pns.clear();
            routed = false;
        }
        public void route(Fpslic fpslic, PhysicalDevice pd) {
            if (driver == null) return;
            if (routed) return;
            Node.Port[] dests = new Node.Port[ports.size() - (ports.contains(driver) ? 1 : 0)];
            int j = 0;
            for(Node.Port p : ports)
                if (p != driver)
                    dests[j++] = p;
            PhysicalDevice.PhysicalNet[] destsp = new PhysicalDevice.PhysicalNet[dests.length];
            for(int i=0; i<dests.length; i++) {
                Node.Port dest = dests[i];
                switch(dest.index) {
                    case 0: destsp[i] = dest.getNode().physicalCell.getNet("xi"); break;
                    case 1: destsp[i] = dest.getNode().physicalCell.getNet("yi"); break;
                    default: throw new Error();
                }
            }
            driver.getNode().physicalCell.getNet("out").route(destsp, this);
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
        public void add(LogicalNet n) {
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


    public HashMap<EdifCellInstance,NetList.Node> cache =
        new HashMap<EdifCellInstance,NetList.Node>();
    public HashMap<String,NetList.Node> top =
        new HashMap<String,NetList.Node>();
       
    public NetList.Node createNode(EdifCellInstance eci, String portName) {
        NetList.Node n = eci==null ? top.get(portName) : cache.get(eci);
        if (n != null) return n;
        if (eci==null) {
            n = new NetList.Node("top_"+portName);
            top.put(portName, n);
            return n;
        } else {
            n = new NetList.Node(eci.getType());
            cache.put(eci,n);
        }
        for(EdifPortRef epr : eci.getAllEPRs()) {
            EdifPort ep = epr.getPort();
            EdifNet  en = epr.getNet();
            String name = ep.getOldName();
            boolean driver = ep.getDirection()==ep.OUT;
            if (eci==null) driver = !driver;
            if (eci==null) name = driver ? "out" : "xi";
            NetList.Node.Port p = n.getPort(name, driver);
            for(EdifPortRef epr2 : en.getConnectedPortRefs()) {
                EdifCellInstance eci2 = epr2.getCellInstance();
                EdifPort ep2 = epr2.getPort();
                Node n2 = createNode(eci2, ep2.getOldName());
                driver = ep2.getDirection()==ep.OUT;
                name = ep2.getOldName();
                if (eci2==null) driver = !driver;
                if (eci2==null) name = driver ? "out" : "xi";
                NetList.Node.Port p2 = n2.getPort(name, driver);
                p.connect(p2);
            }
        }
        return n;
    }
}
