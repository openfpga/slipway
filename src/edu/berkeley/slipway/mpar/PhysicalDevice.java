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

public class PhysicalDevice {
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
