package edu.berkeley.slipway.demos;

import edu.berkeley.slipway.*;
import com.atmel.fpslic.*;
import static com.atmel.fpslic.FpslicConstants.*;
import edu.berkeley.slipway.gui.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.color.*;
import org.ibex.util.*;
import java.io.*;
import java.util.*;
import gnu.io.*;

public class Demo {

    public static boolean mullers = true;
    public static int masterx = 1;

    public static int PIPELEN=20;

    public static void main(String[] s) throws Exception {
        SlipwayBoard slipway = new SlipwayBoard();
        FpslicDevice device = slipway.getFpslicDevice();
        FpslicDevice at40k = device;
        try {
            Log.info(Demo.class, "issuing command");

            //at40k.iob_top(2, true).oe(false);
            //at40k.iob_top(2, false).oe(false);
            //at40k.iob_top(1, true).oe(false);

            // this command confirmed to turn *on* led0
            //at40k.iob_top(1, false).output(0);
            /*
              for(int i=0; i<20; i++) {
              at40k.iob_bot(i, false).output(0);
              at40k.iob_bot(i, true).output(0);
              }
            */

            //System.out.println("tick");
            //Thread.sleep(3000);
            //System.out.println("tick");
            //at40k.cell(0x01, 0x17).xlut((byte)0x);

            /*
              System.out.println(Integer.toString(0xff & at40k.cell(0x01, 0x17).xlut(), 16));
              System.out.println(Integer.toString(0xff & at40k.cell(0x01, 0x17).ylut(), 16));
              at40k.cell(0x01, 0x17).ylut((byte)0xff);
            */

            //at40k.cell(0x01, 0x17).wi(L1);
            /*
              System.out.println("a: " + at40k.new SectorWire(true, 0, 4, 0x17).driverRight());
              System.out.println("b: " + at40k.new SectorWire(true, 1, 4, 0x17).driverRight());
              FpslicDevice.SectorWire h0p0 = at40k.new SectorWire(true, 0, 0, 0x17);
              FpslicDevice.SectorWire h0p1 = at40k.new SectorWire(true, 1, 0, 0x17);
              FpslicDevice.SectorWire h0p2 = at40k.new SectorWire(true, 2, 0, 0x17);
              FpslicDevice.SectorWire h4p0 = at40k.new SectorWire(true, 0, 4, 0x17);
              FpslicDevice.SectorWire h4p1 = at40k.new SectorWire(true, 1, 4, 0x17);
              FpslicDevice.SectorWire h4p2 = at40k.new SectorWire(true, 2, 4, 0x17);

              //h4p1.drives(h0p1, false);
              //at40k.cell(0x04, 0x17).out(L1, false);
              //at40k.cell(0x04, 0x17).h(L0, false);

              for(int plane=0; plane<5; plane++) {
              at40k.new SectorWire(true, plane,     4, 0x17).drives(at40k.new SectorWire(true, plane,     0, 0x17), false);
              at40k.cell(0x04, 0x17).out(plane, false);
              at40k.cell(0x04, 0x17).h(plane, false);
              at40k.cell(0x01, 0x17).h(plane, false);
              }
              try { Thread.sleep(2000); } catch (Exception e) { }

              int plane=0;
              at40k.new SectorWire(true, plane, 4, 0x17).drives(at40k.new SectorWire(true, plane, 0, 0x17), true);
              at40k.cell(0x04, 0x17).out(plane, true);
              at40k.cell(0x04, 0x17).h(plane, true);
              at40k.cell(0x01, 0x17).h(plane, true);
              at40k.cell(0x01, 0x17).wi(plane);

            */

            /*
              System.out.println("xlut is " + hex(at40k.cell(0x04, 0x17).xlut()));
              System.out.println("ylut is " + hex(at40k.cell(0x04, 0x17).ylut()));
              FpslicDevice.Cell cell = at40k.cell(0x04, 0x17);
              //cell.xlut(0xff);
              //cell.f(false);
              System.out.println(cell.c());
              cell.c(YLUT);
              cell.ylut(0x4D);
              cell.xlut(0x00);

              cell.b(false);
              cell.f(false);
              //cell.t(false, false, true);
              cell.t(false, true, false);
              cell.out(L3, true);
              cell.wi(L3);

              cell.yo(false);
              cell.h(L0, false);
              cell.h(L1, false);
              cell.h(L2, false);
              cell.h(L3, false);
              cell.h(L4, false);

              for(int i=3; i>=1; i--) {
              at40k.cell(i, 0x17).yi(EAST);
              at40k.cell(i, 0x17).ylut(0x55);
              at40k.cell(i, 0x17).yo(false);
              }
            */

            //System.out.println("reading port status: " + Integer.toString(device.readFpgaData() & 0xff, 16));


            // blank these out
            /*
              at40k.cell(23, 8).ylut(0xff);
              at40k.cell(23, 11).ylut(0xff);
              at40k.iob_right(8, true).enableOutput();
              at40k.iob_right(11, true).enableOutput();
            */
            //for(int x=4;  x<=22; x++) swap(at40k.cell(x, 22), NW, NORTH);


            // entry cell: just copy X->X Y->Y
            //at40k.cell(4,23).b(false);
            //at40k.cell(4,23).yo(false);
            //at40k.cell(4,23).ylut(at40k.cell(4,23).xlut());
            //at40k.cell(4,23).xo(false);
            /*
              at40k.cell(4,23).xlut(0x55);
              at40k.cell(4,23).ylut(0x55);
            */
            /*
              at40k.cell(4,23).xlut(0x71);
              at40k.cell(4,23).ylut(0x44);
              at40k.cell(4,23).c(YLUT);
              at40k.cell(4,23).f(false);
              at40k.cell(4,23).t(false, false, true);
            */

            //for(int x=6;  x<=23; x++) copy(at40k.cell(x, 23), NW, WEST);  // top row copies to the right
            /*
              copy(at40k.cell(5, 22), NW, NORTH);
              for(int x=6;  x<=22; x++) copy(at40k.cell(x, 22), NW, WEST);  // second top row copies to the right
              //for(int y=22; y>=10; y--) copy(at40k.cell(23, y), NW, NORTH); // right edge copies down
              for(int y=21; y>=9;  y--) copy(at40k.cell(22, y), NW, NORTH); // second right edge copies down
              copy(at40k.cell(23, 9), NW, WEST);                            // second output
            */
            /*
              handshaker(at40k.cell(4,23));
              at40k.cell(4,23).xi(NW);
              at40k.cell(4,23).yi(SOUTH);

              //handshaker(at40k.cell(5,23));
              //at40k.cell(5,23).yi(NORTH);

              at40k.cell(5,23).yi(NORTH);
              at40k.cell(5,23).xlut(0x55);
              at40k.cell(5,23).xi(SW);
              at40k.cell(5,23).ylut(0x55);
              at40k.cell(5,22).yi(NORTH);
              at40k.cell(5,22).xlut(0x55);

              bounce(at40k.cell(4,22));

              // cell southeast of entry cell
              at40k.cell(3,22).xi(NE);      // NW->xin
              at40k.cell(3,22).ylut(0x33);  // xin->y
              at40k.cell(3,22).yo(false);   // y->yout
              copy(at40k.cell(3, 21), NW, NORTH);  // second top row copies to the right
              copy(at40k.cell(4, 21), NW, EAST);  // second top row copies to the right
              copy(at40k.cell(5, 21), NW, EAST);  // second top row copies to the right
              copy(at40k.cell(6, 21), NW, EAST);  // second top row copies to the right
              copy(at40k.cell(6, 22), NW, SOUTH);  // second top row copies to the right
            */
            /*
              at40k.cell(05,22).xlut(0xff);
              at40k.cell(05,22).ylut(0xff);
              at40k.cell(05,22).c(XLUT);
              at40k.cell(05,22).f(false);
              at40k.cell(05,22).b(false);
              at40k.cell(05,22).oe(NONE);
              at40k.cell(05,22).v(L3, true);
              at40k.cell(05,22).out(L3, true);
            */
            /*
              at40k.cell(4,23).ylut(~0xCC);
              at40k.cell(4,23).xlut(~0xAA);
              at40k.cell(5,23).ylut(~0xAA);
              at40k.cell(5,23).xlut(~0xAA);
              for(int i=6; i<PIPELEN+2; i++) {
              at40k.cell(i, 23).ylut(0xAA);
              at40k.cell(i, 23).xlut(0xCC);
              at40k.cell(i, 23).yi(WEST);
              }
            */

            doitx(at40k, slipway);
            Gui vis = new Gui(device, slipway);
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
            /* LAST
               System.out.println("doit");
               if (mullers) doitx(at40k, device);
               //System.out.println("counter");
               //counter(at40k, device);

               at40k.cell(21,15).yi(WEST);
               at40k.cell(21,15).ylut(0xAA);

               at40k.cell(22,15).yi(WEST);
               at40k.cell(22,15).ylut(0xAA);
            */

            FpslicDevice.Cell root = at40k.cell(10,20);
            
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

            //////////////////////////////////////////////////////////////////////////////

            at40k.cell(23,15).h(3, true);
            at40k.cell(23,15).yi(L3);
            at40k.cell(23,15).ylut(0xAA);
            at40k.iob_right(15, true).enableOutput(WEST);


            FpslicDevice.Cell c = at40k.cell(10,10);
            c.ylut(~LUT_SELF);
            c.xlut(LUT_Z);
            c.yi(WEST);
            c.c(YLUT);
            c.f(false);
            c.t(TMUX_FB);
            copy(c.west(), EAST, NW);
            copy(c.west().north().west(), SE, SE);

            c = c.east();
            c.ylut(~LUT_SELF);
            c.xlut(LUT_Z);
            c.yi(EAST);
            c.c(YLUT);
            c.f(false);
            c.t(TMUX_FB);
            copy(c.east(), WEST, SE);
            copy(c.east().south().east(), NW, NW);

            c = c.north();
            copy(c.north(), SOUTH, SOUTH);
            c.xlut((LUT_SELF & ~LUT_OTHER) | LUT_Z);
            c.ylut(LUT_Z);
            c.yi(SOUTH);
            c.c(XLUT);
            c.xi(SW);
            c.wi(L4);
            c.f(false);
            c.t(TMUX_W_AND_FB);
            c.v(L4, false);
            c.h(L4, true);
            c.v(L2, false);
            c.h(L2, true);

            c = c.west();
            copy(c.north(), SOUTH, SOUTH);
            c.xlut((LUT_SELF & ~LUT_OTHER) | LUT_Z);
            c.ylut(~LUT_Z);
            c.yi(SOUTH);
            c.xi(SE);
            c.c(XLUT);
            c.wi(L4);
            c.f(false);
            c.t(TMUX_W_AND_FB);
            c.v(L4, false);
            c.h(L4, true);
            c.v(L2, false);
            c.h(L2, true);

            c = c.west();
            c.v(L4, false);
            c.h(L4, true);
            c.out(L4, true);
            c.f(false);
            c.b(false);
            c.oe(NONE);
            c.c(YLUT);
            c.hwire(L4).west().drives(c.hwire(L4), false);
            c.hwire(L4).east().drives(c.hwire(L4), false);

            c = c.south();
            c = c.south();
            c.v(L4, false);
            c.h(L4, true);
            c.out(L4, true);
            c.f(false);
            c.b(false);
            c.oe(NONE);
            c.c(YLUT);
            c.hwire(L4).west().drives(c.hwire(L4), false);
            c.hwire(L4).east().drives(c.hwire(L4), false);

            c = c.east();
            c = c.east();
            copy(c.south(), NORTH, NORTH);
            c.xlut(((~LUT_SELF) & (~LUT_OTHER)) | LUT_Z);
            c.ylut(LUT_Z);
            c.yi(NORTH);
            c.c(XLUT);
            c.xi(NW);
            c.wi(L4);
            c.f(false);
            c.t(TMUX_W_AND_FB);
            c.v(L4, false);
            c.h(L4, true);
            c.v(L2, false);
            c.h(L2, true);

            c = c.west();
            copy(c.south(), NORTH, NORTH);
            c.xlut((LUT_SELF & LUT_OTHER) | LUT_Z);
            c.ylut(LUT_Z);
            c.yi(NORTH);
            c.xi(NE);
            c.c(XLUT);
            c.wi(L4);
            c.f(false);
            c.t(TMUX_W_AND_FB);
            c.v(L4, false);
            c.h(L4, true);
            c.v(L2, false);
            c.h(L2, true);


            // catch a rising transition
            /*
              c = c.west();
              c.v(L2, false);
              c.h(L2, true);
              c.out(L2, true);
              c.f(false);
              c.b(false);
              c.oe(NONE);
              c.c(YLUT);
            */
            c.hwire(L2).west().drives(c.hwire(L2), false);
            c.hwire(L2).east().drives(c.hwire(L2), false);



            //////

            c = at40k.cell(20,20);
            c.yi(WEST);
            c.ylut(LUT_SELF);
            c.c(YLUT);
            c.oe(H4);
            c.h(L4, true);
            c.b(false);
            c.f(false);
            c.out(L4);

            c = at40k.cell(21,20);
            c.c(YLUT);
            c.oe(NONE);
            c.h(L4, true);
            c.b(false);
            c.f(false);


            c = at40k.cell(8,8);
            c.f(true);
            c.b(true);
            c.xo(true);
            c.xi(NE);
            c.zi(L3);
            c.wi(L0);
            c.yi(NORTH);
            c.oe(H4);
            c.h(L0, true);
            c.h(L2, true);
            c.h(L4, true);
            c.v(L1, true);
            c.v(L3, true);
            c.out(L0, true);
            c.out(L1, true);
            c.out(L2, true);
            c.out(L3, true);
            c.out(L4, true);
            c.xo(true);
            c.yo(true);
            c.c(ZMUX);

            at40k.cell(9,10).xo(true);
            at40k.cell(9,10).yo(true);
            at40k.cell(9,10).c(YLUT);
            at40k.cell(9,10).b(false);

            //for(int x=5; x<PIPELEN; x++) {
            //at40k.cell(x,23).hwire(L0).drives(at40k.cell(x,23).hwire(L0).east());
            //}

            /*
              at40k.cell(22,11).ylut(0xff);
              at40k.cell(23,11).yi(L3);
              //at40k.cell(23,11).yi(WEST);
              //at40k.cell(23,11).xi(L1);
              at40k.cell(23,11).ylut(0xAA);
              at40k.iob_right(11, true).enableOutput(WEST);
              at40k.cell(23,11).v(L3, true);
              at40k.cell(23,11).yo(false);
              //at40k.flush();
              */
            int vx=04;
            int vv=23;
            /*
              System.out.println("correct: " + at40k.cell(19,15).hwire(L3) + " drives " + at40k.cell(20,15).hwire(L3));
              System.out.println("correct: " + at40k.cell(15,15).hwire(L3) + " drives " + at40k.cell(19,15).hwire(L3));
              System.out.println("correct: " + at40k.cell(11,15).hwire(L3) + " drives " + at40k.cell(15,15).hwire(L3));
              System.out.println("correct: " + at40k.cell(07,15).hwire(L3) + " drives " + at40k.cell(11,15).hwire(L3));

              at40k.cell(19,15).hwire(L3).drives(at40k.cell(20,15).hwire(L3), true);
              at40k.cell(15,15).hwire(L3).drives(at40k.cell(19,15).hwire(L3), true);
              at40k.cell(11,15).hwire(L3).drives(at40k.cell(15,15).hwire(L3), true);
              at40k.cell(07,15).hwire(L3).drives(at40k.cell(11,15).hwire(L3), true);
            */
            //at40k.cell(05,vv).xlut(0xff);
            //at40k.cell(05,vv).ylut(0xff);
            /*
              at40k.cell(vx,vv).c(YLUT);
              at40k.cell(vx,vv).f(false);
              at40k.cell(vx,vv).b(false);
              at40k.cell(vx,vv).oe(NONE);
              at40k.cell(vx,vv).v(L3, true);
              at40k.cell(vx,vv).out(L3, true);
            */
            /*
              at40k.cell(vx,15).v(L3, true);
              at40k.cell(vx,15).h(L3, true);
              at40k.cell(vx,19).vwire(L3).drives(at40k.cell(vx,15).vwire(L3), true);
              at40k.cell(vx,23).vwire(L3).drives(at40k.cell(vx,19).vwire(L3), true);
            */

            //at40k.cell(5,23).ylut(0x00);
            //at40k.cell(6,22).ylut(0xff);
            //at40k.cell(22,11).ylut(0xff);
            /*
              FpslicDevice.Cell cell = at40k.cell(4, 16);
              cell.xlut(0xff);
              cell.ylut(0xff);
              cell.b(false);
              cell.f(false);
              cell.c(XLUT);
              cell.h(L3, true);
              cell.v(L3, true);
              cell.out(L3, true);
              cell.oe(NONE);
            */
            //scan(at40k, cell, YLUT, true);
            //scan(at40k, cell, YLUT, false);

            //device.scanFPGA(true);

            at40k.cell(10,10).f(true);
            at40k.cell(10,10).c(ZMUX);

            at40k.cell(8,7).xlut(LUT_SELF);
            at40k.cell(8,7).xi(NW);

            at40k.cell(7,8).xlut(LUT_SELF & LUT_Z);
            at40k.cell(7,8).xi(SE);
            at40k.cell(7,8).c(XLUT);
            at40k.cell(7,8).f(false);
            at40k.cell(7,8).b(false);
            at40k.cell(7,8).t(TMUX_FB);
            at40k.cell(7,8).xo(false);
            System.out.println(at40k.cell(7,8).fb_relevant());

            at40k.cell(6,13).xi(SE);
            at40k.cell(6,13).c(ZMUX);
            at40k.cell(6,13).xlut(LUT_SELF);
            at40k.cell(6,13).ylut(LUT_OTHER);
            at40k.cell(6,13).xo(false);
            at40k.cell(6,13).yo(false);
            at40k.cell(7,12).xi(SE);

            for(int i=0; i<24; i++) {
                at40k.iob_bot(i, true).enableOutput(NORTH);
                at40k.iob_bot(i, false).enableOutput(NW);
                at40k.cell(i, 0).xlut(0xff);
                at40k.cell(i, 0).ylut(0xff);
            }

            device.flush();

            fr.addKeyListener(vis);
            fr.setLayout(new BorderLayout());
            fr.add(vis, BorderLayout.CENTER);
            fr.pack();
            fr.setSize(900, 900);
            vis.repaint();
            fr.repaint();
            fr.show();
            synchronized(Demo.class) { Demo.class.wait(); }



            /*
              Visualizer v = new Visualizer(at40k, device);
              v.show();
              v.setSize(1380, 1080);
              FpslicDevice.Cell cell = at40k.cell(4, 23);
            */
            //Image img = v.createImage(v.getWidth(), v.getHeight());
            /*
              int x = 1;
              int y = 14;
              cell = at40k.cell(x,y);
              scan(at40k, cell, YLUT, true);
              cell.c(YLUT);
              cell.b(false);
              cell.f(false);
              cell.oe(NONE);
              cell.ylut(0xff);
            */
            //int x = 5;
            //int y = 11;

            //selfTest(device, at40k, v);
            //System.out.println("save: " + SlipwayBoard.save + " of " + (SlipwayBoard.saveof*5));

            at40k.iob_top(0, true).enableInput();
            copy(at40k.cell(0, 23), NORTH, NORTH);
            at40k.iob_bot(0, true).enableOutput(NORTH);

            for(int i=0; i<10000; i++) {
                //v.refresh();
                try { Thread.sleep(100); } catch (Exception e) { }
            }
            //cell.ylut(0x09);

            //at40k.cell(0x01, 0x17).h(0, false);
            //at40k.cell(0x01, 0x17).xi(NE);
            //at40k.cell(0x01, 0x17).ylut((byte)0x55);

            //at40k.cell(0x04, 0x17).xlut((byte)0x10);
            //at40k.cell(0x04, 0x17).ylut((byte)0x10);
            //at40k.cell(0x04, 0x17).yo(false);
            //at40k.cell(0x04, 0x17).xo();

            /*
              at40k.cell(0x01, 0x17).xi(L0);
              at40k.cell(0x01, 0x17).h(L0, true);
            */
            /*
              at40k.cell(0x03, 0x17).xlut((byte)0x55);
              at40k.cell(0x03, 0x17).ylut((byte)0x55);
              at40k.cell(0x03, 0x17).yi(EAST);
              at40k.cell(0x03, 0x17).ylut((byte)0x55);
              at40k.cell(0x03, 0x17).yo(true);

              at40k.cell(0x03, 0x17).f(false);
              at40k.cell(0x03, 0x17).c(XLUT);
              at40k.cell(0x03, 0x17).oe(NONE);
              at40k.cell(0x03, 0x17).out(L0, true);

              at40k.cell(0x02, 0x17).yi(EAST);
              at40k.cell(0x02, 0x17).ylut((byte)0x55);
              at40k.cell(0x02, 0x17).yo(false);

              at40k.cell(0x01, 0x17).yi(EAST);
              at40k.cell(0x01, 0x17).ylut((byte)0x55);
              at40k.cell(0x01, 0x17).yo(false);

              at40k.cell(0x01, 0x17).h(L0, true);
              at40k.cell(0x01, 0x17).v(L0, false);
            */
            //at40k.cell(0x01, 0x17).yi(L0);
            //at40k.cell(0x01, 0x17).xi(L0);
            //at40k.cell(0x01, 0x17).ylut((byte)0x33);

            /*
              at40k.cell(0x03, 0x17).h(L0, true);
              at40k.cell(0x03, 0x17).out(L0, true);
              at40k.cell(0x03, 0x17).c(XLUT);
              at40k.cell(0x03, 0x17).f(false);
            */
            /*
              at40k.cell(0x01, 0x17).xin(4);
              at40k.cell(0x01, 0x17).yin(4);
              at40k.cell(0x01, 0x16).ylut((byte)0x00);
              device.mode4(2, 0x17, 0x01, 0);

              for(int i=0; i<10; i++) {
              Thread.sleep(3000);
              System.out.println("tick");
              //at40k.cell(0x01, 0x17).xlut((byte)0xFF);
              at40k.cell(0x00, 0x17).ylut((byte)0x00);
              device.getFpslicDevice().flush();
              Thread.sleep(3000);
              System.out.println("tick");
              //at40k.cell(0x01, 0x17).xlut((byte)0x00);
              at40k.cell(0x00, 0x17).ylut((byte)0xFF);
              device.getFpslicDevice().flush();
              }
            */


            /*
              at40k.iob_top(0, true).output(0);
              at40k.iob_top(0, true).oe(false);
              at40k.iob_top(0, true).pullup();
              device.getFpslicDevice().flush();
              Thread.sleep(3000);

              Log.info(Demo.class, "issuing command");
              at40k.iob_top(1, true).pulldown();
              device.getFpslicDevice().flush();
            */
            Log.info(Demo.class, "done");
            System.exit(0);
        } catch (Exception e) { e.printStackTrace(); }
    }


    public static void copy(FpslicDevice.Cell c, int xdir, int ydir) {
        switch(xdir) {
            case NW: case NE: case SW: case SE: {
                c.xi(xdir);
                c.xlut(LUT_SELF);
                break;
            }
            case NORTH: case SOUTH: case EAST: case WEST: {
                c.yi(xdir);
                c.xlut(LUT_OTHER);
                break;
            }
            case NONE: break;
            default: throw new Error();
        }
        switch(ydir) {
            case NW: case NE: case SW: case SE: {
                c.xi(ydir);
                c.ylut(LUT_OTHER);
                break;
            }
            case NORTH: case SOUTH: case EAST: case WEST: {
                c.yi(ydir);
                c.ylut(LUT_SELF);
                break;
            }
            case NONE: break;
            default: throw new Error();
        }
        c.xo(false);
        c.yo(false);
    }
    public static String hex(int x) {
        return Long.toString(x & 0xffffffffL, 16);
    }

    public static void handshaker(FpslicDevice.Cell cell) {
        cell.xlut(0x22);
        cell.ylut(0x71);
        cell.c(XLUT);
        cell.f(false);
        cell.t(false, false, true);
    }


    private static String pad(int i, String s) { if (s.length()>i) return s; return "0"+pad((i-1),s); }
    public static String bin8(byte b) {
        int n = b & 0xff;
        String ret = "";
        for(int i=7; i>=0; i--)
            ret += (n & (1<<i))==0 ? "0" : "1";
        return ret;
    }

    public static void bounce(FpslicDevice.Cell cell, int xi, int yi) {
        cell.xlut((byte)0xCC);
        cell.ylut((byte)0xCC);
        cell.xi(xi);
        cell.yi(yi);
        cell.xo(false);
        cell.yo(false);
    }
    public static void muller(FpslicDevice.Cell cell, int xi, int yi) {
        cell.ylut(0xB2);
        cell.c(YLUT);
        cell.f(false);
        cell.t(false, false, true);
        cell.xi(xi);
        cell.yi(yi);
        cell.yo(false);
        cell.xo(false);
    }

    public static int lutSwap(int x) {
        return
            (x & 0x80)        |
            ((x & 0x20) << 1) |
            ((x & 0x40) >> 1) |
            (x & 0x10) |
            (x & 0x08)        |
            ((x & 0x02) << 1) |
            ((x & 0x04) >> 1) |
            (x & 0x01);
    }

    /** watches for a rising/falling edge on Yin, emits a pulse on Xout */
    public static void pulse_detect(FpslicDevice.Cell c, int in, boolean falling) {
        c.ylut(0x00);
        c.xlut(0x00);
        switch(in) {
            case NW: case NE: case SW: case SE: {
                c.xi(in);
                loopback(c, XLUT);
                if (!falling) c.ylut(lutSwap(0x0C)); /* x & !z */
                else          c.ylut(lutSwap(0x30)); /* !x & z */
                c.xlut(LUT_SELF);
                break;
            }
            case NORTH: case SOUTH: case EAST: case WEST: {
                c.yi(in);
                loopback(c, YLUT);
                if (!falling) c.xlut(0x0C); /* y & !z */
                else          c.xlut(0x30); /* !y & z */
                c.ylut(LUT_SELF);
                break;
            }
            default: throw new Error();
        }
    }

    /** watches for a pulse on Xin, copies value of Yin */
    public static void pulse_copy(FpslicDevice.Cell cell, int xi, int yi, boolean invert) {
        loopback(cell, YLUT);
        if (!invert) cell.ylut(0xB8);   /* yo = x ?  yi : z => 1011 1000 */
        else         cell.ylut(0x74);   /* yo = x ? !yi : z => 0111 0100 */
        if (!invert) cell.xlut(lutSwap(0xB8));   /* yo = x ?  yi : z => 1011 1000 */
        else         cell.xlut(lutSwap(0x74));   /* yo = x ? !yi : z => 0111 0100 */
        cell.xi(xi);
        cell.yi(yi);
    }

    public static void loopback(FpslicDevice.Cell cell, int cin) {
        cell.f(false);
        cell.b(false);
        cell.t(false, false, true);
        cell.yo(false);
        cell.xo(false);
        cell.c(cin);
    }

    public static void doit(FpslicDevice at40k, SlipwayBoard device) throws Exception {

        FpslicDevice.Cell led = at40k.cell(1, 23);
        led.v(L2, true);
        led.h(L2, false);
        led.yi(L2);
        led.ylut(~LUT_SELF);
        led.xlut(LUT_SELF);
        led.yo(false);

        FpslicDevice.Cell c = at40k.cell(1, 22);
        c.out(L1, true);
        c.out(L0, true);
        c.oe(V4);
        c.ylut(0xff);
        c.h(L1, true);
        c.h(L0, false);

        c.v(L0, /*false*/true);

        c.v(L1, true);
        c.f(false);
        c.b(false);
        c.c(YLUT);

        for(int i=0; i<4; i++) at40k.cell(i, 20).h(L0, false);
        FpslicDevice.Cell z = at40k.cell(1, 20);
        z.out(L0, true);
        z.xlut(0xff);
        z.c(XLUT);
        z.yi(L0);
        z.ylut(~LUT_SELF);
        z.v(L0, true);
        //z.h(L0, true);
        z.h(L0, false);
        z.f(false);
        z.b(false);
        z.hwire(L0).east().drives(z.hwire(L0), false);
        z.hwire(L1).east().drives(z.hwire(L1), false);
        z.vwire(L0).south().drives(z.vwire(L0), false);
        z.vwire(L1).south().drives(z.vwire(L1), false);
        z.oe(H4);

        z = at40k.cell(0, 20);
        z.oe(NONE);
        z.out(L0, true);
        z.out(L1, true);
        z.out(L2, true);
        //z.out(L3, true);
        z.out(L4, true);
        z.h(L0, true);
        z.h(L1, true);
        z.h(L2, true);
        //z.h(L3, true);
        z.h(L4, true);
        z.f(false);
        z.b(false);
        z.yi(EAST);
        z.ylut(LUT_SELF);
        z.c(YLUT);

        for(int y=20; y<=22; y++)
            for(int x=2; x<=5; x++) {
                c = at40k.cell(x, y);
                copy(c, NW, WEST);
            }

        //c = at40k.cell(2, 22);
        //c.h(L0, true);
        //c.yi(L0);

        c = at40k.cell(1, 21);
        c.v(L0, true);
        c.v(L2, true);
        c.yi(L0);
        c.out(L2, true);
        c.ylut(LUT_SELF);
        c.c(YLUT);
        c.b(false);
        c.f(false);
        c.oe(NONE);
        c.yo(false);

        

        c = at40k.cell(13, 22);
        c.xlut(LUT_OTHER | 0xF0);
        c.c(XLUT);
        c.t(false, false, true);
        c.b(false);
        c.f(false);
        c.ylut(0xF0);
        c.yi(EAST);
        c.yo(false);
        /*
        // this gate detects a rising edge on its Xin (delayed copy on Yin); when viewed, it inverts its state
        c = at40k.cell(14, 22);
        c.ylut(0x00);
        c.c(XLUT);
        c.f(false);
        c.b(false);
        c.t(false, false, true);
        c.xi(SE);
        c.yi(SOUTH);
        c.yo(false);
        c.xo(false);
        c.ylut(0xA6); // (x & !z) ? ~y : y
        c.xlut(LUT_SELF); 

        c = at40k.cell(14, 20);
        c.ylut(LUT_OTHER);
        c.xi(NE);
        c = at40k.cell(14, 21);
        c.ylut(LUT_SELF);
        c.xi(SOUTH);

        c = at40k.cell(13, 22);
        c.xlut(0x00);
        c.xlut(LUT_OTHER);// | 0xF0);
        */
        //c = at40k.cell(13, 22);
        //copy(c, NW, EAST);
        /*
          c.ylut(0x00);
          c.c(YLUT);
          c.f(false);
          c.b(false);
          c.t(false, false, true);
          c.xi(SE);
          c.yi(NORTH);
          c.yo(false);
          c.xo(false);
          c.ylut(0x54);  // (x || z) & !y
        */

        /*        
                  c = at40k.cell(2, 21);
                  c.ylut(0x00);
                  c.c(YLUT);
                  c.f(false);
                  c.b(false);
                  c.t(false, false, true);
                  c.xi(SE);
                  c.yi(WEST);
                  c.yo(false);
                  c.xo(false);
                  c.ylut(0xE8);
 
                  //at40k.cell(2, 21).xlut(0xF0);

                  at40k.cell(3, 22).ylut(LUT_OTHER);
                  at40k.cell(3, 22).xi(SW);
        */
        //at40k.iob_top(5, true).enableOutput(SOUTH);
        //at40k.iob_top(5, false).enableOutput(SE);
    }

    public static int yofs = mullers ? 19 : 22;
    public static void counter(FpslicDevice at40k, SlipwayBoard device) throws Exception {
        // watch for rising edge from south, emit pulse on Xout (to NE)
        //copy(at40k.cell(16,23), SW, WEST);
        
        for(int x=22; x>=1; x-=2) {
            pulse_detect(at40k.cell(x, yofs), SE,      false);
            pulse_detect(at40k.cell(x, yofs-1), EAST,    true);
            pulse_copy(at40k.cell(x-1, yofs), SE, SOUTH, false);
            pulse_copy(at40k.cell(x-1, yofs-1), NE, NORTH, true);

            //pulse_detect(at40k.cell(15, 22), NORTH, false);
            //pulse_detect(at40k.cell(16, 22), NW,    true);
            //pulse_copy(at40k.cell(16, 21), NW, WEST, false);
            //pulse_copy(at40k.cell(15, 21), NE, EAST, true);
        }
        for(int x=23; x>1; x-=2) {
            pulse_detect(at40k.cell(x-1, yofs-2), SW,    false);
            pulse_detect(at40k.cell(x-1, yofs-3), WEST,  true);
            pulse_copy(at40k.cell(x, yofs-2), SW, SOUTH, false);
            pulse_copy(at40k.cell(x, yofs-3), NW, NORTH, true);

            //pulse_detect(at40k.cell(15, 22), NORTH, false);
            //pulse_detect(at40k.cell(16, 22), NW,    true);
            //pulse_copy(at40k.cell(16, 21), NW, WEST, false);
            //pulse_copy(at40k.cell(15, 21), NE, EAST, true);
        }
        copy(at40k.cell(1, yofs-2), SOUTH, SOUTH);
        copy(at40k.cell(1, yofs-3), NORTH, NORTH);
        at40k.cell(1, yofs-3).ylut(~at40k.cell(1, yofs-3).ylut());
        at40k.cell(1, yofs-3).xlut(~at40k.cell(1, yofs-3).xlut());

        copy(at40k.cell(23, yofs), SOUTH, SOUTH);
        copy(at40k.cell(23, yofs-1), SOUTH, SOUTH);

        for(int i=23; i>yofs; i--) copy(at40k.cell(1, i), SOUTH, SOUTH);

        //at40k.iob_top(1, true).slew(SLOW);
        //at40k.iob_top(1, false).slew(SLOW);

    }

    public static void fill(FpslicDevice at40k, SlipwayBoard device, int num) throws Exception {
        //muller(at40k.cell(PIPELEN,22), NE, WEST);
        FpslicDevice.Cell a = at40k.cell(10,22);
        FpslicDevice.Cell b = at40k.cell(11,22);
        a.ylut(0x00);
        for(int i=0; i<num; i++) {
            //System.out.println(i);
            b.lut(0xff, 0xff);
            device.getFpslicDevice().flush();
            try { Thread.sleep(1); } catch (Exception e) { }
            b.lut(0x00, 0x00);
            device.getFpslicDevice().flush();
            try { Thread.sleep(1); } catch (Exception e) { }
        }
        b.ylut(0xB2);
        a.ylut(0xB2);
    }

    public static void drain(FpslicDevice at40k, SlipwayBoard device) throws Exception {
        FpslicDevice.Cell a = at40k.cell(10,22);
        FpslicDevice.Cell b = at40k.cell(11,22);
        a.lut(0x00, 0x00);
        b.lut(0x00, 0x00);
        for(int i=0; i<30; i++) {
            //System.out.println(i);
            a.lut(0xff, 0xff);
            device.getFpslicDevice().flush();
            try { Thread.sleep(1); } catch (Exception e) { }
            a.lut(0x00, 0x00);
            device.getFpslicDevice().flush();
            try { Thread.sleep(1); } catch (Exception e) { }
        }
        b.ylut(0xB2);
        a.ylut(0xB2);
    }

    public static void doitx(FpslicDevice at40k, SlipwayBoard device) throws Exception {
        for(int i=5; i<PIPELEN+1; i++) bounce(at40k.cell(i, 23), SE,                     SOUTH);
        for(int x=5; x<PIPELEN;   x++) muller(at40k.cell(x, 22), x==PIPELEN-1 ? SE : NE, WEST);
        
        bounce(at40k.cell(PIPELEN,  21), NW, WEST);
        
        for(int x=5; x<PIPELEN;   x++) muller(at40k.cell(x, 21), SW,                     x==PIPELEN-1 ? NORTH : EAST);
        for(int x=4; x<PIPELEN+1; x++) bounce(at40k.cell(x, 20), NW,                     NORTH);
        
        bounce(at40k.cell(4, 22), SE, EAST);
        //muller(at40k.cell(4PIPELEN-1,21), SW, NORTH);
        
        //muller(at40k.cell(4,22), NE, WEST);
        //at40k.cell(4,22).ylut(0xEE);
        muller(at40k.cell(5, 22), NE, SOUTH);
        muller(at40k.cell(5, 21), NW, EAST);
        /*
          for(int x=4; x>=0; x--) {
          at40k.cell(x, 21).ylut(0xAA);
          at40k.cell(x, 21).yi(EAST);
          at40k.cell(x, 21).yo(false);
          }

          at40k.cell(0, 22).ylut(0xAA);
          at40k.cell(0, 22).yi(SOUTH);
          at40k.cell(0, 22).yo(false);

          at40k.cell(0, 23).ylut(~0xAA);
          at40k.cell(0, 23).xlut(~0xcc);
          at40k.cell(0, 23).yi(SOUTH);
          at40k.cell(0, 23).yo(false);
        */
        for(int x=3; x<=23; x+=2) {
            pulse_detect(at40k.cell(x-1, 19), SW,    false);
            pulse_detect(at40k.cell(x-1, 18), WEST,  true);
            pulse_copy(at40k.cell(x, 19), SW, SOUTH, false);
            pulse_copy(at40k.cell(x, 18), NW, NORTH, true);

            if (x<17) {
                pulse_detect(at40k.cell(x-1, 16), SW,    false);
                pulse_detect(at40k.cell(x-1, 15), WEST,  true);
                pulse_copy(at40k.cell(x, 16), SW, SOUTH, false);
                pulse_copy(at40k.cell(x, 15), NW, NORTH, true);
            }
            //pulse_detect(at40k.cell(15, 22), NORTH, false);
            //pulse_detect(at40k.cell(16, 22), NW,    true);
            //pulse_copy(at40k.cell(16, 21), NW, WEST, false);
            //pulse_copy(at40k.cell(15, 21), NE, EAST, true);
        }
        for(int x=14; x>=1; x--)
            copy(at40k.cell(x, 17), EAST, EAST);
        for(int x=4; x>=0; x--)
            copy(at40k.cell(x, 21), EAST, EAST);
        copy(at40k.cell(13, 17), SOUTH, SOUTH);

        copy(at40k.cell(0, 20), NORTH, NORTH);
        copy(at40k.cell(0, 19), NORTH, NORTH);
        copy(at40k.cell(0, 18), NORTH, NORTH);
        copy(at40k.cell(0, 17), NORTH, NORTH);
        copy(at40k.cell(0, 16), NORTH, NORTH);
        copy(at40k.cell(1, 16), WEST, WEST);
        copy(at40k.cell(1, 15), NORTH, NORTH);

        copy(at40k.cell(1, 20), SOUTH, SOUTH);
        copy(at40k.cell(1, 19), SOUTH, SOUTH);
        copy(at40k.cell(1, 18), SOUTH, SOUTH);

        for(int y=20; y<=23; y++)
            copy(at40k.cell(23, y), SOUTH, SOUTH);


        //for(int x=19; x<=23; x++)
        //copy(at40k.cell(x, 0), WEST, WEST);
        //copy(at40k.cell(18, 19), NW, NW);
        //at40k.iob_top(5, true).enableOutput(SOUTH);
        //at40k.iob_top(5, false).enableOutput(SOUTH);
    }
}
