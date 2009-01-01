package com.atmel.fpslic;

import edu.berkeley.abits.*;
import java.io.*;

/**
 * Implementation of <tt>Board</tt> for Fpslic devices; subclass must
 * implement methods to wiggle pins on the chip.
 */
public abstract class FpslicBoard implements Board {

    public FpslicBoard() throws IOException { }

    public void reset() throws IOException {
        avrrstPin(false);
        configDataPin(false);
        resetPin(false);
        cclkPin(false);
        
        conPin(false);
        flush();

        resetPin(false);
        try { Thread.sleep(500); } catch (Exception e) { }
        if (initPin()) throw new IOException("INIT was still high after pulling RESET low");

        resetPin(true);
        try { Thread.sleep(500); } catch (Exception e) { }
        if (!initPin()) throw new IOException("INIT was still low after releasing RESET");

        sendConfigBits(0,2);
        flush();
    }

    public OutputStream getConfigStream() throws IOException {
        reset();
        return new OutputStream() {
                int bytes = 0;
                int bits = 0;
                public void write(int in) throws IOException {
                    for(int i=7; i>=0; i--) {
                        bits++;
                        sendConfigBits((((in & 0xff) & (1<<i))!=0)?1:0, 1);
                    }
                }
                public void write(byte[] b, int off, int len) throws IOException {
                    for(int i=off; i<off+len; i++)
                        write(b[i]);
                }
                public void flush() throws IOException {
                    FpslicBoard.this.flush();
                }
                public void close() throws IOException {
                    flush();
                    releaseConPin();
                    if (!initPin())
                        throw new IOException("initialization failed at " + bytes);
                    for(int i=0; i<100; i++) {
                        flush();
                        if (!initPin())
                            throw new IOException("initialization failed at " + bytes);
                        try { Thread.sleep(20); } catch (Exception e) { }
                        sendConfigBits(0,1);
                    }
                    FpslicBoard.this.close();
                }
            };
    }

    public void selfTest(SelfTestResultListener resultListener) throws IOException {
        boolean pin;

        // correct preamble
        getConfigStream();
        sendConfigBits(Integer.parseInt("00000000", 2), 8);
        sendConfigBits(Integer.parseInt("10110111", 2), 8);
        sendConfigBits(0,1);
        flush();
        try { Thread.sleep(100); } catch (Exception e) { }
        pin = initPin();
        resultListener.reportTestResult(0, 4, pin);

        // preamble shifted one bit earlier than it should be
        getConfigStream();
        sendConfigBits(Integer.parseInt("0000000",  2), 7);
        sendConfigBits(Integer.parseInt("10110111", 2), 8);
        sendConfigBits(0, 2);
        flush();
        try { Thread.sleep(100); } catch (Exception e) { }
        pin = initPin();
        resultListener.reportTestResult(1, 4, !pin);

        // preamble shifted one bit later than it should be
        getConfigStream();
        sendConfigBits(Integer.parseInt("000000000", 2), 9);
        sendConfigBits(Integer.parseInt("10110111",  2), 8);
        //sendConfigBits(0, 1);
        flush();
        try { Thread.sleep(100); } catch (Exception e) { }
        pin = initPin();
        resultListener.reportTestResult(2, 4, !pin);

        // plain 'ol bogus preamble
        getConfigStream();
        sendConfigBits(Integer.parseInt("00000000", 2), 8);
        sendConfigBits(Integer.parseInt("11110111", 2), 8);
        sendConfigBits(0, 1);
        flush();
        try { Thread.sleep(100); } catch (Exception e) { }
        pin = initPin();
        resultListener.reportTestResult(3, 4, !pin);
    }

    ////////////////////////////////////////////////////////////////////////////////

    private void sendConfigBits(int bits, int numbits) throws IOException {
        for(int i=(numbits-1); i>=0; i--) {
            boolean bit = (bits & (1<<i)) != 0;
            configDataPin(bit);
            cclkPin(true);
            cclkPin(false);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    protected abstract void    avrrstPin(boolean on)      throws IOException;
    protected abstract void    cclkPin(boolean on)        throws IOException;
    protected abstract void    configDataPin(boolean on)  throws IOException;
    protected abstract void    resetPin(boolean on)       throws IOException;
    protected abstract boolean initPin()                  throws IOException;
    protected abstract void    releaseConPin()            throws IOException;
    protected abstract void    conPin(boolean on)         throws IOException;

    protected abstract void    purge()                    throws IOException;
    protected abstract void    flush()                    throws IOException;
    protected abstract void    close()                    throws IOException;

}
