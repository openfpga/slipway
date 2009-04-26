package edu.berkeley.slipway;

import java.io.*;
import java.util.*;
import com.ftdi.usb.*;
import com.atmel.fpslic.*;
import edu.berkeley.abits.*;
import org.ibex.util.*;

// FEATURE: more state checking (ie must have reset high before uart-mode, etc)

/**
 * Slipway board (Fpslic via FTDI USB-UART, running <tt>SlipwaySlave.c</tt>)
 */
public class FakeBoard extends FpslicDevice implements Board {

    private byte[][][] cache;
    public FakeBoard(int width, int height) {
        super(width, height);
        cache = new byte[256][][];
        for(int i=0; i < cache.length; i++) {
            cache[i] = new byte[256][];
            for(int j=0; j < cache.length; j++) {
                cache[i][j] = new byte[256];
            }
        }
    }

    public void flush() { }
    public void mode4(int z, int y, int x, int d) {
        cache[z][y][x] = (byte)d;
    }
    public byte mode4(int z, int y, int x) {
        return cache[z][y][x];
    }

    public void reset() throws IOException { }

    public OutputStream getConfigStream() throws IOException {
        return new OutputStream() {
            public void flush() { }
            public void write(int b) { }
            public void write(byte[] b, int x, int y) { }
        };
    }

    public InputStream  getInputStream() {
        return new InputStream() {
            public int available() { return 0; }
            public int read() { return -1; }
            public int read(byte[] b, int x, int y) { return -1; }
        };
    }

    public OutputStream getOutputStream() {
        return new OutputStream() {
            public void flush() { }
            public void write(int b) { }
            public void write(byte[] b, int x, int y) { }
        };
    }

    //public void selfTest(SelfTestResultListener resultListener) throws Exception { }

    public Device getDevice() { return this; }

    public void boot(Reader r) throws Exception { }
}

