package edu.berkeley.slipway.demos;

import java.io.*;
import java.util.*;
import java.awt.*;
import com.atmel.fpslic.*;
import edu.berkeley.slipway.*;
import edu.berkeley.slipway.gui.*;
import edu.berkeley.slipway.util.*;
import static com.atmel.fpslic.FpslicConstants.*;

public class FastestMicropipelineFifoDemo extends MicropipelineFifoDemo {

    public static void main(String[] s) throws Exception {
        new FastestMicropipelineFifoDemo().mainx(s);
    }

    //////////////////////////////////////////////////////////////////////////////

    public FpslicDevice.Cell start;
    public FastestMicropipelineFifoDemo() throws Exception {
        start = fpslic.cell(21, 21);
    }

    protected FpslicDevice.Cell masterCell() { return start.north().north(); }

    private int dividers = 0;
    protected int numDivisors() { return dividers; }

    /** drive this plane high to cause the "master" fifo stage to pause (hold current value) */
    public static final int PLANE_PAUSE_MASTER_WHEN_HIGH  = L0;

    /** drive this plane high to cause the "slave" fifo stages to pause (hold current value) */
    public static final int PLANE_PAUSE_SLAVES_WHEN_HIGH  = L1;

    /** drive this plane low to cause the "slave" fifo stages to pause (hold current value) */
    public static final int PLANE_PAUSE_SLAVES_WHEN_LOW   = L2;

    /** drive this plane low to cause all fifo stages to reset (set current value to 0) */
    public static final int PLANE_RESET_ALL_WHEN_LOW      = L3;

    /** unpauses the master stage */
    public void unPauseMaster() {
        fpslic.cell(0,PLANE_PAUSE_MASTER_WHEN_HIGH).ylut(0x00);
        fpslic.flush();
    }

    /** unpauses the slave stages */
    public void unPauseSlaves() {
        fpslic.cell(0,PLANE_PAUSE_SLAVES_WHEN_HIGH).ylut(0x00);
        fpslic.cell(0,PLANE_PAUSE_SLAVES_WHEN_LOW).ylut(0xff);
        fpslic.flush();
    }

    /** pauses the master stage */
    public void pauseMaster() {
        fpslic.cell(0,PLANE_PAUSE_MASTER_WHEN_HIGH).ylut(0xff);
        fpslic.flush();
    }

    /** pauses the slave stages */
    public void pauseSlaves() {
        fpslic.cell(0,PLANE_PAUSE_SLAVES_WHEN_HIGH).ylut(0xff);
        fpslic.cell(0,PLANE_PAUSE_SLAVES_WHEN_LOW).ylut(0x00);
        fpslic.flush();
    }

    /** reset all stages (should be paused before doing this) */
    public void resetAll() {
        fpslic.cell(0,PLANE_RESET_ALL_WHEN_LOW).ylut(0x00);
        fpslic.flush();
        fpslic.cell(0,PLANE_RESET_ALL_WHEN_LOW).ylut(0xff);
        fpslic.flush();
    }

    /** configures the ylut of the cell at (0,plane) to drive plane "plane" across the entire chip */
    private void drivePlane(int plane) {
        for(int i=0; i<=23; i++){
            FpslicDevice.Cell c = fpslic.cell(0, i);
            c.h(plane, true);
            c.v(plane, true);
            if (c.vwire(plane).south() != null)
                c.vwire(plane).south().drives(c.vwire(plane), true);
            for(FpslicDevice.SectorWire sw = c.hwire(plane).east();
                sw!=null;
                sw=sw.east())
                sw.west().drives(sw, true);
        }
        fpslic.cell(0, plane-L0).c(YLUT);
        fpslic.cell(0, plane-L0).b(false);
        fpslic.cell(0, plane-L0).f(false);
        fpslic.cell(0, plane-L0).out(plane, true);
    }

    /** causes the master cell's successor output to be set to the given value */
    protected void forceMasterSuccessor(boolean high) {
        masterCell().ylut(0xff);
        masterCell().xo(false);
        masterCell().yo(false);
        masterCell().xlut(high ? 0xff : 0x00);
        fpslic.flush();
    }

    /** causes the master cell's successor output to resume normal functionality, leaving it in state "state" */
    protected void unForceMasterSuccessor(boolean state) {
        pauseSlaves();
        masterCell().xo(true);
        masterCell().yo(true);
        masterCell().xlut(LUT_Z);
        fpslic.flush();
        masterCell().ylut(!state ? 0x00 : 0xff);
        fpslic.flush();
        pauseMaster();
        masterCell().ylut((LUT_SELF & ~LUT_OTHER) |
                       (LUT_Z & ~LUT_OTHER) |
                       (LUT_Z & LUT_SELF));
        fpslic.flush();
        unPauseMaster();
        unPauseSlaves();
    }


    protected int init(int size) {
        return init(size, this.start);
    }
    protected int init(int size, FpslicDevice.Cell start) {
        for(int x=1; x<24; x++)
            for(int y=0; y<24; y++) {
                FpslicDevice.Cell c = fpslic.cell(x, y);
                c.xlut(0x00);
                c.ylut(0x00);
                c.b(false);
                c.f(false);
                c.c(YLUT);
            }
        ExperimentUtils.setupScanCell(fpslic);
        fpslic.flush();

        this.start = start;
        drivePlane(L0);
        drivePlane(L1);
        drivePlane(L2);
        drivePlane(L3);

        int rsize = 0;

        // create a column of dividers
        FpslicDevice.Cell div;

        if (size == 4) {
            rsize = 4;
            pipe(start.west().north(), start.west().north().north(), new int[] { NE, SOUTH, NW, SOUTH });
            div = start.east();
            // annoying "bridge cell", because dividers must take input from the north
            div.north().yo(start.north());
            div.north().xo(start.north());
        } else {
            rsize = size-createPipeline(start, true, size-4, false);
            unPauseMaster();
            unPauseSlaves();
            pipe(start.west().north(), start.west(), new int[] { NE, EAST, SW, SOUTH });
            div = start.east();
            // annoying "bridge cell", because dividers must take input from the north
            div.north().yo(start.north());
            div.north().xo(start.north());
        }

        dividers = 0;
        while(div != null) {
            FpslicDevice.Cell xdiv = ExperimentUtils.divider(div);
            dividers++;
            if (xdiv==null) break;
            div = xdiv;
        }
        div = div.south().east();  // lower-right hand corner of the last divider placed
        if (dividers < 10) {
        div.east().yo(div);
        div = div.east();
        while(div.north() != null) {
            div.north().yo(div);
            div = div.north();
        }
        div.xo(div.south());
        div.east().yo(div);
        div.east().xo(div);
        div = div.east();
        div.east().xo(div);
        div.east().yo(div);
        div.south().yo(div);
        div = div.south();
        while(div != null && dividers < 10) {
            FpslicDevice.Cell xdiv = ExperimentUtils.divider(div);
            dividers++;
            if (xdiv==null) { div = div.south().east(); break; }
            if (dividers >= 10)  { div = div.south().east(); break; }
            div = xdiv;
        }
        }
        while(div.south() != null) {
            div.south().yo(div);
            div = div.south();
        }
        while(div.east() != null) {
            div.east().yo(div);
            div = div.east();
        }
        // assumption that we wind up in the lower-right-hand corner

        return rsize;
    }

    //////////////////////////////////////////////////////////////////////////////

    /** create a pipeline starting at cell "c", with predecessor "prev", and move in the directions
     *  specified by "dirs" */
    private FpslicDevice.Cell pipe(FpslicDevice.Cell c, FpslicDevice.Cell prev, int[] dirs) {
        for(int i=0; i<dirs.length; i++) {
            FpslicDevice.Cell next = c.dir(dirs[i]);
            micropipelineStage(c, prev, next);
            prev = c;
            c = next;
        }
        return c;
    }

    /** this is really ugly and I no longer understand it */
    private int createPipeline(FpslicDevice.Cell c, boolean downward, int length, boolean start) {
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
                    FpslicDevice.Cell ret = c = pipe(c, c.south(), new int[] { NE, NORTH, NW, NORTH });
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

    private FpslicDevice.Cell micropipelineStage(FpslicDevice.Cell c,
                                                 FpslicDevice.Cell prev,
                                                 FpslicDevice.Cell next) {
        boolean polarity = false;
        switch(c.dir(next)) {
            case NORTH: case SOUTH: case EAST: case WEST:
                switch (c.dir(prev)) {
                    case NORTH: case SOUTH: case EAST: case WEST: throw new Error("cannot have prev&next both use y");
                }
                polarity = false;
                break;
            case NW: case SE: case SW: case NE:
                switch (c.dir(prev)) {
                    case NW: case SE: case SW: case NE: throw new Error("cannot have prev&next both use x");
                }
                polarity = true;
                break;
            default: throw new Error();
        }

        c.yi(polarity ? prev : next);
        c.xi(polarity ? next : prev);

        c.b(false);
        c.f(false);
        c.yo(true);
        c.xo(true);
        c.c(ZMUX);

        c.wi(PLANE_RESET_ALL_WHEN_LOW);
        c.t(TMUX_W_AND_FB);

        for(int i=L0; i<=L3; i++) c.h(i, true);

        if (!polarity) {
            if (c.row==masterCell().row && c.col==masterCell().col) {
                c.zi(PLANE_PAUSE_MASTER_WHEN_HIGH);
            } else {
                c.zi(PLANE_PAUSE_SLAVES_WHEN_HIGH);
            }
            c.ylut((LUT_SELF & ~LUT_OTHER) |
                   (LUT_Z & ~LUT_OTHER) |
                   (LUT_Z & LUT_SELF));
            c.xlut(LUT_Z);
        } else {
            /*
            // internally asymmetric
            c.zi(PLANE_PAUSE_SLAVES_WHEN_LOW);
            c.xlut((LUT_SELF & ~LUT_OTHER) |
                   (LUT_Z & ~LUT_OTHER) |
                   (LUT_Z & LUT_SELF));
            c.ylut(LUT_Z);
            */

            // internally symmetric
            c.zi(PLANE_PAUSE_SLAVES_WHEN_HIGH);
            c.ylut((~LUT_SELF & LUT_OTHER) |
                   (LUT_Z & ~LUT_SELF) |
                   (LUT_Z & LUT_OTHER));
            c.xlut(LUT_Z);
        }
        return next;
    }



}
