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
import static edu.berkeley.slipway.mpar.PhysicalFpslic.*;

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


    public static final double alphaParameter    =   0.9;
    public static final double betaParameter     =   0.5;
    public static final double wireCost          =   0.005;  
    public static final double wireCostPlacement =   0.01;   // cost of an uncongested net (cost of congested net is 1)
    //public static final double gammaParameter  =   1.0;
    public static final double lambda            =  0.7;

    public static double temperature = 300.0;
    public static double congestion = 0;
    public static double timingpenalty = 0;

    public static void main(String[] s) throws Exception {

        NetList fnl = new NetList(s[0]);
        int width = 12;
        int height = 12;

        //SlipwayBoard slipway = new SlipwayBoard();
        Board slipway = new FakeBoard(24, 24);

        FpslicDevice fpslic = (FpslicDevice)slipway.getDevice();
        while(true) {
        PhysicalDevice pd = new PhysicalFpslic(fpslic, width, height);

        Placement placement = new Placement(fnl, pd);
        Routing routing = new Routing(placement);
        Random rand = new Random();
        placement.random(rand);
        routing.routeAll();

        int trial = 0;
        int num_moves = width*height;

        Visualization vis = new Visualization((PhysicalFpslic)pd);

        boolean alldone = false;
        long lastDraw = System.currentTimeMillis();
        OUT: while(true) {
            System.out.println();
            double max_swap_dist = 3 * Math.max(width,height) * Math.exp(-1 / temperature);
            System.out.println("round " + (++trial) + "  (temp="+temperature+", maxdist="+max_swap_dist+")");
            
            if (alldone) break;

            congestion = routing.measureCongestion();
            timingpenalty = routing.measureTimingpenalty();
            double wirecost = routing.measureWireCost();
            int swaps = 0;
            num_moves = 200;

            for(int i=0; i<num_moves; i++) {
                System.out.print("\r  [place: " + i + "/" + num_moves + "  congestion="+congestion+"]");
                NetList.Node node1 = fnl.randomNode(rand);
                PhysicalDevice.PhysicalCell cell1 = placement.nodeToCell(node1);
                PhysicalDevice.PhysicalCell cell2 = cell1.randomCellWithin(rand, max_swap_dist);
                NetList.Node node2 = placement.cellToNode(cell2);

                // FIXME: cache and reuse "newrouting"
                // also: fold down newrouting to collapse parentage
                routing.checkpoint();
                routing.unroute(node1);
                routing.unroute(node2);
                placement.unplace(cell1);
                placement.unplace(cell2);
                placement.place(node1, cell2);
                placement.place(node2, cell1);
                routing.routeAll();

                double newcongestion = routing.measureCongestion();
                double newwirecost = routing.measureWireCost();
                double newtimingpenalty = routing.measureTimingpenalty();
                double lam = 0.1;
                double deltaCost =
                    lam * ((newcongestion - congestion) / Math.max(0.001, congestion))
                    +
                    //(1-lam) * ((newwirecost - wirecost) / wirecost)
                    //+
                    (1-lam) * ((newtimingpenalty - timingpenalty) / Math.max(0.001, timingpenalty))
                    ;
                double swapProbability = Math.exp((-1 * deltaCost) / temperature);
                //double dist = Math.sqrt( (5-p2x)*(5-p2x) + (5-p2y)*(5-p2y) );
                double rad = 4;
                /*
                if (rand2 == null)
                    swapProbability = Math.max(0, Math.min(swapProbability, dist / Math.sqrt(rad+rad)));
                */
                boolean doSwap = Math.random() < swapProbability;
                if (doSwap) {
                    swaps++;
                    congestion = newcongestion;
                    wirecost = newwirecost;
                    timingpenalty = newtimingpenalty;
                } else {
                    placement.unplace(cell1);
                    placement.unplace(cell2);
                    placement.place(node1, cell1);
                    placement.place(node2, cell2);
                    routing.rollback();
                }
            }
            double acceptance = ((double)swaps) / num_moves;
            double gamma = 0;
            if (acceptance > 0.96) gamma = 0.5;
            else if (acceptance > 0.8) gamma = 0.9;
            else if (acceptance > 0.15) gamma = 0.95;
            else gamma = 0.8;

            System.out.println("  acceptance="+acceptance);
            temperature = temperature * gamma;
            int num_routes = num_moves - swaps;
            num_routes = 2;
            for(int i=0; i<num_routes; i++) {
                int overrouted = routing.measureOverloaded();
                routing.unrouteOverloaded();
                routing.reRouteAll();
                System.out.print("\r  [route "+i+"/"+num_routes+"] overrouted="+overrouted+" congestion="+routing.measureCongestion()+"     penalty="+timingpenalty);
                routing.updateCongestion(alphaParameter, betaParameter);
                if (overrouted==0 && timingpenalty < 1) alldone = true;
            }
            num_moves = (int)(300 * gamma * gamma);
            vis.draw(placement, routing, false);
            System.out.println();
        }
        vis.draw(placement, routing, true);
        placement.setPlacement();
        routing.setPips(true);
        System.out.println("wire utilization: " + Math.round(routing.measureWireUtilization()*100));

        // set up scan cell
        fpslic.cell(23,15).h(3, true);
        fpslic.cell(23,15).yi(L3);
        fpslic.cell(23,15).ylut(0xAA);
        fpslic.iob_right(15, true).enableOutput(WEST);
        fpslic.cell(23,0).ylut(0x00);
        fpslic.iob_right(0, true).enableOutput(WEST);
        fpslic.flush();
        /*
        int xwidth = 8;
        temperature = 0;
        while(temperature==0) {
            int a = Math.abs(rand.nextInt()) % (1 << xwidth);
            int b = Math.abs(rand.nextInt()) % (1 << xwidth);
            setInput(fnl, fpslic, "a",  a, placement);
            setInput(fnl, fpslic, "b",  b, placement);
            setInput(fnl, fpslic, "ci", 0, placement);
            int result = getOutput(fnl, fpslic, slipway, "out", placement);
            int expect = (a+b) & ~(-1 << (xwidth+1));
            System.out.println(Integer.toString(a,16) + " + " +
                               Integer.toString(b,16) + " = " +
                               Integer.toString(result,16) +
                               " [ " + (expect==result ? "ok" : "bad" ) + " ] " + (Integer.toString((result^(expect)),16)));
        }
        */
    }
    }


    private static int ret;
    public static synchronized int scan(final FpslicDevice device, final SlipwayBoard slipway, final FpslicDevice.Cell cell) {
        try {
            scan(device, cell, XLUT, true);
            slipway.readFpgaData(new SlipwayBoard.ByteCallback() {
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
            scan(device, cell, XLUT, false);
            return ret;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static void scan(FpslicDevice dev, FpslicDevice.Cell cell, int source, boolean setup) {
        if (setup) {
            //if (source != NONE) cell.c(source);
            if (cell.b()) cell.b(false);
            if (cell.f()) cell.f(false);
        }
        if (cell.out(L3)!=setup) cell.out(L3, setup);
        if (cell.vx(L3)!=setup) cell.v(L3, setup);

        FpslicDevice.SectorWire sw = cell.vwire(L3);
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

    public static void setInput(NetList fnl, FpslicDevice fpslic, String prefix, int val, Placement placement) {
        for(int i=0; ; i++) {
            NetList.Node n = fnl.top.get(prefix + "["+i+"]");
            if (n==null && i==0) n = fnl.top.get(prefix);
            if (n==null) return;
            FpslicDevice.Cell c = ((PhysicalFpslic.PhysicalFpslicCell)placement.nodeToCell(n)).cell();
            c.c(XLUT);
            c.b(false);
            c.xlut((val & 0x1)==0 ? 0x00 : 0xff);
            val = val >> 1;
        }
    }
    public static int getOutput(NetList fnl, FpslicDevice fpslic, SlipwayBoard slipway, String prefix, Placement placement) {
            int val = 0;
            for(int i=0; ; i++) {
                NetList.Node n = fnl.top.get(prefix+"["+i+"]");
                if (n==null && i==0) n = fnl.top.get(prefix);
                if (n==null) return val;
                FpslicDevice.Cell c = ((PhysicalFpslic.PhysicalFpslicCell)placement.nodeToCell(n)).cell();
                c.xlut(LUT_SELF);
                c.c(XLUT);
                c.b(false);
                fpslic.flush();
                int scan = scan(fpslic, slipway, c);
                val |= ((scan==0 ? 0 : 1) << i);
            }
        }


}