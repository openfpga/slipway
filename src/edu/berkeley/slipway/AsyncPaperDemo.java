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

        runGui(24, 24);
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
        synchronized(Demo.class) { Demo.class.wait(); }
    }
}


