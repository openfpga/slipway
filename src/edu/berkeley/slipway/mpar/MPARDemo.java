package edu.berkeley.slipway.mpar;
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
        NetList fnl = new NetList();

        for(Iterator<EdifCellInstance> it = (Iterator<EdifCellInstance>)env.getTopCell().cellInstanceIterator();
            it.hasNext();
            ) {
            NetList.Node n = fnl.createNode(it.next(), null);
        }

        Fpslic fpslic = new FtdiBoard();
        int width = 20;
        int height = 20;
        PhysicalDevice pd = new PhysicalFpslic(fpslic, width, height);

        int px = 0;
        int py = 0;

        // crude map
        Random rand = new Random();
        boolean[][] used = new boolean[width][height];
        for(NetList.Node n : fnl.nodes) {
            while(true) {
                px = Math.abs(rand.nextInt()) % width;
                py = Math.abs(rand.nextInt()) % height;
                if (!used[px][py]) {
                    used[px][py] = true;
                    System.out.println("placed " + n + " at ("+px+","+py+")");
                    pd.getCell(px, py).place(n);
                    break;
                }
            }
        }

        int trial = 0;
        HashSet<NetList.LogicalNet> needUnroute = new HashSet<NetList.LogicalNet>();
        while(true) {
            System.out.println();
            System.out.println("routing trial " + (++trial));
            for(NetList.LogicalNet net : fnl.nets) {
                if (net.getSize() <= 1) continue;
                net.route(fpslic, pd);
            }
            double congestion = 0;
            int overrouted = 0;
            needUnroute.clear();
            for(PhysicalDevice.PhysicalNet pn : pd) {
                if (pn.isCongested()) {
                    overrouted++;
                    congestion += pn.getCongestion();
                }
                pn.updateCongestion();
                if (pn.isCongested())
                    for(NetList.LogicalNet n : pn.getLogicalNets())
                        needUnroute.add(n);
            }
            System.out.println("  overrouted="+overrouted+", congestion="+congestion +
                               ", ripping up " + needUnroute.size() +" nets of " + fnl.nets.size());
            if (overrouted <= 0) break;
            for(NetList.LogicalNet net : needUnroute) net.unroute();
        }

        // set up scan cell
        fpslic.cell(23,15).h(3, true);
        fpslic.cell(23,15).yi(L3);
        fpslic.cell(23,15).ylut(0xAA);
        fpslic.iob_right(15, true).enableOutput(WEST);
        fpslic.cell(23,0).ylut(0x00);
        fpslic.iob_right(0, true).enableOutput(WEST);
        fpslic.flush();

        int xwidth = 8;
        while(true) {
            int a = Math.abs(rand.nextInt()) % (1 << xwidth);
            int b = Math.abs(rand.nextInt()) % (1 << xwidth);
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

        public static void setInput(NetList fnl, Fpslic fpslic, String prefix, int val) {
            for(int i=0; ; i++) {
                NetList.Node n = fnl.top.get(prefix + "["+i+"]");
                if (n==null && i==0) n = fnl.top.get(prefix);
                if (n==null) return;
                Fpslic.Cell c = n.getPlacement(fpslic);
                c.c(XLUT);
                c.b(false);
                c.xlut((val & 0x1)==0 ? 0x00 : 0xff);
                val = val >> 1;
            }
        }
        public static int getOutput(NetList fnl, Fpslic fpslic, String prefix) {
            int val = 0;
            for(int i=0; ; i++) {
                NetList.Node n = fnl.top.get(prefix+"["+i+"]");
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