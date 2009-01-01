package edu.berkeley.slipway.mpar;
import com.atmel.fpslic.*;
import byucc.edif.tools.merge.*;
import byucc.edif.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import edu.berkeley.slipway.*;
import com.atmel.fpslic.*;
import static com.atmel.fpslic.FpslicConstants.*;
import static edu.berkeley.slipway.mpar.MPARDemo.*;

public class NetList implements Iterable<NetList.Node> {

    public NetList(String s) throws EdifNameConflictException, InvalidEdifNameException {
        EdifEnvironment topEnv = new EdifEnvironment("top");
        EdifLibraryManager elm = new EdifLibraryManager(topEnv);
        EdifLibrary initLib = new EdifLibrary(elm, "initLib");
        EdifEnvironment env = EdifMergeParser.parseAndMerge(new String[] { s }, initLib);
        for(Iterator<EdifCellInstance> it = (Iterator<EdifCellInstance>)env.getTopCell().cellInstanceIterator();
            it.hasNext();
            ) {
            createNode(it.next(), null);
        }
    }

    private HashMap<String,Integer> ids = new HashMap<String,Integer>();

    public HashSet<Node>        nodes  = new HashSet<Node>();
    public ArrayList<Node>      nodes_ = new ArrayList<Node>();

    public HashSet<LogicalNet>  nets  = new HashSet<LogicalNet>();
    public Iterable<NetList.LogicalNet> getLogicalNets() { return nets; }

    /** a node is some primitive element; a potential configuration of a CLB */
    public class Node implements Iterable<Node.Port> {
        private final String type;
        private final int    id;

        private HashMap<String,Port> ports = new HashMap<String,Port>();
        public Iterator<Port> iterator() { return ports.values().iterator(); }

        public Node(String type) {
            nodes.add(this);
            nodes_.add(this);
            this.type = type.toLowerCase();
            Integer num = ids.get(type);
            this.id = num == null ? 0 : num.intValue();
            ids.put(type, this.id+1);
        }
        public String getType() { return type; }
        public String toString() {
            return type;
        }
        public Port getPort(String name, boolean driver) {
            Port p = ports.get(name);
            if (p==null) ports.put(name, p = new Port(name, driver));
            return p;
        }

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
         Node.Port driver = null;
         HashSet<Node.Port> ports = new HashSet<Node.Port>();

        public LogicalNet() { nets.add(this); }
        public Iterator<Node.Port> iterator() { return ports.iterator(); }
        public int getSize() { return ports.size(); }

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

    public Node randomNode(Random rand) {
        return nodes_.get(Math.abs(rand.nextInt()) % nodes_.size());
    }

    public Iterator<Node> iterator() { return nodes.iterator(); }
}
