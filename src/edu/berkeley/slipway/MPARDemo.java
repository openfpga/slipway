import com.atmel.fpslic.*;
import byucc.edif.tools.merge.*;
import byucc.edif.*;
import java.io.*;
import java.util.*;
import edu.berkeley.slipway.*;
import com.atmel.fpslic.*;
import static com.atmel.fpslic.FpslicConstants.*;

// FIXME: sometimes gets stuck in a loop routing the last few nets

// FEATURE: ability to rip up only one branch of a multi-terminal net

// FEATURE: re-placement based on routing congestion
//          note: must assign a cost to "bare wire" -- if not, the
//          placer will fail to recognize moves that put blocks closer
//          together, thereby decreasing the potential for future
//          congestion.

// FEATURE: A* search (chap7 of independence thesis)

// FIXME: distinguish out,xo,yo                                                                                              
// FIXME: y-axis shortcuts                                                                                                   
// FEATURE: ability to use a cell for routing purposes                                                                       
// FEATURE: global two-sector-long wires                                                                                     

public class MPARDemo {

    public static final double alphaParameter = 00.9;
    public static final double betaParameter  = 02.5;
    public static final double gammaParameter =  1.0;

    public static class FlatNetlist {

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

    /*
      test code for inter-sector switchboxes
    public static void main2() throws Exception {
        Fpslic fpslic = new FtdiBoard();
        // set up scan cell
        fpslic.cell(23,15).h(3, true);
        fpslic.cell(23,15).yi(L3);
        fpslic.cell(23,15).ylut(0xAA);
        fpslic.iob_right(15, true).enableOutput(WEST);
        fpslic.cell(23,0).ylut(0x00);
        fpslic.iob_right(0, true).enableOutput(WEST);
        fpslic.flush();
        for(int x=0; x<20; x++) {
            for(int y=0; y<20; y++) {
                for(int l=0; l<5; l++) {
                    for(int v = 0; v <= 1; v++) {
                        boolean vert = v==1;
                        int newx = vert ? x   : x-1;
                        int newy = vert ? y-1 : y;
                        if (newx<0 || newy<0) continue;
                        if (vert  && (y%4) != 0) continue;
                        if (!vert && (x%4) != 0) continue;

                        int layer = l;
                        if (layer==3) continue;
                        Fpslic.Cell c  = fpslic.cell(x, y);
                        Fpslic.Cell c2 = fpslic.cell(newx, newy);
                        Fpslic.SectorWire sw1 = vert ? c.vwire(layer)  : c.hwire(layer);
                        Fpslic.SectorWire sw2 = vert ? c2.vwire(layer) : c2.hwire(layer);
                        sw1.drives(sw2, true);

                        c.c(YLUT);
                        if (vert) c.v(L0 + layer, true);
                        else      c.h(L0 + layer, true);
                        c.out(L0 + layer, true);
                        c.b(false);
                        
                        c2.yi(L0 + layer);
                        if (vert) c2.v(L0 + layer, true);
                        else      c2.h(L0 + layer, true);
                        c2.ylut(LUT_SELF);
                        c2.c(YLUT);
                        c2.b(false);
                        
                        System.out.print(x+","+y+","+l+","+(vert?"v":"h")+": ");
                        c.ylut(0x00);
                        fpslic.flush();
                        boolean good = scan(fpslic, c2)==0;
                        if (!good) fails++;
                        System.out.print(good ? "ok " : "bad ");
                        c.ylut(0xff);
                        fpslic.flush();
                        good = scan(fpslic, c2)!=0;
                        if (!good) fails++;
                        System.out.print(good ? "ok " : "bad ");
                        System.out.println();
                        sw1.drives(sw2, false);
                        if (vert) c.v(layer, false);
                        else      c.h(layer, false);
                        c.out(layer, false);
                    }
                }
            }
        }
        System.out.println("fails = " + fails);
        
    }
    public static int fails = 0;
    */

    public static void main(String[] s) throws Exception {
        EdifEnvironment topEnv = new EdifEnvironment("top");
        EdifLibraryManager elm = new EdifLibraryManager(topEnv);
        EdifLibrary initLib = new EdifLibrary(elm, "initLib");
        EdifEnvironment env = EdifMergeParser.parseAndMerge(s, initLib);
        System.out.println("top is " + env.getTopCell());
        FlatNetlist fnl = new FlatNetlist();

        for(Iterator<EdifCellInstance> it = (Iterator<EdifCellInstance>)env.getTopCell().cellInstanceIterator();
            it.hasNext();
            ) {
            FlatNetlist.Node n = fnl.createNode(it.next(), null);
        }

        Fpslic fpslic = new FtdiBoard();
        PhysicalDevice pd = new PhysicalDevice(fpslic, 20, 20);

        int px = 0;
        int py = 0;

        // crude map
        Random rand = new Random();
        boolean[][] used = new boolean[pd.width][pd.height];
        for(FlatNetlist.Node n : fnl.nodes) {
            while(true) {
                px = Math.abs(rand.nextInt()) % pd.width;
                py = Math.abs(rand.nextInt()) % pd.height;
                if (!used[px][py]) {
                    used[px][py] = true;
                    n.x = px;
                    n.y = py;
                    n.physicalCell = pd.getCell(px, py);
                    System.out.println("placed " + n + " at ("+px+","+py+")");
                    n.place(fpslic);
                    break;
                }
            }
        }

        int trial = 0;
        HashSet<FlatNetlist.Net> needUnroute = new HashSet<FlatNetlist.Net>();
        while(true) {
            System.out.println();
            System.out.println("routing trial " + (++trial));
            for(FlatNetlist.Net net : fnl.nets) {
                if (net.getSize() <= 1) continue;
                net.route(fpslic, pd);
            }
            double congestion = 0;
            int overrouted = 0;
            needUnroute.clear();
            for(PhysicalDevice.PhysicalNet pn : pd.allPhysicalNets) {
                if (pn.load > 1) {
                    //System.out.println("overrouted: " + pn + ", congestion="+pn.congestion + ", load=" + pn.load);
                    overrouted++;
                    congestion += pn.congestion;
                }
                pn.congestion = pn.congestion * alphaParameter;
                if (pn.load > 1) {
                    pn.congestion += betaParameter;
                    // don't do this here
                    //pn.congestion += betaParameter;
                    for(FlatNetlist.Net n : pn.owners)
                        needUnroute.add(n);
                }
            }
            System.out.println("  overrouted="+overrouted+", congestion="+congestion +", ripping up " + needUnroute.size() +" nets of " + fnl.nets.size());
            if (overrouted <= 0) break;
            //for(FlatNetlist.Net net : fnl.nets)
            for(FlatNetlist.Net net : needUnroute)
                net.unroute();
            /*
            for(PhysicalDevice.PhysicalNet pn : pd.allPhysicalNets)
                for(PhysicalDevice.PhysicalPip pip : pn) {
                    pip.set(false);
                }
            */
        }

        // set up scan cell
        fpslic.cell(23,15).h(3, true);
        fpslic.cell(23,15).yi(L3);
        fpslic.cell(23,15).ylut(0xAA);
        fpslic.iob_right(15, true).enableOutput(WEST);
        fpslic.cell(23,0).ylut(0x00);
        fpslic.iob_right(0, true).enableOutput(WEST);
        fpslic.flush();

        int width = 8;
        while(true) {
            int a = Math.abs(rand.nextInt()) % (1 << width);
            int b = Math.abs(rand.nextInt()) % (1 << width);
            setInput(fnl, fpslic, "a",  a);
            setInput(fnl, fpslic, "b",  b);
            setInput(fnl, fpslic, "ci", 0);
            int result = getOutput(fnl, fpslic, "out");
            System.out.println(Integer.toString(a,16) + " + " +
                               Integer.toString(b,16) + " = " +
                               Integer.toString(result,16) +
                               " [ " + (a+b==result ? "ok" : "bad" ) + " ] ");
        }
    }

    public static class PhysicalDevice {
        private final Fpslic fpslic;
        
        public final int width;
        public final int height;
        private final PhysicalNet[][][][] sectorWires;
        private final PhysicalCell[][] cells;

        public PhysicalCell getCell(int col, int row) {
            if (col<0) return null;
            if (row<0) return null;
            if (col>=width) return null;
            if (row>=height) return null;
            return cells[col][row];
        }

        public PhysicalDevice(final Fpslic fpslic, int width, int height) {
            this.fpslic = fpslic;
            this.width = width;
            this.height = height;
            sectorWires = new PhysicalNet[width][height][5][2];
            for(int x=0; x<width; x+=4)
                for(int y=0; y<height; y+=4)
                    for(int p=0; p<5; p++) {
                        for(int xc=x; xc<x+4; xc++) {
                            PhysicalNet vwire = new PhysicalNet("("+xc+","+y+"-"+(y+3)+")");
                            for(int yc=y; yc<y+4; yc++)
                                sectorWires[xc][yc][p][0] = vwire;
                        }
                        for(int yc=y; yc<y+4; yc++) {
                            PhysicalNet hwire = new PhysicalNet("("+x+"-"+(x+3)+","+yc+")");
                            for(int xc=x; xc<x+4; xc++)
                                sectorWires[xc][yc][p][1] = hwire;
                        }
                    }

            for(int x=4; x<width; x+=4) {
                for(int y=0; y<height; y++) {
                    for(int p=0; p<5; p++) {
                        final int xc = x;
                        final int yc = y;
                        final int pc = p;
                        new PhysicalPip("xxx",
                                        sectorWires[x-1][y][p][1],
                                        new PhysicalNet[] { sectorWires[x][y][p][1] },
                                        5) {
                            public void set(boolean connected) {
                                fpslic.cell(xc-1, yc).hwire(pc).drives(fpslic.cell(xc, yc).hwire(pc), connected);
                            }
                        };
                        new PhysicalPip("xxx",
                                        sectorWires[x][y][p][1],
                                        new PhysicalNet[] { sectorWires[x-1][y][p][1] },
                                        5) {
                            public void set(boolean connected) {
                                fpslic.cell(xc, yc).hwire(pc).drives(fpslic.cell(xc-1, yc).hwire(pc), connected);
                            }
                        };
                    }
                }
            }

            for(int x=0; x<width; x++) {
                for(int y=4; y<height; y+=4) {
                    for(int p=0; p<5; p++) {
                        final int xc = x;
                        final int yc = y;
                        final int pc = p;
                        new PhysicalPip("xxx",
                                        sectorWires[x][y-1][p][0],
                                        new PhysicalNet[] { sectorWires[x][y][p][0] },
                                        5) {
                            public void set(boolean connected) {
                                fpslic.cell(xc, yc-1).vwire(pc).drives(fpslic.cell(xc, yc).vwire(pc), connected);
                            }
                        };
                        new PhysicalPip("xxx",
                                        sectorWires[x][y][p][0],
                                        new PhysicalNet[] { sectorWires[x][y-1][p][0] },
                                        5) {
                            public void set(boolean connected) {
                                fpslic.cell(xc, yc).vwire(pc).drives(fpslic.cell(xc, yc-1).vwire(pc), connected);
                            }
                        };
                    }
                }
            }

            cells = new PhysicalCell[width][height];
            for(int x=0; x<width; x++)
                for(int y=0; y<height; y++) {
                    cells[x][y] = new PhysicalCell(x, y);
                }
            for(int x=0; x<width; x++)
                for(int y=0; y<height; y++)
                    cells[x][y].link();
        }

        private PhysicalNet getSectorWire(int col, int row, int plane, boolean horizontal) {
            return sectorWires[col][row][plane][horizontal ? 1 : 0];
        }

        public class PhysicalCell {

            public PhysicalNet getNet(String name) {
                if (name.equals("out")) return outputNet;
                if (name.equals("xi"))  return xin;
                if (name.equals("yi"))  return yin;
                throw new RuntimeException("unknown");
            }

            private int col;
            private int row;
            private PhysicalNet   outputNet;
            private PhysicalNet   xin;
            private PhysicalNet   yin;
            private PhysicalNet[] local = new PhysicalNet[5];

            private Fpslic.Cell cell() { return fpslic.cell(col, row); }

            public void setFunction(String type) {
                Fpslic.Cell cell = cell();
                cell.c(XLUT);
                cell.xo(false);
                cell.b(false);
                cell.f(false);
                if      (type.equals("and2"))    cell.xlut(LUT_SELF & LUT_OTHER);
                else if (type.equals("or2"))     cell.xlut(LUT_SELF | LUT_OTHER);
                else if (type.equals("xor2"))    cell.xlut(LUT_SELF ^ LUT_OTHER);
                else if (type.equals("buf"))     cell.xlut(LUT_SELF);
                else if (type.equals("inv"))     cell.xlut(~LUT_SELF);
            }

            public void link() {
                // FIXME wow, this is a horrendous hack!
                if (getCell(col-1, row+1) != null)
                    new PhysicalPip(this+".xiNW", getCell(col-1, row+1).getNet("out"), new PhysicalNet[] { xin }, 5) {
                        public void set(boolean connected) { cell().xi(connected ? NW : NONE); }
                    };
                if (getCell(col-1, row-1) != null)
                    new PhysicalPip(this+".xiSW", getCell(col-1, row-1).getNet("out"), new PhysicalNet[] { xin }, 5) {
                        public void set(boolean connected) { cell().xi(connected ? SW : NONE); }
                    };
                if (getCell(col+1, row+1) != null)
                    new PhysicalPip(this+".xiNE", getCell(col+1, row+1).getNet("out"), new PhysicalNet[] { xin }, 5) {
                        public void set(boolean connected) { cell().xi(connected ? NE : NONE); }
                    };
                if (getCell(col+1, row-1) != null)
                    new PhysicalPip(this+".xiSE", getCell(col+1, row-1).getNet("out"), new PhysicalNet[] { xin }, 5) {
                        public void set(boolean connected) { cell().xi(connected ? SE : NONE); }
                    };
            }

            private PhysicalCell(int col, int row) {
                this.row = row;
                this.col = col;
                outputNet = new PhysicalNet(this.toString()+".out");
                xin       = new PhysicalNet(this.toString()+".xi");
                yin       = new PhysicalNet(this.toString()+".yi");
                for(int j=0; j<5; j++) {

                    // plane 3 is reserved for debugging
                    if (j==3) continue;

                    final int i = j;
                    local[i] = new PhysicalNet(this.toString()+".L"+i);
                    new PhysicalPip(this+".h"+i,  null,      new PhysicalNet[] { local[i], getSectorWire(col, row, i, true) }) {
                        public void set(boolean connected) { cell().h(i, connected); }
                    };
                    new PhysicalPip(this+".v"+i,  null,      new PhysicalNet[] { local[i], getSectorWire(col, row, i, false) }) {
                        public void set(boolean connected) { cell().v(i, connected); }
                    };
                    new PhysicalPip(this+".xi"+i, local[i],  new PhysicalNet[] { xin }) {
                        public void set(boolean connected) { cell().xi(connected ? i : NONE); }
                    };
                    new PhysicalPip(this+".yi"+i, local[i],  new PhysicalNet[] { yin }) {
                        public void set(boolean connected) { cell().yi(connected ? i : NONE); }
                    };
                    new PhysicalPip(this+".o"+i,  outputNet, new PhysicalNet[] { local[i] }) {
                        public void set(boolean connected) { cell().out(i, connected); }
                    };
                }
            }
            public  String toString() { return "cell@("+col+","+row+")"; }

        }

        public void route(PhysicalNet source, PhysicalNet[] dests, FlatNetlist.Net owner) {
            HashSet<PhysicalNet> remainingDests = new HashSet<PhysicalNet>();
            for(PhysicalNet dest : dests) remainingDests.add(dest);

            HashSet<PhysicalNet> needsReset = new HashSet<PhysicalNet>();
            PriorityQueue<PhysicalNet> pq = new PriorityQueue<PhysicalNet>();
            needsReset.add(source);
            source.distance = 0;
            pq.add(source);

            OUTER: while(true) {
                PhysicalNet pn = pq.poll();
                if (pn==null) throw new Error("unroutable! " + source + " -> " + dests[0]);
                double frontier = pn.distance;
                for(PhysicalPip pip : pn)
                    for(PhysicalNet net : pip.getDrivenNets()) {
                        double newfrontier = frontier + 0.05 + net.congestion;

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
                                //pnx.distance = 0;
                                pq.add(pnx);
                            }
                            break;
                        }
                    }
            }

            for(PhysicalNet dest : dests) {
                PhysicalNet pn = dest;
                while(pn != null && pn.backpointer != null) {
                    pn.owners.add(owner);
                    owner.pns.add(pn);
                    if (pn.distance != Double.MAX_VALUE) {
                        pn.distance = Double.MAX_VALUE;
                        pn.load++;
                        if (pn.load>=2) pn.congestion += betaParameter;
                    }
                    PhysicalPip pip = pn.getPipFrom(pn.backpointer);
                    pip.set(true);
                    owner.pips.add(pip);
                    pn = pn.backpointer;
                }
                // FIXME: check pn==source at this point
            }

            for(PhysicalNet pn : needsReset) {
                pn.distance    = Double.MAX_VALUE;
                pn.backpointer = null;
            }
        }
        public HashSet<PhysicalNet> allPhysicalNets = new HashSet<PhysicalNet>();
        public class PhysicalNet implements Iterable<PhysicalPip>, Comparable<PhysicalNet> {
            public double      congestion = 0;
            public int         load = 0;
            public double      distance = Double.MAX_VALUE;
            public PhysicalNet backpointer = null;

            public int compareTo(PhysicalNet pn) {
                double x = distance - pn.distance;
                return distance > pn.distance
                    ? 1
                    : distance < pn.distance
                    ? -1
                    : 0;
            }

            private final HashSet<PhysicalPip> pips = new HashSet<PhysicalPip>();
            public Iterator<PhysicalPip> iterator() { return pips.iterator(); }
            private String name;
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
            public HashSet<FlatNetlist.Net> owners = new HashSet<FlatNetlist.Net>();
        }
        
        public abstract class PhysicalPip {
            private PhysicalNet   driver;
            private PhysicalNet[] driven;
            private String name;
            private int defaultCost;
            public String toString() { return name; }
            public PhysicalNet   getDriverNet()  { return driver; }
            public PhysicalNet[] getDrivenNets() { return driven; }
            public int           getCost(PhysicalNet in, PhysicalNet out) { return defaultCost; }
            public PhysicalPip(String name, PhysicalNet driver, PhysicalNet[] driven) { this(name, driver, driven, 100); }
            public PhysicalPip(String name, PhysicalNet driver, PhysicalNet[] driven, int defaultCost) {
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

    private static int ret;
    public static synchronized int scan(final Fpslic device, final Fpslic.Cell cell) {
        try {
            scan(device, cell, YLUT, true);
            ((FtdiBoard)device).readBus(new FtdiBoard.ByteCallback() {
                    public void call(byte b) throws Exception {
                        ret = b;
                        synchronized(device) {
                            device.notifyAll();
                        }
                    }
                });
            synchronized(device) {
                try {
                    device.wait();
                } catch (Exception e) { throw new RuntimeException(e); }
            }
            scan(device, cell, YLUT, false);
            return ret;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static void scan(Fpslic dev, Fpslic.Cell cell, int source, boolean setup) {
        if (setup) {
            //if (source != NONE) cell.c(source);
            if (cell.b()) cell.b(false);
            if (cell.f()) cell.f(false);
        }
        if (cell.out(L3)!=setup) cell.out(L3, setup);
        if (cell.vx(L3)!=setup) cell.v(L3, setup);

        Fpslic.SectorWire sw = cell.vwire(L3);
        //System.out.println("wire is: " + sw);

        if (sw.row > (12 & ~0x3) && sw.north()!=null && sw.north().drives(sw))
            sw.north().drives(sw, false);
        while(sw.row > (12 & ~0x3) && sw.south() != null) {
            //System.out.println(sw + " -> " + sw.south());
            if (sw.drives(sw.south())!=setup) sw.drives(sw.south(), setup);
            sw = sw.south();
        }
        if (sw.row < (12 & ~0x3) && sw.south() != null && sw.south().drives(sw))
            sw.north().drives(sw, false);
        while(sw.row < (12 & ~0x3) && sw.north() != null) {
            //System.out.println(sw + " -> " + sw.north());
            if (sw.drives(sw.north())!=setup) sw.drives(sw.north(), setup);
            sw = sw.north();
        }

        //cell = dev.cell(19, 15);
        cell = dev.cell(cell.col, 15);
        /*
        System.out.println("cell is " + cell);
        cell.xlut(0xff);
        cell.ylut(0xff);
        cell.b(false);
        cell.f(false);
        cell.c(XLUT);
        cell.out(L3, true);
        cell.oe(NONE);
        */
        if (cell.hx(L3) != setup) cell.h(L3, setup);
        if (cell.vx(L3) != setup) cell.v(L3, setup);
        sw = cell.hwire(L3);

        if (sw.west()!=null && sw.west().drives(sw)) { sw.west().drives(sw, false); }
        while(sw.east() != null) {
            //System.out.println(sw + " -> " + sw.east());
            if (sw.drives(sw.east())!=setup) sw.drives(sw.east(), setup);
            sw = sw.east();
        }

    }

        public static void setInput(FlatNetlist fnl, Fpslic fpslic, String prefix, int val) {
            for(int i=0; ; i++) {
                FlatNetlist.Node n = fnl.top.get(prefix + "["+i+"]");
                if (n==null && i==0) n = fnl.top.get(prefix);
                if (n==null) return;
                Fpslic.Cell c = n.getPlacement(fpslic);
                c.c(XLUT);
                c.b(false);
                c.xlut((val & 0x1)==0 ? 0x00 : 0xff);
                val = val >> 1;
            }
        }
        public static int getOutput(FlatNetlist fnl, Fpslic fpslic, String prefix) {
            int val = 0;
            for(int i=0; ; i++) {
                FlatNetlist.Node n = fnl.top.get(prefix+"["+i+"]");
                if (n==null && i==0) n = fnl.top.get(prefix);
                if (n==null) return val;
                Fpslic.Cell c = n.getPlacement(fpslic);
                c.xlut(LUT_SELF);
                c.c(XLUT);
                c.b(false);
                fpslic.flush();
                int scan = scan(fpslic, c);
                val |= ((scan==0 ? 0 : 1) << i);
            }
        }


}