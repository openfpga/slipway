package edu.berkeley.slipway;

import edu.berkeley.slipway.*;
import com.atmel.fpslic.*;
import static com.atmel.fpslic.FpslicConstants.*;
import static com.atmel.fpslic.FpslicUtil.*;
import edu.berkeley.slipway.gui.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.color.*;
import org.ibex.util.*;
import java.io.*;
import java.util.*;
import gnu.io.*;

public class AsyncPaperDemo {

    public FtdiBoard fpslic;

    public AsyncPaperDemo() throws Exception {
        fpslic = new FtdiBoard();
    }

    Fpslic.Cell start;
    public void main(String[] s) throws Exception {

        //turnOnLeds();
        setupScanCell();

        for(int i=0; i<255; i++)
            fpslic.readCount();


        //System.in.read();
        for(int i=400; i<402; i+=2) {
            go(i);
        }
        runGui(24, 24);
        System.out.println("done");

    }

    public void go(int size) throws Exception {
        start = fpslic.cell(20, 21);
        int rsize = size-createPipeline(start, true, size-4, false);
        System.out.println("actual size => " + rsize);
        pipe(start.west().north(), start.west(),
             new int[] { NE, EAST, SW, SOUTH });

        Fpslic.Cell div = start.east();
        while(true) {
            divider(div);
            if (div.south()==null || div.south().south()==null) break;
            div = div.south().south();
        }
        div = div.south().east();
        div.east().yo(div);
        //fpslic.cell(23,0).yo(fpslic.cell(22,0));
        fpslic.cell(21,22).yo(fpslic.cell(20,22));
        fpslic.cell(21,22).xo(fpslic.cell(20,22));

        reconfigTopLeft();
        reconfigTopRight();
        fpslic.flush();
        /*
        for(int i=0; i<23; i++){
            Fpslic.Cell c = fpslic.cell(0, i);
            c.ylut(0x00);
            c.c(YLUT);
            c.out(L3, true);
            c.h(L3, true);
            for(Fpslic.SectorWire sw = c.hwire(L3).east();
                sw!=null;
                sw=sw.east())
                sw.west().drives(sw, true);
        }
        */
        String sizes = rsize+"";
        while(sizes.length()<3) sizes = "0"+sizes;
        String fname = "data/size"+sizes+".csv";
        if (!new File(fname).exists()) {
            PrintWriter outfile = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fname)));
            for(int i=rsize/2; i>=0; i--) {
                test(i, rsize, outfile);
            }
            outfile.flush();
            outfile.close();
        }
        System.out.println("done.");
    }

    public void test(int count, int size, PrintWriter outfile) throws Exception {
        fpslic.flush();
        drain(count);
        fpslic.flush();
        fill(count, size);

        fpslic.flush();

        fpslic.readCount();
        long now = System.currentTimeMillis();
        Thread.sleep(1000);
        topLeft().ylut(0xff);
        topLeft().xlut(0xff);
        fpslic.flush();
        long then = System.currentTimeMillis();

        int tokens = fpslic.readCount();

        int clockrate = 24; // (in mhz)
        double elapsed = (double)((((FtdiBoard)fpslic).timer)/clockrate);
        
        double occupancy = ((double)(2*count))/((double)size);

        // eleven dividers... ...and the interrupt pin counts *pairs* of transitions
        int multiplier = 2*2*2*2*2*2*2*2*2*2*2*2;

        int clockdivisor = 64;
        multiplier /= clockdivisor;

        double result = (tokens*multiplier)/elapsed; // in millions


        // result is transitions/sec => 633mcell/sec velocity! =)
        outfile.println(occupancy + ", " + result);
        System.out.println((2*count)+"/"+size+"  "+occupancy + ", " + result/* + " @ " + elapsed*/);
        outfile.flush();
    }

    private void drain(int size) {

        while(true){
            /*
            topLeft().xlut(0x00);
            for(int i=0; i<size*4; i++) {
                topLeft().ylut(0xff);
                topLeft().ylut(0x00);
            }
            
            fpslic.flush();
            fpslic.readCount();
            try { Thread.sleep(100); } catch (Exception e) { }
            int rc = fpslic.readCount();
            if (rc!=0) { System.err.println("flush() failed REALLY BADLY => " + rc); continue; }
            reconfigTopLeft();
            */
            for(int x=0; x<24; x++)
                for(int y=0; y<24; y++) {
                    fpslic.cell(x,y).wi(L3);
                    fpslic.cell(x,y).h(L3, true);
                }
            fpslic.flush();
            for(int x=0; x<24; x++)
                for(int y=0; y<24; y++) {
                    fpslic.cell(x,y).wi(NONE);
                    //fpslic.cell(x,y).h(L3, false);
                }
            fpslic.flush();
            
            fpslic.readCount();
            try { Thread.sleep(100); } catch (Exception e) { }
            int rc = fpslic.readCount();
            if (rc!=0) {
                System.err.println("flush() failed => " + rc);
                try { Thread.sleep(1000); } catch (Exception e) { }
                continue;
            }
            break;
        }
    }

    private void fill(int count, int size) {
        //topLeft().ylut((count>0 && count<size/2-1) ? 0xff : 0x00);
        if (count>0)
            topLeft().ylut(0x00);
        boolean yes = true;
        if (count==1) {
            topLeft().xlut(0x00);
            yes = true;
        } else {
            for(int i=0; i<count-1; i++) {
                if (yes) {
                    topLeft().xlut(0xff);
                } else {
                    topLeft().xlut(0x00);
                }
                fpslic.flush();
                yes = !yes;
            //System.out.println("fill => " + yes);
            //try { Thread.sleep(500); } catch (Exception _) { }
            }
        }
        //System.out.println("done filling.");
        //try { Thread.sleep(2000); } catch (Exception _) { }

        //System.out.println("reconfigured.");
        //try { System.in.read(); }  catch (Exception _) { }


        if (count>0 && count!=size/2-1) {
            reconfigTopLeftPreserve(yes);
        } else if (count>0) {
            topLeft().xlut(0xff);
            fpslic.flush();
            topLeft().ylut(0xff);
            reconfigTopLeftPreserve(false);
        } 

        //System.out.println("running.");
        //try { System.in.read(); }  catch (Exception _) { }

        //try { Thread.sleep(2000); } catch (Exception _) { }
    }

    private Fpslic.Cell topLeft() { return start.north().north(); }
    private Fpslic.Cell topRight() { return start.north().ne(); }
    private void reconfigTopLeft() {
        Fpslic.Cell c = topLeft();
                c.c(YLUT);
                c.ylut(0x00);
                c.xlut(0x00);
                c.wi(L0);
                c.t(TMUX_W_AND_FB);
                c.ylut((LUT_SELF & ~LUT_OTHER) |
                       (LUT_Z & ~LUT_OTHER) |
                       (LUT_Z & LUT_SELF));
            fpslic.flush();
                c.xlut(LUT_Z);
            fpslic.flush();
                c.wi(NONE);
            fpslic.flush();
    }
    private void reconfigTopLeftNice() {
        Fpslic.Cell c = topLeft();
        c.c(YLUT);
        c.xlut(LUT_Z);
        fpslic.flush();
        c.ylut((LUT_SELF & ~LUT_OTHER) |
               (LUT_Z & ~LUT_OTHER) |
               (LUT_Z & LUT_SELF));
        fpslic.flush();
    }
    private void reconfigTopLeftPreserve(boolean on) {
        Fpslic.Cell c = topLeft();
        fpslic.flush();
        if (on) c.ylut(0x00);
        //else    c.ylut(0xff);
        //fpslic.flush();
        c.xlut(LUT_Z);
        fpslic.flush();
        c.ylut((LUT_SELF & ~LUT_OTHER) |
               (LUT_Z & ~LUT_OTHER) |
               (LUT_Z & LUT_SELF));
        fpslic.flush();
    }
    private void reconfigTopRight() { micropipelineStage(topRight(), topRight().west(), topRight().sw()); }

    private Fpslic.Cell pipe(Fpslic.Cell c, Fpslic.Cell prev, int[] dirs) {
        for(int i=0; i<dirs.length; i++) {
            Fpslic.Cell next = c.dir(dirs[i]);
            micropipelineStage(c, prev, next);
            prev = c;
            c = next;
        }
        return c;
    }

    private int createPipeline(Fpslic.Cell c, boolean downward, int length, boolean start) {
        boolean stop = false;
        do {
        if (downward) {
            if (c.row < 6) {
                if (length < 8+4) { stop = true; break; }
                length -= 8;
                c = pipe(c, c.north(), new int[] { SW, EAST, SW, WEST, NW, NORTH });
                c = c.se();
                c = pipe(c, c.north(), new int[] { NE, NORTH });
                c = c.sw().west();
                downward = false;
            } else {
                if (length < 8+4) { stop = true; break; }
                length -= 8;
                c = micropipelineStage(c, c.north(), c.sw());
                c = micropipelineStage(c, c.ne(),    c.south());
                c = micropipelineStage(c, c.north(), c.se());
                c = micropipelineStage(c, c.nw(),    c.south());
                c = c.nw();
                c = micropipelineStage(c, c.south(), c.ne());
                c = micropipelineStage(c, c.sw(),    c.north());
                c = micropipelineStage(c, c.south(), c.nw());
                micropipelineStage(c, c.se(),    c.north());
                c = c.south().south().south().south().east();
            }
        } else {
            if (c.row > c.fpslic().getHeight()-7) {
                if (length < 8+4) { stop = true; break; }
                length -= 8;
                c = pipe(c, c.south(), new int[] { NW, SOUTH });
                c = c.nw();
                c = pipe(c, c.south(), new int[] { NE, EAST, SE, WEST, SE, SOUTH });
                c = c.nw().west();
                downward = true;
            } else {
                if (length < 8+4) { stop = true; break; }
                length -= 8;
                Fpslic.Cell ret = c = pipe(c, c.south(), new int[] { NE, NORTH, NW, NORTH });
                c = c.se();
                c = pipe(c, c.north(), new int[] { SW, SOUTH, SE, SOUTH });
                c = ret;
            }
        }
        } while(false);
        if (stop) {
            length -= 4;
            if (downward) {
                c = micropipelineStage(c, c.north(), c.sw());
                c = micropipelineStage(c, c.ne(), c.west());
                c = micropipelineStage(c, c.east(), c.ne());
                c = micropipelineStage(c, c.sw(), c.north());
            } else {
                c = pipe(c, c.south(), new int[] { NW, EAST, SE, SOUTH });
            }
            return length;
        } else {
            return createPipeline(c, downward, length, false);
        }
    }

    /*
    private void createPipeline(Fpslic.Cell c, boolean downward, int length) {
        length -= 2;
        if (downward) {
            if (c.row == 0) {
                c = micropipelineStage(c, c.ne(),   c.west());
                c = micropipelineStage(c, c.east(), c.nw());
                if (length > 0) createPipeline(c, false, length);
            } else {
                c = micropipelineStage(c, c.ne(),   c.east());
                c = micropipelineStage(c, c.west(), c.sw());
                if (length > 0) createPipeline(c, true, length);
            }
        } else {
            if (c.row == c.fpslic().getHeight()-1) {
                c = micropipelineStage(c, c.se(),   c.west());
                c = micropipelineStage(c, c.east(), c.sw());
                if (length > 0) createPipeline(c, true, length);
            } else {
                c = micropipelineStage(c, c.se(),   c.east());
                c = micropipelineStage(c, c.west(), c.nw());
                if (length > 0) createPipeline(c, false, length);
            }
        }
    }
    */

    private Fpslic.Cell micropipelineStage(Fpslic.Cell c, Fpslic.Cell prev, Fpslic.Cell next) {
        return micropipelineStage(c, prev, next, true);
    }
    private Fpslic.Cell micropipelineStage(Fpslic.Cell c, Fpslic.Cell prev, Fpslic.Cell next, boolean configDir) {
        c.b(false);
        c.f(false);
        switch(c.dir(next)) {
            case NORTH: case SOUTH: case EAST: case WEST:
                switch (c.dir(prev)) {
                    case NORTH: case SOUTH: case EAST: case WEST: throw new Error("cannot have prev&next both use y");
                }
                if (configDir) {
                    c.yi(next);
                    c.xi(prev);
                }

        if (c.row==topLeft().row && c.col==topLeft().col) {
            c.yo(false);
            c.xo(false);
            c.c(YLUT);
        } else {
            //c.yo(true);
            c.yo(true);
            c.xo(true);
            //c.xo(false);

            c.c(ZMUX);
            c.zi(L3);
            c.h(L3, true);

            //c.c(YLUT);
            /*
            Fpslic.SectorWire sw = c.vwire(L3);
            while(sw.south() != null) {
                sw.south().drives(sw, true);
                sw = sw.south();
            }
            fpslic.cell(sw.col,0).ylut(0x00);
            fpslic.cell(sw.col,0).c(YLUT);
            fpslic.cell(sw.col,0).out(L3, true);
            fpslic.cell(sw.col,0).v(L3, true);
            */
        }


                c.ylut(0x00);
                c.xlut(0x00);
                c.wi(L0);
                c.t(TMUX_W_AND_FB);
                c.ylut((LUT_SELF & ~LUT_OTHER) |
                       (LUT_Z & ~LUT_OTHER) |
                       (LUT_Z & LUT_SELF));
                c.xlut(LUT_Z);
            fpslic.flush();
                c.wi(NONE);
            fpslic.flush();
                break;
            case NW: case SE: case SW: case NE:
                switch (c.dir(prev)) {
                    case NW: case SE: case SW: case NE: throw new Error("cannot have prev&next both use x");
                }
                if (configDir) {
                    c.xi(next);
                    c.yi(prev);
                }

        c.yo(true);
        c.xo(true);
        c.c(ZMUX);
        c.zi(NONE);

        //c.c(XLUT);
                c.xlut(0x00);
                c.ylut(0x00);
                c.wi(L0);
                c.t(TMUX_W_AND_FB);
                c.xlut((LUT_SELF & ~LUT_OTHER) |
                       (LUT_Z & ~LUT_OTHER) |
                       (LUT_Z & LUT_SELF));
                c.ylut(LUT_Z);
            fpslic.flush();
                c.wi(NONE);
            fpslic.flush();
                break;
            default: throw new Error();
        }
        //c.t(TMUX_FB);
        return next;
    }

    /*
    private void turnOnLeds() {
        for(int i=0; i<24; i++) {
            //fpslic.iob_bot(i, true).enableOutput(NORTH);
            //fpslic.iob_bot(i, false).enableOutput(NW);
            fpslic.cell(i, 0).xlut(0xff);
            fpslic.cell(i, 0).ylut(0xff);
        }
    }
    */
    private void setupScanCell() {
        fpslic.cell(23,15).h(3, true);
        fpslic.cell(23,15).yi(L3);
        fpslic.cell(23,15).ylut(0xAA);
        fpslic.iob_right(15, true).enableOutput(WEST);

        fpslic.cell(23,0).ylut(0x00);
        fpslic.iob_right(0, true).enableOutput(WEST);
        fpslic.flush();
    }

    public static void divider(Fpslic.Cell c) {
        Fpslic.Cell detect1 = c;
        Fpslic.Cell detect2 = c.east();

        detect1.yi(NORTH);
        detect1.ylut(LUT_SELF);
        detect1.xlut(LUT_OTHER & (~LUT_Z));
        detect1.c(YLUT);
        detect1.t(TMUX_FB);
        detect1.f(false);
        detect1.b(false);

        detect2.xi(NW);
        detect2.ylut(LUT_OTHER);
        detect2.xlut((~LUT_SELF) & LUT_Z);
        detect2.c(YLUT);
        detect2.t(TMUX_FB);
        detect2.f(false);
        detect2.b(false);

        detect1.south().yi(EAST);
        detect1.south().xi(NE);
        detect1.south().c(YLUT);
        detect1.south().t(TMUX_FB);
        detect1.south().f(false);
        detect1.south().b(false);
        detect1.south().ylut( (LUT_OTHER    & (~LUT_SELF)) |
                              ((~LUT_OTHER) &   LUT_Z)
                              );
        detect1.south().xlut( (LUT_SELF    & (~LUT_OTHER)) |
                              ((~LUT_SELF) &   LUT_Z)
                              );

        detect2.south().yi(WEST);
        detect2.south().xi(NW);
        detect2.south().c(YLUT);
        detect2.south().t(TMUX_FB);
        detect2.south().f(false);
        detect2.south().b(false);
        detect2.south().ylut( (LUT_OTHER    & (LUT_SELF)) |
                              ((~LUT_OTHER) &   LUT_Z)
                              );
        detect2.south().xlut( (LUT_SELF    & (~LUT_OTHER)) |
                              ((~LUT_SELF) &   LUT_Z)
                              );

    }

    public void runGui(int width, int height) throws Exception {
        Gui vis = new Gui(fpslic, fpslic, width, height);
        Frame fr = new Frame();
        fr.setTitle("SLIPWAY Live Fabric Debugger");
        fr.addKeyListener(vis);
        fr.setLayout(new BorderLayout());
        fr.add(vis, BorderLayout.CENTER);
        fr.pack();
        fr.setSize(900, 900);
        vis.repaint();
        fr.repaint();
        fr.show();
    }
}


