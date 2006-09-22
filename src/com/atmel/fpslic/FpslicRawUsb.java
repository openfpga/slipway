package com.atmel.fpslic;
import com.ftdi.usb.*;
import java.io.*;

/**
 * Exposes the FpslicRaw interface of an FPSLIC wired to an FTDI USB-UART.
 */
public class FpslicRawUsb implements FpslicRaw {

    private FpslicPins pins;

    public FpslicRawUsb(FtdiUart ftdiuart) throws IOException {
        this.pins = new FpslicPinsUsb(ftdiuart);
        reset();
    }

    public void reset() throws IOException {

        pins.avrrstPin(false);
        pins.configDataPin(false);
        pins.resetPin(false);
        pins.cclkPin(false);
        
        pins.conPin(false);
        pins.flush();

        pins.resetPin(false);
        try { Thread.sleep(500); } catch (Exception e) { }
        if (pins.initPin()) throw new RuntimeException("INIT was still high after pulling RESET low");

        pins.resetPin(true);
        try { Thread.sleep(500); } catch (Exception e) { }
        if (!pins.initPin()) throw new RuntimeException("INIT was still low after releasing RESET");

        sendConfigBits(0,2);
        pins.flush();
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
                    pins.flush();
                }
                public void close() throws IOException {

                    pins.flush();

                    // turn off the CON pin we've been pulling low...
                    pins.releaseConPin();

                    if (!pins.initPin())
                        throw new RuntimeException("initialization failed at " + bytes);
                    for(int i=0; i<100; i++) {
                        pins.flush();
                        if (!pins.initPin())
                            throw new RuntimeException("initialization failed at " + bytes);
                        try { Thread.sleep(20); } catch (Exception e) { }
                        sendConfigBits(0,1);
                    }

                    pins.close();
                }
            };
    }

    public OutputStream getOutputStream() throws IOException { return pins.getUartOutputStream(); }
    public InputStream  getInputStream() throws IOException { return pins.getUartInputStream(); }

    public void selfTest() throws Exception {
        boolean pin;
        System.out.print("smoke check: ");

        // correct preamble
        getConfigStream();
        sendConfigBits(Integer.parseInt("00000000", 2), 8);
        sendConfigBits(Integer.parseInt("10110111", 2), 8);
        sendConfigBits(0,1);
        pins.flush();
        try { Thread.sleep(100); } catch (Exception e) { }
        pin = pins.initPin();
        System.out.print((pin ? green(" [pass]") : red(" [FAIL]")));

        // preamble shifted one bit earlier than it should be
        getConfigStream();
        sendConfigBits(Integer.parseInt("0000000",  2), 7);
        sendConfigBits(Integer.parseInt("10110111", 2), 8);
        sendConfigBits(0, 2);
        pins.flush();
        try { Thread.sleep(100); } catch (Exception e) { }
        pin = pins.initPin();
        System.out.print((pin ? red(" [FAIL]") : green(" [pass]")));

        // preamble shifted one bit later than it should be
        getConfigStream();
        sendConfigBits(Integer.parseInt("000000000", 2), 9);
        sendConfigBits(Integer.parseInt("10110111",  2), 8);
        //sendConfigBits(0, 1);
        pins.flush();
        try { Thread.sleep(100); } catch (Exception e) { }
        pin = pins.initPin();
        System.out.print((pin ? red(" [FAIL]") : green(" [pass]")));

        // plain 'ol bogus preamble
        getConfigStream();
        sendConfigBits(Integer.parseInt("00000000", 2), 8);
        sendConfigBits(Integer.parseInt("11110111", 2), 8);
        sendConfigBits(0, 1);
        pins.flush();
        try { Thread.sleep(100); } catch (Exception e) { }
        pin = pins.initPin();
        System.out.print((pin ? red(" [FAIL]") : green(" [pass]")));

        System.out.println();
    }

    // Private //////////////////////////////////////////////////////////////////////////////

    private void sendConfigBits(int dat, int numbits) throws IOException {
        for(int i=(numbits-1); i>=0; i--) {
            boolean bit = (dat & (1<<i)) != 0;
            pins.configDataPin(bit);
            pins.cclkPin(true);
            pins.cclkPin(false);
            //dbits &= ~(1<<6);  // let the clock fall with the next data bit, whenever it goes out
        }
    }

    private static String red(Object o) { return "\033[31m"+o+"\033[0m"; }
    private static String green(Object o) { return "\033[32m"+o+"\033[0m"; }
}
