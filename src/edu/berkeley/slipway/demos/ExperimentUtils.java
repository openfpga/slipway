package edu.berkeley.slipway.demos;

import java.io.*;
import java.util.*;
import java.awt.*;
import com.atmel.fpslic.*;
import edu.berkeley.slipway.*;
import edu.berkeley.slipway.gui.*;
import static com.atmel.fpslic.FpslicConstants.*;

/** useful test structures */
public class ExperimentUtils {

    /** 
     *  Creates a 2x2 cell frequency divider with top left corner at
     *  c, taking input from c.north() and providing output on the
     *  orthogonal axis at c.south().  Returns c.south().south() for
     *  easy daisy-chaining.
     */
    public static FpslicDevice.Cell divider(FpslicDevice.Cell c) {
        FpslicDevice.Cell detect1 = c;
        FpslicDevice.Cell detect2 = c.east();

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

        if (c.south().south()==null) return null;
        if (c.south().south().south()==null) return null;
        return c.south().south();
    }

    /** set up the scan cell */
    public static void setupScanCell(FpslicDevice fpslic) { setupScanCell(fpslic, L3); }
    public static void setupScanCell(FpslicDevice fpslic, int debug_plane) {
        fpslic.cell(23,15).h(3, true);
        fpslic.cell(23,15).yi(debug_plane);
        fpslic.cell(23,15).ylut(0xAA);
        fpslic.iob_right(15, true).enableOutput(WEST);

        fpslic.cell(23,0).ylut(0x00);
        fpslic.iob_right(0, true).enableOutput(WEST);
        fpslic.flush();
    }

    public static void scan(FpslicDevice.Cell cell, int source, boolean setup) { scan(cell, source, setup, L3); }
    public static void scan(FpslicDevice.Cell cell, int source, boolean setup, int debug_plane) {
        FpslicDevice dev = cell.fpslic();
        if (setup) {
            if (source != NONE && cell.c()!=source) cell.c(source);
            if (cell.b()) cell.b(false);
            if (cell.f()) cell.f(false);
        }
        if (cell.out(debug_plane)!=setup) cell.out(debug_plane, setup);
        if (cell.vx(debug_plane)!=setup) cell.v(debug_plane, setup);

        FpslicDevice.SectorWire sw = cell.vwire(debug_plane);

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

        cell = dev.cell(cell.col, 15);

        if (cell.hx(debug_plane) != setup) cell.h(debug_plane, setup);
        if (cell.vx(debug_plane) != setup) cell.v(debug_plane, setup);
        sw = cell.hwire(debug_plane);

        if (sw.west()!=null && sw.west().drives(sw)) { sw.west().drives(sw, false); }
        while(sw.east() != null) {
            //System.out.println(sw + " -> " + sw.east());
            if (sw.drives(sw.east())!=setup) sw.drives(sw.east(), setup);
            sw = sw.east();
        }

    }

    /** watches for a pulse on Xin, copies value of Yin */
    public static void pulse_copy(FpslicDevice.Cell cell, int xi, int yi, boolean invert) {
        loopback(cell, YLUT);
        if (!invert) cell.ylut(0xB8);                        /* yo = x ?  yi : z => 1011 1000 */
        else         cell.ylut(0x74);                        /* yo = x ? !yi : z => 0111 0100 */
        if (!invert) cell.xlut(FpslicUtil.lutSwap(0xB8));   /* yo = x ?  yi : z => 1011 1000 */
        else         cell.xlut(FpslicUtil.lutSwap(0x74));   /* yo = x ? !yi : z => 0111 0100 */
        cell.xi(xi);
        cell.yi(yi);
    }

    /** watches for a rising/falling edge on Yin, emits a pulse on Xout */
    public static void pulse_detect(FpslicDevice.Cell c, int in, boolean falling) {
        c.ylut(0x00);
        c.xlut(0x00);
        switch(in) {
            case NW: case NE: case SW: case SE: {
                c.xi(in);
                loopback(c, XLUT);
                if (!falling) c.ylut(FpslicUtil.lutSwap(0x0C)); /* x & !z */
                else          c.ylut(FpslicUtil.lutSwap(0x30)); /* !x & z */
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

    public static void loopback(FpslicDevice.Cell cell, int cin) {
        cell.f(false);
        cell.b(false);
        cell.t(false, false, true);
        cell.yo(false);
        cell.xo(false);
        cell.c(cin);
    }

    /** copies an x/y-direction input to a y/x-direction output */
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

}