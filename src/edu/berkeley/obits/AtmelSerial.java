package edu.berkeley.obits;

import static edu.berkeley.obits.device.atmel.AtmelDevice.Constants.*;
import edu.berkeley.obits.device.atmel.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.color.*;
import org.ibex.util.*;
import java.io.*;
import java.util.*;
import gnu.io.*;

public class AtmelSerial {

    public static SerialPort detectObitsPort() throws Exception {
        Enumeration e = CommPortIdentifier.getPortIdentifiers();
        while(e.hasMoreElements()) {
            CommPortIdentifier cpi = (CommPortIdentifier)e.nextElement();
            Log.info(AtmelSerial.class, "trying " + cpi.getName());
            if (cpi.getName().startsWith("/dev/cu.usbserial-")) return new RXTXPort(cpi.getName());
            if (cpi.getName().startsWith("/dev/ttyS0")) return new RXTXPort(cpi.getName());
        }
        Log.info(AtmelSerial.class, "returning null...");
        return null;
    }
    public static int PIPELEN=20;
    public static void main(String[] s) throws Exception {
        AvrDrone device = new AvrDrone(detectObitsPort());
        At40k at40k = new At40k.At40k10(device);
        int count = 0;
        try {
            long begin = System.currentTimeMillis();
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            for(String str = br.readLine(); str != null; str = br.readLine()) {
                long foo = Long.parseLong(str, 16);
                device.mode4((int)(foo >> 24), (int)(foo >> 16), (int)(foo >>  8), (int)(foo >>  0));
                count++;
                if (count % 100 == 0) Log.info(AtmelSerial.class, "wrote " + count + " configuration octets");
            }
            device.flush();
            long end = System.currentTimeMillis();
            Log.info(AtmelSerial.class, "finished in " + ((end-begin)/1000) + "s");
            Thread.sleep(1000);
            Log.info(AtmelSerial.class, "issuing command");

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
            At40k.SectorWire h0p0 = at40k.new SectorWire(true, 0, 0, 0x17);
            At40k.SectorWire h0p1 = at40k.new SectorWire(true, 1, 0, 0x17);
            At40k.SectorWire h0p2 = at40k.new SectorWire(true, 2, 0, 0x17);
            At40k.SectorWire h4p0 = at40k.new SectorWire(true, 0, 4, 0x17);
            At40k.SectorWire h4p1 = at40k.new SectorWire(true, 1, 4, 0x17);
            At40k.SectorWire h4p2 = at40k.new SectorWire(true, 2, 4, 0x17);

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
            At40k.Cell cell = at40k.cell(0x04, 0x17);
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

            //System.out.println("reading port status: " + Integer.toString(device.readBus() & 0xff, 16));


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
            at40k.cell(4,23).ylut(~0xCC);
            at40k.cell(4,23).xlut(~0xAA);
            at40k.cell(5,23).ylut(~0xAA);
            at40k.cell(5,23).xlut(~0xAA);
            for(int i=6; i<PIPELEN+2; i++) {
                at40k.cell(i, 23).ylut(0xAA);
                at40k.cell(i, 23).xlut(0xCC);
                at40k.cell(i, 23).yi(WEST);
            }
            for(int i=4; i<PIPELEN+2; i++) bounce(at40k.cell(i, 21));
                

            at40k.cell(4,22).ylut(0xB2);
            //at40k.cell(5,22).xlut(0x44);
            at40k.cell(4,22).xi(SE);
            at40k.cell(4,22).yi(NORTH);
            at40k.cell(4,22).c(YLUT);
            at40k.cell(4,22).f(false);
            at40k.cell(4,22).t(false, false, true);
            at40k.cell(4,22).yo(false);
            at40k.cell(4,22).xo(false);

            for(int x=5; x<PIPELEN; x++) {
                at40k.cell(x,22).ylut(0xB2);
                //at40k.cell(x,22).xlut(0x44);
                at40k.cell(x,22).c(YLUT);
                at40k.cell(x,22).f(false);
                at40k.cell(x,22).t(false, false, true);
                at40k.cell(x,22).xi(SE);
                at40k.cell(x,22).yi(WEST);
                at40k.cell(x,22).yo(false);
                at40k.cell(x,22).xo(false);
            }
            //at40k.cell(5,22).yi(WEST);
            at40k.cell(4,22).yi(NORTH);
            at40k.cell(4,22).ylut(0xAA);

            at40k.cell(PIPELEN,22).c(YLUT);
            at40k.cell(PIPELEN,22).ylut(0xB2);
            //at40k.cell(PIPELEN,22).xlut(0x44);
            at40k.cell(PIPELEN,22).xi(NE);
            at40k.cell(PIPELEN,22).yi(WEST);
            at40k.cell(PIPELEN,22).yo(false);
            at40k.cell(PIPELEN,22).xo(false);
            at40k.cell(PIPELEN,22).f(false);
            at40k.cell(PIPELEN,22).t(false, false, true);

            at40k.cell(21,15).yi(WEST);
            at40k.cell(21,15).ylut(0xAA);
            at40k.cell(22,15).yi(WEST);
            at40k.cell(22,15).ylut(0xAA);
            at40k.cell(23,15).h(3, true);
            at40k.cell(23,15).yi(L3);
            at40k.cell(23,15).ylut(0xAA);
            at40k.iob_right(15, true).enableOutput(WEST);
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
            At40k.Cell cell = at40k.cell(4, 16);
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

            device.scanFPGA(true);
            Visualizer v = new Visualizer(at40k, device);
            v.show();
            v.setSize(1380, 1080);
            At40k.Cell cell = at40k.cell(4, 23);
            Image img = v.createImage(v.getWidth(), v.getHeight());
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

            for(int i=0; i<10000; i++) {
                v.refresh();
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
                device.flush();
                Thread.sleep(3000);
                System.out.println("tick");
                //at40k.cell(0x01, 0x17).xlut((byte)0x00);
                at40k.cell(0x00, 0x17).ylut((byte)0xFF);
                device.flush();
            }
            */


            /*
            at40k.iob_top(0, true).output(0);
            at40k.iob_top(0, true).oe(false);
            at40k.iob_top(0, true).pullup();
            device.flush();
            Thread.sleep(3000);

            Log.info(AtmelSerial.class, "issuing command");
            at40k.iob_top(1, true).pulldown();
            device.flush();
            */
            Log.info(AtmelSerial.class, "done");
            System.exit(0);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void scan(At40k dev, At40k.Cell cell, int source, boolean setup) {
        if (setup) {
            if (source != NONE) cell.c(source);
            cell.b(false);
            cell.f(false);
            cell.out(L3, true);
        }
        cell.v(L3, setup);

        At40k.SectorWire sw = cell.vwire(L3);
        //System.out.println("wire is: " + sw);
        while(sw.row > (12 & ~0x3) && sw.south() != null) {
            //System.out.println(sw + " -> " + sw.south());
            sw.drives(sw.south(), setup);
            sw = sw.south();
        }
        while(sw.row < (12 & ~0x3) && sw.north() != null) {
            //System.out.println(sw + " -> " + sw.north());
            sw.drives(sw.north(), setup);
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
        cell.h(L3, setup);
        cell.v(L3, setup);
        sw = cell.hwire(L3);
        while(sw.east() != null) {
            //System.out.println(sw + " -> " + sw.east());
            sw.drives(sw.east(), setup);
            sw = sw.east();
        }
    }

    public static void copy(At40k.Cell cell, int xdir, int ydir) {
        cell.xlut((byte)0x33);
        cell.ylut((byte)0x55);
        cell.xi(xdir);
        cell.yi(ydir);
        cell.xo(false);
        cell.yo(false);
    }
    public static String hex(int x) {
        return Long.toString(x & 0xffffffffL, 16);
    }

    public static void bounce(At40k.Cell cell) {
        cell.xlut((byte)0xCC);
        cell.ylut((byte)0xCC);
        cell.xi(NE);
        cell.yi(NORTH);
        cell.xo(false);
        cell.yo(false);
    }
    public static void handshaker(At40k.Cell cell) {
        cell.xlut(0x22);
        cell.ylut(0x71);
        cell.c(XLUT);
        cell.f(false);
        cell.t(false, false, true);
    }

    public static class Visualizer extends Frame implements KeyListener, MouseMotionListener, MouseListener {
        public static final int WIDTH = 40;
        public static final int HEIGHT = 40;
        public static final int LW = 15;
        public static final int LH = 15;
        public static final Color RED  = new Color(0xaa, 0x55, 0x55);
        public static final Color BLUE = new Color(0x55, 0x55, 0xaa);
        private final At40k dev;
        private final AvrDrone drone;
        int selx = -1;
        int sely = -1;
        public Visualizer(final At40k dev, final AvrDrone drone) {
            this.dev = dev;
            this.drone = drone;
            show();
            addMouseMotionListener(this);
            addMouseListener(this);
            addKeyListener(this);
            new Thread() {
                public void run() {
                    try {
                        while(true) {
                            Thread.sleep(500);
                            if (!enabled) continue;
                            keyPressed(null);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
        public boolean enabled = false;
        public void mouseClicked(MouseEvent e) { }
        public void mouseEntered(MouseEvent e) { }
        public void mouseExited(MouseEvent e) { }
        public void mouseReleased(MouseEvent e) {
        }
        public void keyTyped(KeyEvent k) {
        }
        public void keyReleased(KeyEvent k) {
        }
        public void keyPressed(KeyEvent k) {
            switch(k==null ? '_' : k.getKeyChar()) {
                case '1': {
                    if (selx==-1 || sely==-1) break;
                    At40k.Cell cell = dev.cell(selx, sely);
                    cell.xlut(0xff);
                    cell.ylut(0xff);
                    drawCell(getGraphics(), selx, sely);
                    break;
                }
                case ' ': {
                    enabled = !enabled;
                    break;
                }
                case 'C': {
                    if (selx==-1 || sely==-1) break;
                    At40k.Cell cell = dev.cell(selx, sely);
                    cell.ylut(0xB2);
                    drawCell(getGraphics(), selx, sely);
                    break;
                }
                case '0': {
                    if (selx==-1 || sely==-1) break;
                    At40k.Cell cell = dev.cell(selx, sely);
                    cell.xlut(0x00);
                    cell.ylut(0x00);
                    drawCell(getGraphics(), selx, sely);
                    break;
                }
            } 
            for(int xx=5; xx<=PIPELEN; xx++) {
                final int x = xx;
                final At40k.Cell cell = dev.cell(x, 22);
                AvrDrone.ByteCallback bc = new AvrDrone.ByteCallback() {
                        public void call(byte b) throws Exception {
                            boolean y = (b & 0x80) != 0;
                            
                            Graphics g = getGraphics();
                            g.setFont(new Font("sansserif", Font.BOLD, 24));
                            g.setColor(Color.white);
                            g.drawString("0", left(cell) + 12, top(cell) + 30);
                            g.drawString("1", left(cell) + 12, top(cell) + 30);
                            //g.setColor(RED);
                            //g.drawString("X="+(x?"1":"0"), left(cell) + 10, top(cell) + 20);
                            
                            //g.drawString((y?"1":"0"), left(cell) + 12, top(cell) + 30);
                            drawCell(g, x, 22, y?new Color(0xff, 0x99, 0x99):new Color(0x99, 0xff, 0x99));
                        }
                    };

                scan(dev, cell, YLUT, true);
                drone.readBus(bc);
                scan(dev, cell, YLUT, false);
            }
        }
        public void mousePressed(MouseEvent e) {
            /*
            At40k.Cell cell = dev.cell(selx, sely);
            if (cell==null) return;
            int old = cell.c();
            scan(dev, cell, YLUT, true);
            boolean y = (drone.readBus() & 0x80) != 0;
            //scan(dev, cell, XLUT, true);
            //boolean x = (drone.readBus() & 0x80) != 0;
            scan(dev, cell, YLUT, false);
            cell.c(old);
            Graphics g = getGraphics();
            g.setFont(new Font("sansserif", Font.BOLD, 14));
            g.setColor(Color.white);
            //g.drawString("X=0", left(cell) + 10, top(cell) + 20);
            //g.drawString("X=1", left(cell) + 10, top(cell) + 20);
            
            //g.setColor(Color.white);
            //g.drawString("Y=0", left(cell) + 8, top(cell) + 35);
            //g.drawString("Y=1", left(cell) + 8, top(cell) + 35);
            
            //g.setColor(RED);
            //g.drawString("X="+(x?"1":"0"), left(cell) + 10, top(cell) + 20);
            g.setColor(BLUE);
            g.drawString("Y="+(y?"1":"0"), left(cell) + 8, top(cell) + 35);
            */
        }

        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            if (selx >= 0 && selx < 24 && sely >= 0 && sely < 24) {
                int cx = selx;
                int cy = sely;
                At40k.Cell cell = dev.cell(cx, cy);
                selx = -1;
                sely = -1;
                drawCell(getGraphics(), cx, cy);
                drawSector(getGraphics(), dev.cell(cx, cy).sector());
            }
            selx = (x-20)/(WIDTH+2);
            sely = (23 - (y-20)/(HEIGHT+2))+1;
            At40k.Cell cell = dev.cell(selx, sely);
            if (selx >= 0 && selx < 24 && sely >= 0 && sely < 24) {
                drawCell(getGraphics(), selx, sely);
                drawSector(getGraphics(), dev.cell(selx, sely).sector());
            }
        }
        public void mouseDragged(MouseEvent e) { mousePressed(e); }
        public void paint(Graphics g) {
            g.setColor(Color.white);
            g.fillRect(0, 0, getWidth(), getHeight());
            for(int x=0; x<24; x++)
                for(int y=0; y<24; y++)
                    drawCell(g,x,y);
            for(int x=0; x<=23; x+=4)
                for(int y=23; y>=0; y-=4) 
                    drawSector(g, dev.cell(x, y).sector());
            g.setColor(BLUE);
            g.drawString("Ready", (5*(WIDTH+2))+20, 40);
            g.setColor(RED);
            g.drawString("Send",  (3*(WIDTH+2))+20, 40);
            g.setColor(BLUE);
            refresh();
        }
        public void refresh() {
            Graphics g = getGraphics();
            /*
            int data = drone.readBus() & 0xff;
            for(int i=0; i<8; i++) {
                g.setColor((data & (1<<i))==0 ? Color.black : Color.green);
                g.drawString("D"+i,  (24*(WIDTH+2))+20, ((23-(i+7))*(HEIGHT+2))+60-HEIGHT/2);
            }
            */
        }
        public static int left(At40k.Cell cell) { return (cell.col)   *(WIDTH+2)+20; }
        public static int top(At40k.Cell cell)  { return (23-cell.row)*(HEIGHT+2)+60; }
        public void drawSector(Graphics g, At40k.Sector sector) {
            g.setColor(Color.gray);
            ((Graphics2D)g).setStroke(new BasicStroke(1));
            int px = ((sector.col)*(WIDTH+2))+20-1;
            int py = ((23-(sector.row+3))*(HEIGHT+2))+60-1;
            g.drawRect(px, py, (WIDTH+2)*4+2, (HEIGHT+2)*4+2);
            for(int dir=0; dir<2; dir++) {
                boolean h = dir==0;
                for(int y=h?sector.row:sector.col; y<(h?sector.row+4:sector.col+4); y++)
                    for(int plane=0; plane<=4; plane++) {
                        At40k.Cell cell      = h ? dev.cell(sector.col,   y) : dev.cell(y, sector.row);
                        At40k.Cell cell_east = h ? dev.cell(sector.col-1, y) : dev.cell(y, sector.row-1);
                        At40k.Cell cell_west = h ? dev.cell(sector.col+4, y) : dev.cell(y, sector.row+4);
                        boolean draw = false;
                        if (h) {
                            if (cell_east!=null &&
                                (cell_east.hwire(plane).drives(cell.hwire(plane)) ||
                                 cell_east.hwire(plane).drives(cell.hwire(plane))))
                                draw = true;
                            if (cell_west!=null &&
                                (cell_west.hwire(plane).drives(cell.hwire(plane)) ||
                                 cell_west.hwire(plane).drives(cell.hwire(plane))))
                                draw = true;
                        } else {
                            if (cell_east!=null &&
                                (cell_east.vwire(plane).drives(cell.vwire(plane)) ||
                                 cell_east.vwire(plane).drives(cell.vwire(plane))))
                                draw = true;
                            if (cell_west!=null &&
                                (cell_west.vwire(plane).drives(cell.vwire(plane)) ||
                                 cell_west.vwire(plane).drives(cell.vwire(plane))))
                                draw = true;
                        }
                        if (!draw)
                            for(int x=h?sector.col:sector.row; x<(h?sector.col+4:sector.row+4); x++)
                                if (((h ? dev.cell(x,y).hx(plane) : dev.cell(y,x).vx(plane))) ||
                                    (h?dev.cell(x,y).out(plane):dev.cell(y,x).out(plane)))
                                    draw = true;
                        if (draw) {
                            g.setColor(new Color(0xff, 0x00, 0xff));
                            if (h) {
                                g.drawLine(left(cell),
                                           top(cell)+3,
                                           left(cell) + 4*(WIDTH+2),
                                           top(cell)+3
                                           );
                            } else {
                                g.drawLine(left(cell)+3,
                                           top(cell) + (HEIGHT+2),
                                           left(cell)+3,
                                           top(cell) - 3*(HEIGHT+2)
                                           );
                            }
                        }
                    }
            }
        }
        public void drawCell(Graphics g, int cx, int cy) { drawCell(g, cx, cy, Color.white); }
        public void drawCell(Graphics g, int cx, int cy, Color bg) {
            int x = (cx*(WIDTH+2))+20;
            int y = ((23-cy)*(HEIGHT+2))+60;
            if (g.getClipBounds() != null && !g.getClipBounds().intersects(new Rectangle(x, y, x+WIDTH, y+HEIGHT))) return;

            System.out.println("drawcell " + cx + "," + cy);
            At40k.Cell cell = dev.cell(cx, cy);
            g.setColor(bg);
            g.fillRect(x, y, WIDTH, HEIGHT);

            g.setColor((selx==cx && sely==cy) ? Color.red : Color.black);
            g.drawRect(x, y, WIDTH, HEIGHT);

            //g.setColor((selx==cx && sely==cy) ? Color.red : Color.gray);
            //g.drawRect(x+(WIDTH-(LW*2))/2-1,    y+(HEIGHT-LW)/2-1, LW*2+1, LH+1);

            g.setColor(RED);
            //g.fillRect(x+(WIDTH-(LW*2))/2,    y+(HEIGHT-LW)/2, LW,   LH);
            g.setColor(Color.white);
            //g.drawString("1", x+(WIDTH-(LW*2))/2,    y+(HEIGHT-LW)/2);

            g.setColor(BLUE);
            //g.fillRect(x+(WIDTH-(LW*2))/2+LW, y+(HEIGHT-LW)/2, LW,   LH);
            g.setColor(Color.white);
            //g.drawString("0", x+(WIDTH-(LW*2))/2+LW,    y+(HEIGHT-LW)/2);

            /*
              g.setColor(BLUE);
            ((Graphics2D)g).setStroke(new BasicStroke((float)1));
            switch(cell.yi()) {
                case NORTH: g.drawLine(x+WIDTH/2+5,  y-10,        x+WIDTH/2+5, y+HEIGHT/2); break;
                case SOUTH: g.drawLine(x+WIDTH/2-5,  y+HEIGHT+10, x+WIDTH/2-5, y+HEIGHT/2); break;
                case EAST:  g.drawLine(x+WIDTH+10, y+HEIGHT/2+5,  x+WIDTH/2, y+HEIGHT/2+5); break;
                case WEST:  g.drawLine(x-10,       y+HEIGHT/2-5,  x+WIDTH/2, y+HEIGHT/2-5); break;
                case NONE:  break;
            }
            g.setColor(RED);
            ((Graphics2D)g).setStroke(new BasicStroke((float)1));
            switch(cell.xi()) {
                case NW: g.drawLine(x-10+3,       y-10,        x+WIDTH/2+3, y+HEIGHT/2); break;
                case SW: g.drawLine(x-10-3,       y+HEIGHT+10, x+WIDTH/2-3, y+HEIGHT/2); break;
                case NE: g.drawLine(x+WIDTH+10+3, y-10,        x+WIDTH/2+3, y+HEIGHT/2); break;
                case SE: g.drawLine(x+WIDTH+10-3, y+HEIGHT+10, x+WIDTH/2-3, y+HEIGHT/2); break;
                case NONE:  break;
            }
            ((Graphics2D)g).setStroke(new BasicStroke(1));
            */

            if (selx==cx && sely==cy) {
                int xp = 23 * (WIDTH+2) + 100;
                int yp = 100;
                g.setColor(Color.white);
                g.fillRect(xp, yp, 300, 1000);
                g.setColor(Color.black);
                g.drawString("Cell " + cx + "," + cy,       xp, (yp+=15));
                //g.drawString("X-Lut: " + bin8(cell.xlut()), xp, (yp+=15));
                g.drawString("X-Lut: " + cell.printXLut(), xp, (yp+=15));
                //g.drawString("Y-Lut: " + bin8(cell.ylut()), xp, (yp+=15));
                g.drawString("Y-Lut: " + cell.printYLutX(), xp, (yp+=15));
            }

            if ((cell.ylut()&0xff)==0xff && (cell.xlut()&0xff)==0xff) {
                g.setFont(new Font("sansserif", Font.BOLD, 24));
                g.setColor(Color.white);
                g.drawString("0", left(cell) + 12, top(cell) + 30);
                g.drawString("1", left(cell) + 12, top(cell) + 30);
                //g.setColor(RED);
                //g.drawString("X="+(x?"1":"0"), left(cell) + 10, top(cell) + 20);
                g.setColor(new Color(0x00, 0x00, 0xff));
                g.drawString("1", left(cell) + 12, top(cell) + 30);
            }
            if ((cell.ylut()&0xff)==0x00 && (cell.xlut()&0xff)==0x00) {
                g.setFont(new Font("sansserif", Font.BOLD, 24));
                g.setColor(Color.white);
                g.drawString("0", left(cell) + 12, top(cell) + 30);
                g.drawString("1", left(cell) + 12, top(cell) + 30);
                //g.setColor(RED);
                //g.drawString("X="+(x?"1":"0"), left(cell) + 10, top(cell) + 20);
                g.setColor(new Color(0x00, 0x00, 0xff));
                g.drawString("0", left(cell) + 12, top(cell) + 30);
            }
            if ((cell.ylut()&0xff)==0xB2) {
                System.out.println("muller @ " + cell);
                g.setFont(new Font("sansserif", Font.BOLD, 24));
                g.setColor(Color.white);
                g.drawString("0", left(cell) + 12, top(cell) + 30);
                g.drawString("1", left(cell) + 12, top(cell) + 30);
                //g.setColor(RED);
                //g.drawString("X="+(x?"1":"0"), left(cell) + 10, top(cell) + 20);
                g.setColor(new Color(0x00, 0xaa, 0x00));
                g.drawString("C", left(cell) + 12, top(cell) + 30);
            }
        }
    }

    private static String pad(int i, String s) { if (s.length()>i) return s; return "0"+pad((i-1),s); }
    private static String bin8(byte b) {
        int n = b & 0xff;
        String ret = "";
        for(int i=7; i>=0; i--)
            ret += (n & (1<<i))==0 ? "0" : "1";
        return ret;
    }

    public static void selfTest(AvrDrone device, At40k at40k, Visualizer v) {
        /*
            int fail = 0;
            long now = System.currentTimeMillis();
            for(int x=0; x<24; x++)
                for(int y=0; y<24; y++) {
                    At40k.Cell cell = at40k.cell(x,y);
                    scan(at40k, cell, YLUT, true);
                    //v.paint(img.getGraphics());
                    //v.getGraphics().drawImage(img, 0, 0, null);
                    cell.ylut(0xff);
                    boolean a = (device.readBus() & 0x80)!=0;
                    cell.ylut(0x00);
                    boolean b = (device.readBus() & 0x80)!=0;
                    if (a & !b) {
                        //System.out.println("pass " + x+","+y);
                        Graphics g = v.getGraphics();
                        g.setColor(Color.green);
                        g.drawString("pass", v.left(cell) + 10, v.top(cell) + 20);
                    } else {
                        System.out.println("FAIL!!!! " + x+","+y+" => " + a + " " + b);
                        fail++;
                        Graphics g = v.getGraphics();
                        g.setColor(Color.red);
                        g.drawString("FAIL", v.left(cell) + 10, v.top(cell) + 20);
                    }
                    scan(at40k, cell, YLUT, false);
                }

            System.out.println("failures: " + fail);
            System.out.println("scan time: " + (System.currentTimeMillis()-now) + "ms");
        */
    }

}
