package edu.berkeley.slipway.demos;

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

public class Demo4 implements KeyListener {

    public static void main(String[] s) throws Exception {
        new Demo4().go(); 
    }
    public SlipwayBoard board;
    public FpslicDevice device;
    public Demo4() throws Exception {
        board = new SlipwayBoard();
        device = (FpslicDevice)board.getDevice();
    }
    public void go() throws Exception {
        long begin = System.currentTimeMillis();
        //FpslicUtil.readMode4(new ProgressInputStream("configuring fabric", System.in, 111740), device);
        long end = System.currentTimeMillis();
        Log.info(Demo.class, "finished in " + ((end-begin)/1000) + "s");
        Thread.sleep(1000);


        Log.info(Demo.class, "issuing command");

        FpslicDevice.Cell root = device.cell(5,5);

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
        FpslicDevice.Cell c = device.cell(12, 0);
        c.xo(c.east());
        while(c.east() != null && c.east().east() != null) {
            c.yo(c.east());
            c = c.east();
        }
        device.flush();

        FpslicDevice.Cell div = device.cell(19, 21);
        while(true) {
            AsyncPaperDemo.divider(div);
            div = div.south().south();
            if (div == null) break;
        }
        device.flush();

        int MAX=17;
        for(int x=2; x<MAX+1; x++) {
            c = device.cell(x, 20);
            FpslicDevice.Cell bridge = x==2 ? c.sw()    : c.nw();
            FpslicDevice.Cell pred   = x==MAX ? c.south() : c.east();
            FpslicDevice.Cell next   = x==2 ? c.south() : c.west();
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

        vis = new Gui3(device, board);
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
    public void muller(FpslicDevice.Cell c, FpslicDevice.Cell pred, FpslicDevice.Cell bridge, FpslicDevice.Cell next) {
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
        FpslicDevice fpslic = (FpslicDevice)device;
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
            final FpslicDevice.Cell cell = (FpslicDevice.Cell)(Object)c.fpslicCell;
            scan(device, cell, YLUT, true);
            int x = cell.col;
            int y = cell.row;
            board.readFpgaData(new BCB(c));
            scan(device, cell, YLUT, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void scan(FpslicDevice dev, FpslicDevice.Cell cell, int source, boolean setup) {
        ExperimentUtils.scan(cell, source, setup);
    }


    private class BCB extends SlipwayBoard.ByteCallback {
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
