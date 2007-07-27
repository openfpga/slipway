package edu.berkeley.slipway;

import static java.awt.event.KeyEvent.*;
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

public class Demo2 implements KeyListener {

    public static void main(String[] s) throws Exception {
        new Demo2().go(); 
    }
    public FtdiBoard device;
    public Demo2() throws Exception {
        device = new FtdiBoard();
    }
    public void go() throws Exception {
        long begin = System.currentTimeMillis();
        //FpslicUtil.readMode4(new ProgressInputStream("configuring fabric", System.in, 111740), device);
        long end = System.currentTimeMillis();
        Log.info(Demo.class, "finished in " + ((end-begin)/1000) + "s");
        Thread.sleep(1000);


        Log.info(Demo.class, "issuing command");

        Fpslic.Cell root = device.cell(5,5);

        root.ylut(LUT_SELF);
        root.yi(NORTH);
        root.xi(NW);
        root.wi(L0);
        root.zi(L2);

        root = root.north();
        root.ylut(LUT_SELF);
        root.yi(WEST);
        root.out(1, true);
        root.h(1, true);
        root.wi(L1);
        root.zi(L3);

        root = root.west();
        root.xi(SE);
        root.ylut(LUT_SELF);
        root.yi(SOUTH);
        root.wi(L2);
        root.zi(L4);

        root = root.south();
        root.ylut(LUT_SELF);
        root.yi(EAST);
        root.wi(L3);
        root.zi(L0);
        //root = root.n();

        device.iob_bot(12, false).enableOutput(NW);
        Fpslic.Cell c = device.cell(12, 0);
        c.xo(c.east());
        while(c.east() != null && c.east().east() != null) {
            c.yo(c.east());
            c = c.east();
        }
        device.flush();

        Fpslic.Cell div = device.cell(19, 21);
        while(true) {
            AsyncPaperDemo.divider(div);
            div = div.south().south();
            if (div == null) break;
        }
        device.flush();

        int MAX=17;
        for(int x=2; x<MAX+1; x++) {
            c = device.cell(x, 20);
            Fpslic.Cell bridge = x==2 ? c.sw()    : c.nw();
            Fpslic.Cell pred   = x==MAX ? c.south() : c.east();
            Fpslic.Cell next   = x==2 ? c.south() : c.west();
            muller(c, pred, bridge, next);

            c = c.south();
            bridge = x==MAX ? c.ne()    : c.se();
            pred   = x==2 ? c.north() : c.west();
            next   = x==MAX ? c.north() : c.east();
            muller(c, pred, bridge, next);
        }
        //device.cell(MAX+0,20).yi(WEST);
        //device.cell(MAX+0,20).ylut(LUT_SELF);
        //device.cell(MAX+1,20).yi(WEST);
        //device.cell(MAX+1,20).ylut(LUT_SELF);
        device.cell(MAX+2,20).yi(WEST);
        device.cell(MAX+2,20).ylut(LUT_SELF);
        device.cell(MAX+2,20).xlut(LUT_OTHER);
        device.cell(18,20).ylut(LUT_SELF);
        device.flush();
        go2();
    }

    public void go2() throws Exception {
        setupScanCell();
        device.flush();

        vis = new Gui3((Fpslic)device, (FtdiBoard)device);
        vis.addKeyListener(this);
        Frame fr = new Frame();
        fr.setLayout(new BorderLayout());
        fr.add(vis, BorderLayout.CENTER);
        fr.pack();
        fr.setSize(900, 900);
        vis.repaint();
        fr.repaint();
        fr.show();
        //synchronized(Demo.class) { Demo.class.wait(); }
        while(true) {
            try { Thread.sleep(500); } catch (Exception e) { }
            synchronized(vis) {
                scan();
            }
        }
    }
    Gui3 vis;
    public void muller(Fpslic.Cell c, Fpslic.Cell pred, Fpslic.Cell bridge, Fpslic.Cell next) {
        bridge.yi(next);
        bridge.xlut(LUT_OTHER);

        c.yi(pred);
        c.xi(bridge);
        c.b(false);
        c.f(false);
        c.c(YLUT);
        c.t(TMUX_FB);
        c.ylut((LUT_SELF & ~LUT_OTHER) |
               (LUT_Z    & ~LUT_OTHER) |
               (LUT_Z    &   LUT_SELF));
    }

    public void setupScanCell() {
        Fpslic fpslic = (Fpslic)device;
        fpslic.cell(23,15).h(3, true);
        fpslic.cell(23,15).yi(L3);
        fpslic.cell(23,15).ylut(0xAA);
        fpslic.iob_right(15, true).enableOutput(WEST);

        fpslic.cell(23,0).ylut(0x00);
        fpslic.iob_right(0, true).enableOutput(WEST);
        fpslic.flush();
    }

    public void keyTyped(KeyEvent k) { }
    public void keyReleased(KeyEvent k) { }
    public void keyPressed(KeyEvent k) {
        switch(k.getKeyCode()) {
            case VK_SPACE:
                scan();
                break;
        }
    }
    public void scan() {
        for(int x=0; x<4; x++)
            for(int y=0; y<4; y++)
                scan(vis.ca[x][y]);
        for(int x=0; x<4; x++)
            for(int y=0; y<4; y++)
                scan(vis.ca[x][y]);
    }
    public void scan(final GuiCell c) {
        try {
            final Fpslic.Cell cell = c.fpslicCell;
            scan(device, cell, YLUT, true);
            int x = cell.col;
            int y = cell.row;
            device.readBus(new BCB(c));
            scan(device, cell, YLUT, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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


    private class BCB extends FtdiBoard.ByteCallback {
        GuiCell c;
        public BCB(GuiCell c) {
            this.c = c;
        }
        public void call(byte b) throws Exception {
            boolean on = (b & 0x80) != 0;
            c.val = on;
            vis.repaint();
        }
    }
}
