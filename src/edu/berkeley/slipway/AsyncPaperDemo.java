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

    FtdiBoard fpslic;

    public AsyncPaperDemo() throws Exception {
        fpslic = new FtdiBoard();
    }

    public void main() throws Exception {

        turnOnLeds();
        setupScanCell();

        Fpslic.Cell root = fpslic.cell(2, 2);
            
        root.yo(root.north());
        root.ylut(~LUT_SELF);
        root.c(YLUT);
        root = root.north();

        root.yo(root.east());
        root.ylut(~LUT_SELF);
        root.c(YLUT);
        root = root.east();

        root.yo(root.south());
        root.ylut(~LUT_SELF);
        root.c(YLUT);
        root = root.south();

        root.yo(root.west());
        root.c(YLUT);
        root = root.west();

        root = fpslic.cell(3, 7);
        root.h(1, true);
        root.h(2, true);
        root.wi(L1);
        root.zi(L2);
        root.c(YLUT);
        root.t(TMUX_W);
        root.b(false);
        root.f(false);
        root.ylut(LUT_SELF);
        root.yi(EAST);
        root.xlut(LUT_Z);
        root.xo(false);

        root.west().out(2, true);
        root.west().h(2, true);
        root.west().c(YLUT);

        root.west().west().out(1, true);
        root.west().west().h(1, true);
        root.west().west().c(YLUT);

        root.ne().xo(root);

        createPipeline(fpslic.cell(20, 22), true, 40, true);

        for(int i=1; i<22; i+=2)
            divider(fpslic.cell(21, i));
        fpslic.cell(23,0).yo(fpslic.cell(22,0));
        fpslic.cell(21,22).yo(fpslic.cell(20,22));
        fpslic.cell(21,22).xo(fpslic.cell(20,22));

        runGui(24, 24);

        Thread.sleep(5000);

        for(int i=0; i<20; i++) test(i);
        synchronized(Demo.class) { Demo.class.wait(); }
    }

    public void test(int count) throws Exception {
        flush();
        fill(count);

        fpslic.flush();

        fpslic.readCount();
        long now = System.currentTimeMillis();
        Thread.sleep(4000);
        int save1y = fpslic.cell(19,22).ylut();
        int save1x = fpslic.cell(19,22).xlut();
        fpslic.cell(19,22).ylut(0xff);
        fpslic.cell(19,22).xlut(0xff);
        fpslic.flush();
        long then = System.currentTimeMillis();
        fpslic.cell(19,22).ylut(save1y);
        fpslic.cell(19,22).xlut(save1x);

        int tokens = fpslic.readCount();
        System.out.println(count + ", " + (tokens*1000)/(then-now));
    }

    private void flush() {
        int save1y = fpslic.cell(19,22).ylut();
        int save1x = fpslic.cell(19,22).xlut();
        int save2y = fpslic.cell(20,22).ylut();
        int save2x = fpslic.cell(20,22).xlut();
        fpslic.cell(19,22).ylut(0x00);
        fpslic.cell(19,22).xlut(0x00);
        for(int i=0; i<800; i++) {
            fpslic.cell(20,22).ylut(0xff);
            fpslic.cell(20,22).xlut(0xff);
            fpslic.cell(20,22).ylut(0x00);
            fpslic.cell(20,22).xlut(0x00);
        }
        fpslic.flush();
        fpslic.cell(20,22).ylut(save2y);
        fpslic.cell(20,22).xlut(save2x);
        fpslic.cell(19,22).ylut(save2y);
        fpslic.cell(19,22).xlut(save2x);
        fpslic.flush();
        fpslic.cell(19,22).ylut(save1y);
        fpslic.cell(19,22).xlut(save1x);
        fpslic.flush();
        fpslic.readCount();
        try { Thread.sleep(100); } catch (Exception e) { }
        int rc = fpslic.readCount();
        if (rc!=0)
            throw new Error("flush() failed => " + rc);
    }

    private void fill(int count) {
        int save1y = fpslic.cell(19,22).ylut();
        int save1x = fpslic.cell(19,22).xlut();
        int save2y = fpslic.cell(20,22).ylut();
        int save2x = fpslic.cell(20,22).xlut();
        fpslic.cell(19,22).ylut(0xff);
        fpslic.cell(19,22).xlut(0xff);
        fpslic.cell(20,22).ylut(0xff);
        fpslic.cell(20,22).xlut(0xff);
        boolean yes = true;
        for(int i=0; i<count; i++) {
            if (yes) {
                fpslic.cell(19,22).ylut(0xff);
                fpslic.cell(19,22).xlut(0xff);
            } else {
                fpslic.cell(19,22).ylut(0x00);
                fpslic.cell(19,22).xlut(0x00);
            }
            yes = !yes;
        }
        fpslic.cell(19,22).ylut(save1y);
        fpslic.cell(19,22).xlut(save1x);
        fpslic.cell(20,22).ylut(save2y);
        fpslic.cell(20,22).xlut(save2x);
        fpslic.flush();
    }

    private Fpslic.Cell pipe(Fpslic.Cell c, Fpslic.Cell prev, int[] dirs) {
        for(int i=0; i<dirs.length; i++) {
            Fpslic.Cell next = c.dir(dirs[i]);
            micropipelineStage(c, prev, next);
            prev = c;
            c = next;
        }
        return c;
    }

    private void createPipeline(Fpslic.Cell c, boolean downward, int length, boolean start) {
        length -= 8;
        if (downward) {
            if (c.row < 6) {
                c = pipe(c, c.north(), new int[] { SW, EAST, SW, WEST, NW, NORTH });
                c = c.se();
                c = pipe(c, c.north(), new int[] { NE, NORTH });
                c = c.sw().west();
                downward = false;
            } else {
                c = micropipelineStage(c, start ? c.west() : c.north(), c.sw());
                c = micropipelineStage(c, c.ne(),    c.south());
                c = micropipelineStage(c, c.north(), c.se());
                c = micropipelineStage(c, c.nw(),    c.south());
                c = c.nw();
                c = micropipelineStage(c, c.south(), c.ne());
                c = micropipelineStage(c, c.sw(),    c.north());
                c = micropipelineStage(c, c.south(), c.nw());
                micropipelineStage(c, c.se(),    start ? c.east() : c.north());
                c = c.south().south().south().south().east();
            }
        } else {
            if (c.row > c.fpslic().getHeight()-7) {
                c = pipe(c, c.south(), new int[] { NW, SOUTH });
                c = c.nw();
                c = pipe(c, c.south(), new int[] { NE, EAST, SE, WEST, SE, SOUTH });
                c = c.nw().west();
                downward = true;
            } else {
                Fpslic.Cell ret = c = pipe(c, c.south(), new int[] { NE, NORTH, NW, NORTH });
                c = c.se();
                c = pipe(c, c.north(), new int[] { SW, SOUTH, SE, SOUTH });
                c = ret;
            }
        }
        if (length >= 8) createPipeline(c, downward, length, false);
        else {
            if (downward) {
                c = micropipelineStage(c, c.north(), c.sw());
                c = micropipelineStage(c, c.ne(), c.west());
                c = micropipelineStage(c, c.east(), c.ne());
                c = micropipelineStage(c, c.sw(), c.north());
            } else {
                c = pipe(c, c.south(), new int[] { NW, EAST, SE, SOUTH });
            }
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
        switch(c.dir(next)) {
            case NORTH: case SOUTH: case EAST: case WEST:
                switch (c.dir(prev)) {
                    case NORTH: case SOUTH: case EAST: case WEST: throw new Error("cannot have prev&next both use y");
                }
                c.ylut((LUT_SELF & ~LUT_OTHER) | (LUT_Z & ~LUT_OTHER) | (LUT_Z & LUT_SELF & LUT_OTHER));
                c.xlut(LUT_Z);
                c.c(YLUT);
                c.yi(next);
                c.xi(prev);
                break;
            case NW: case SE: case SW: case NE:
                switch (c.dir(prev)) {
                    case NW: case SE: case SW: case NE: throw new Error("cannot have prev&next both use x");
                }
                c.xlut((LUT_SELF & ~LUT_OTHER) | (LUT_Z & ~LUT_OTHER) | (LUT_Z & LUT_SELF & LUT_OTHER));
                c.ylut(LUT_Z);
                c.c(XLUT);
                c.xi(next);
                c.yi(prev);
                break;
            default: throw new Error();
        }
        c.b(false);
        c.f(false);
        c.t(TMUX_FB);
        c.yo(false);
        c.xo(false);
        return next;
    }

    private void turnOnLeds() {
        for(int i=0; i<24; i++) {
            fpslic.iob_bot(i, true).enableOutput(NORTH);
            fpslic.iob_bot(i, false).enableOutput(NW);
            fpslic.cell(i, 0).xlut(0xff);
            fpslic.cell(i, 0).ylut(0xff);
        }
    }

    private void setupScanCell() {
        fpslic.cell(23,15).h(3, true);
        fpslic.cell(23,15).yi(L3);
        fpslic.cell(23,15).ylut(0xAA);
        fpslic.iob_right(15, true).enableOutput(WEST);

        fpslic.cell(23,0).ylut(0x00);
        fpslic.iob_right(0, true).enableOutput(WEST);
    }

    private void divider(Fpslic.Cell c) {
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

    private void runGui(int width, int height) throws Exception {
        Gui vis = new Gui(fpslic, fpslic, width, height);
        Frame fr = new Frame();
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


