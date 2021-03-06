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

public class PhysicalFpslic extends PhysicalDevice {
    public static int CELLSEP  = 40;
    public static int PLANESEP = 3;

    private final FpslicDevice fpslic;
        
    public final int width;
    public final int height;
    private final PhysicalNet[][][][] sectorWires;
    private final PhysicalFpslicCell[][] cells;

    public PhysicalCell getCell(int col, int row) {
        if (col<0) return null;
        if (row<0) return null;
        if (col>=width) return null;
        if (row>=height) return null;
        return cells[col][row];
    }

    public PhysicalFpslic(final FpslicDevice fpslic, int width, int height) {
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
                            if (xc < sectorWires.length && yc < sectorWires[xc].length)
                                sectorWires[xc][yc][p][0] = vwire;
                    }
                    for(int yc=y; yc<y+4; yc++) {
                        PhysicalNet hwire = new PhysicalNet("("+x+"-"+(x+3)+","+yc+")");
                        for(int xc=x; xc<x+4; xc++)
                            if (xc < sectorWires.length && yc < sectorWires[xc].length)
                                sectorWires[xc][yc][p][1] = hwire;
                    }
                }

        for(int x=4; x<width; x+=4) {
            for(int y=0; y<height; y++) {
                for(int p=0; p<5; p++) {
                    final int xc = x;
                    final int yc = y;
                    final int pc = p;
                    new PhysicalFpslicPip(x,
                                          y,
                                          -1 * (4-p) * PLANESEP,
                                          -1 * (0+p) * PLANESEP,
                                          "xxx",
                                          sectorWires[x-1][y][p][1],
                                          new PhysicalNet[] { sectorWires[x][y][p][1] },
                                          5) {
                        public void set(boolean connected) {
                            fpslic.cell(xc-1, yc).hwire(pc).drives(fpslic.cell(xc, yc).hwire(pc), connected);
                        }
                    };
                    new PhysicalFpslicPip(x,
                                          y,
                                          -1 * (4-p) * PLANESEP,
                                          -1 * (0+p) * PLANESEP,
                                          "xxx",
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
                    new PhysicalFpslicPip(x,
                                          y,
                                          -1 * (4-p) * PLANESEP,
                                          -1 * (0+p) * PLANESEP,
                                          "xxx",
                                          sectorWires[x][y-1][p][0],
                                          new PhysicalNet[] { sectorWires[x][y][p][0] },
                                          5) {
                        public void set(boolean connected) {
                            fpslic.cell(xc, yc-1).vwire(pc).drives(fpslic.cell(xc, yc).vwire(pc), connected);
                        }
                    };
                    new PhysicalFpslicPip(x,
                                          y,
                                          -1 * (4-p) * PLANESEP,
                                          -1 * (0+p) * PLANESEP,
                                          "xxx",
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

        cells = new PhysicalFpslicCell[width][height];
        for(int x=0; x<width; x++)
            for(int y=0; y<height; y++) {
                cells[x][y] = new PhysicalFpslicCell(x, y);
            }
        for(int x=0; x<width; x++)
            for(int y=0; y<height; y++)
                cells[x][y].link();
    }

    private PhysicalNet getSectorWire(int col, int row, int plane, boolean horizontal) {
        return sectorWires[col][row][plane][horizontal ? 1 : 0];
    }

    public PhysicalCell randomCell(Random rand) {
        int x = (Math.abs(rand.nextInt()) % width);
        int y = (Math.abs(rand.nextInt()) % height);
        return getCell(x, y);
    }

    public class PhysicalFpslicCell extends PhysicalCell {
        private int x;
        private int y;
        private int col;
        private int row;
        private PhysicalNet   outputNet;
        private PhysicalNet   xin;
        private PhysicalNet   yin;
        private PhysicalNet[] local = new PhysicalNet[5];

        public PhysicalCell randomCellWithin(Random rand, double percentOfDevice) {
            int distance = (int)(percentOfDevice * Math.max(width, height));
            if (distance < 4) distance = 4;
            // FIXME ugly
            while(true) {
                int dx = (Math.abs(rand.nextInt()) % (distance*2+1));
                int dy = (Math.abs(rand.nextInt()) % (distance*2+1));
                PhysicalCell pc = getCell(col+dx-distance, row+dy-distance);
                if (pc != null) return pc;
            }
        }
        public void place(NetList.Node n) {
            this.x = col;
            this.y = row;
            FpslicDevice.Cell cell = fpslic.cell(x,y);
            cell.c(XLUT);
            cell.b(false);
            cell.f(false);
            cell.xi(NW);
            cell.yi(EAST);
            //cell.xo(true);
            //cell.yo(true);
            String type = n.getType();
            if      (type.equals("and2"))    cell.xlut(LUT_SELF & LUT_OTHER);
            else if (type.equals("or2"))     cell.xlut(LUT_SELF | LUT_OTHER);
            else if (type.equals("xor2"))    cell.xlut(LUT_SELF ^ LUT_OTHER);
            else if (type.equals("buf"))     cell.xlut(LUT_SELF);
            else if (type.equals("inv"))     cell.xlut(~LUT_SELF);
            else if (type.equals("cell0"))   return;
        }

        public FpslicDevice.Cell cell() { return fpslic.cell(col, row); }
        public PhysicalNet getNet(String name) {
            if (name.equals("out")) return outputNet;
            if (name.equals("xi"))  return xin;
            if (name.equals("yi"))  return yin;
            throw new RuntimeException("unknown");
        }

        public void setFunction(String type) {
            FpslicDevice.Cell cell = cell();
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
                new PhysicalFpslicPip(col, row, 0, 0,
                                      this+".xiNW", getCell(col-1, row+1).getNet("out"), new PhysicalNet[] { xin }, 5) {
                    public void set(boolean connected) { cell().xi(connected ? NW : NONE); }
                };
            if (getCell(col-1, row-1) != null)
                new PhysicalFpslicPip(col, row, 0, 0,
                                      this+".xiSW", getCell(col-1, row-1).getNet("out"), new PhysicalNet[] { xin }, 5) {
                    public void set(boolean connected) { cell().xi(connected ? SW : NONE); }
                };
            if (getCell(col+1, row+1) != null)
                new PhysicalFpslicPip(col, row, 0, 0,
                                      this+".xiNE", getCell(col+1, row+1).getNet("out"), new PhysicalNet[] { xin }, 5) {
                    public void set(boolean connected) { cell().xi(connected ? NE : NONE); }
                };
            if (getCell(col+1, row-1) != null)
                new PhysicalFpslicPip(col, row, 0, 0,
                                      this+".xiSE", getCell(col+1, row-1).getNet("out"), new PhysicalNet[] { xin }, 5) {
                    public void set(boolean connected) { cell().xi(connected ? SE : NONE); }
                };

            if (getCell(col-1, row) != null)
                new PhysicalFpslicPip(col, row, 0, 0,
                                      this+".yiW", getCell(col-1, row).getNet("out"), new PhysicalNet[] { yin }, 5) {
                    public void set(boolean connected) { cell().yi(connected ? WEST : NONE); }
                };
            if (getCell(col, row-1) != null)
                new PhysicalFpslicPip(col, row, 0, 0,
                                      this+".yiN", getCell(col, row-1).getNet("out"), new PhysicalNet[] { yin }, 5) {
                    public void set(boolean connected) { cell().yi(connected ? NORTH : NONE); }
                };
            if (getCell(col+1, row) != null)
                new PhysicalFpslicPip(col, row, 0, 0,
                                      this+".yiE", getCell(col+1, row).getNet("out"), new PhysicalNet[] { yin }, 5) {
                    public void set(boolean connected) { cell().yi(connected ? EAST : NONE); }
                };
            if (getCell(col, row+1) != null)
                new PhysicalFpslicPip(col, row, 0, 0,
                                      this+".yiS", getCell(col, row+1).getNet("out"), new PhysicalNet[] { yin }, 5) {
                    public void set(boolean connected) { cell().yi(connected ? SOUTH : NONE); }
                };

        }

        private PhysicalFpslicCell(int col, int row) {
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
                new PhysicalFpslicPip(col, row, -1 * (4-j) * PLANESEP, -1 * j * PLANESEP,
                                      this+".h"+i,  null,      new PhysicalNet[] {
                        local[i], getSectorWire(col, row, i, true) }) {
                    public void set(boolean connected) { cell().h(i, connected); }
                };
                new PhysicalFpslicPip(col, row, -1 * (4-j) * PLANESEP, -1 * j * PLANESEP,
                                      this+".v"+i,  null,      new PhysicalNet[] {
                        local[i], getSectorWire(col, row, i, false) }) {
                    public void set(boolean connected) { cell().v(i, connected); }
                };
                new PhysicalFpslicPip(col, row, -1 * (4-j) * PLANESEP + 15, -1 * j * PLANESEP + 15,
                                      this+".xi"+i, local[i],  new PhysicalNet[] { xin }) {
                    public void set(boolean connected) { cell().xi(connected ? i : NONE); }
                };
                new PhysicalFpslicPip(col, row, -1 * (4-j) * PLANESEP + 15, -1 * j * PLANESEP + 15,
                                      this+".yi"+i, local[i],  new PhysicalNet[] { yin }) {
                    public void set(boolean connected) { cell().yi(connected ? i : NONE); }
                };
                new PhysicalFpslicPip(col, row, -1 * (4-j) * PLANESEP + 15, -1 * j * PLANESEP + 15,
                                      this+".o"+i,  outputNet, new PhysicalNet[] { local[i] }) {
                    public void set(boolean connected) { cell().out(i, connected); }
                };
            }
        }
        public  String toString() { return "cell@("+col+","+row+")"; }
    }

    public abstract class PhysicalFpslicPip extends PhysicalPip {
        public PhysicalFpslicPip(int x, int y, int ox, int oy, String name, PhysicalNet driver, PhysicalNet[] driven, int q) {
            this(x, y, ox, oy, name, driver, driven);
        }
        public PhysicalFpslicPip(int x, int y, int ox, int oy, String name, PhysicalNet driver, PhysicalNet[] driven) {
            super(name, driver, driven);
            this.x = x;
            this.y = y;
            this.ox = ox;
            this.oy = oy;
        }
        private int ox;
        private int oy;
        private int x;
        private int y;
        public boolean prohibited() {
            return Math.abs(x-badx) <= badr && Math.abs(y-bady) <= badr;
        }
        public int getX() {
            return x * CELLSEP + ox;
        }
        public int getY() {
            return ((height * CELLSEP) - (y * CELLSEP)) + oy;
        }
        public double        getCost(PhysicalNet in, PhysicalNet out) {
            if (prohibited()) return 100 * ((2*badr)-(Math.abs(badx-x) + Math.abs(bady-y)));
            return defaultCost;
        }
        public double        getDelay(PhysicalNet in, PhysicalNet out) {
            if (prohibited()) return 10000 * defaultDelay;
            return defaultDelay;
        }
    }

    static int badx = 5;
    static int bady = 5;
    static int badr = 3;
}