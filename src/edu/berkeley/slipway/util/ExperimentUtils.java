package edu.berkeley.slipway.util;

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
    public static void setupScanCell(FpslicDevice fpslic) {
        fpslic.cell(23,15).h(3, true);
        fpslic.cell(23,15).yi(L3);
        fpslic.cell(23,15).ylut(0xAA);
        fpslic.iob_right(15, true).enableOutput(WEST);

        fpslic.cell(23,0).ylut(0x00);
        fpslic.iob_right(0, true).enableOutput(WEST);
        fpslic.flush();
    }

}