package edu.berkeley.slipway.demos;

import java.io.*;
import java.util.*;
import java.awt.*;
import com.atmel.fpslic.*;
import edu.berkeley.slipway.*;
import edu.berkeley.slipway.gui.*;
import static com.atmel.fpslic.FpslicConstants.*;


/**
 *  This demo runs the asynchronous micropipeline fifo experiment from
 *  the FCCM paper.
 *
 *  Output is placed in misc/data/async/ as a collection of .csv
 *  files.  Each file is named sizeXXX.csv, where XXX is the capacity
 *  of the fifo.  Each line of each file is of the form
 *  occupancy,tokenrate where occupancy is the proportion of the fifo
 *  which is occupied (a number between 0 and 1) and tokenrate is the
 *  number of millions of tokens per second observed at a fixed point
 *  on the ring.  All files should be concatenated in order to
 *  reproduce the graphs in the paper.
 */
public abstract class MicropipelineFifoDemo {

    public SlipwayBoard slipway;
    public FpslicDevice fpslic;
    public FpslicDevice.Cell start;

    // Abstract methods to implement //////////////////////////////////////////////////////////////////////////////

    protected abstract int  numDivisors();
    protected abstract void forceMasterSuccessor(boolean high);
    protected abstract void unForceMasterSuccessor(boolean state);
    protected abstract void unPauseMaster();
    protected abstract void unPauseSlaves();
    protected abstract void pauseMaster();
    protected abstract void pauseSlaves();
    protected abstract void resetAll();
    protected abstract int  init(int size);
    protected abstract int  init(int size, FpslicDevice.Cell start);

    // Constructors //////////////////////////////////////////////////////////////////////////////

    public MicropipelineFifoDemo() throws Exception {
        System.err.println("MicropipelineFifoDemo: initializing board...");
        slipway = new SlipwayBoard();
        fpslic = slipway.getFpslicDevice();
    }

    public void mainx(String[] s) throws Exception {
        System.err.println("MicropipelineFifoDemo: setting up scan cell...");
        
        //if (s.length > 0 && s[0].equals("-g")) {

        /*
        Gui vis = new Gui(fpslic, slipway, fpslic.getWidth(), fpslic.getHeight());
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
        */
            //}
    
        ExperimentUtils.setupScanCell(fpslic);

        for(int i=0; i<255; i++) {
            slipway.readInterruptCount();
            System.err.print("\rMicropipelineFifoDemo: paranoia -- flushing interrupt count: " + i + "/254 ");
        }
        System.err.println();

        for(int i=1; i<402; i+=2) go(i);
        System.err.println("MicropipelineFifoDemo: experiment is finished");
    }


    // Experiment Logic //////////////////////////////////////////////////////////////////////////////

    /** drain the fifo */
    protected void drain() {
        while(true){
            pauseMaster();
            pauseSlaves();
            resetAll();
            unPauseMaster();
            unPauseSlaves();
            slipway.readInterruptCount();
            try { Thread.sleep(100); } catch (Exception e) { }
            int rc = slipway.readInterruptCount();
            if (rc!=0) {
                System.err.println("flush() failed => " + rc);
                try { Thread.sleep(1000); } catch (Exception e) { }
                continue;
            }
            break;
        }
    }

    /** fill the fifo with "count" tokens */
    protected void fill(int count) {
        boolean yes = false;
        for(int i=0; i<count; i++) {
            pauseSlaves();
            forceMasterSuccessor(yes);
            unPauseSlaves();
            yes = !yes;
        }
        if (count>0)
            unForceMasterSuccessor(!yes);
    }

    public void go(int size) throws Exception {
        int rsize = init(size, fpslic.cell(20, 20));

        String sizes = rsize+"";
        while(sizes.length()<3) sizes = "0"+sizes;
        String fname = "misc/data/async/size"+sizes+".csv";
        if (!new File(fname).exists()) {
            System.err.println();
            System.err.println("MicropipelineFifoDemo:   fifo size is "+rsize+"...");
            PrintWriter outfile = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fname)));
            for(int i=rsize; i>=0; i-=2)
                test(i, rsize, outfile);
            outfile.flush();
            outfile.close();
        } else {
            System.out.println("MicropipelineFifoDemo: file " + fname + " already exists; skipping");
        }
    }

    public void test(int count, int size, PrintWriter outfile) throws Exception {
        double[] results = new double[numtrials];

      
        int clockdivisor = 64;
        double occupancy = ((double)count)/((double)size);
        int clockrate = 24; // (in mhz)

        for(int i=0; i<results.length; i++) {
            init(size);
            fpslic.flush();
            drain();
            fpslic.flush();
            fill(count);
            fpslic.flush();

            unPauseMaster();
            unPauseSlaves();
            fpslic.flush();

            slipway.readInterruptCount();
            Thread.sleep(1000);
            int tokens = slipway.readInterruptCount();
            
            double elapsed = (double)(slipway.readInterruptCountTime()/clockrate);
            
            int multiplier = 1;
            for(int j=0; j<numDivisors(); j++) multiplier *= 2;
            multiplier /= clockdivisor;

            double result = (tokens*multiplier)/elapsed; // in millions
            results[i] = result;
        }

        double max = 0;
        double min = Double.MAX_VALUE;
        double total = 0;
        for(int i=0; i<numtrials; i++) {
            max = Math.max(max, results[i]);
            min = Math.min(min, results[i]);
            total += results[i];
        }
        total -= max;
        total -= min;
        total /= (numtrials-2);

        // result is transitions/sec
        outfile.println(size + ", " + occupancy + ", " + total);
        outfile.flush();
        System.out.println("num_tokens/capacity: "+count+"/"+size+
                           "  occupancy="+((int)(occupancy*100.0)) +"%"+
                           "  tokenrate=" + total);
    }
    private static final int numtrials = 5;

    protected FpslicDevice.Cell topLeft() { return start.north().north(); }



}


