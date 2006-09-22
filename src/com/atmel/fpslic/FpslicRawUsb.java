package com.atmel.fpslic;
import com.ftdi.usb.*;
import java.io.*;

/**
 * Exposes the FpslicRaw interface of an FPSLIC wired to an FTDI USB-UART.
 */
public class FpslicRawUsb implements FpslicRaw {

    private FtdiUart ftdiuart;
    private FpslicPinsUsb pins;

    private int dmask =
        (1<<0) |
        (1<<1) |
        (1<<2) |
        //(1<<3) |
        //(1<<4) |
        (1<<5) |
        (1<<6) |
        (1<<7);

    public FpslicRawUsb(FtdiUart ftdiuart) throws IOException {
        this.ftdiuart = ftdiuart;
        //this.pins = new FpslicPinsUsb(ftdiuart);
        reset();
    }

    public void reset() throws IOException {

        avrrstPin(false);
        configDataPin(false);
        resetPin(false);
        cclkPin(false);
        
        conPin(false);
        flush();

        resetPin(false);
        try { Thread.sleep(500); } catch (Exception e) { }
        if (initPin()) throw new RuntimeException("INIT was still high after pulling RESET low");

        resetPin(true);
        try { Thread.sleep(500); } catch (Exception e) { }
        if (!initPin()) throw new RuntimeException("INIT was still low after releasing RESET");

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
                    FpslicRawUsb.this.flush();
                }
                public void close() throws IOException {

                    flush();

                    // turn off the CON pin we've been pulling low...
                    releaseConPin();

                    if (!initPin())
                        throw new RuntimeException("initialization failed at " + bytes);
                    for(int i=0; i<100; i++) {
                        flush();
                        if (!initPin())
                            throw new RuntimeException("initialization failed at " + bytes);
                        try { Thread.sleep(20); } catch (Exception e) { }
                        sendConfigBits(0,1);
                    }

                    // switching to uart mode will implicitly release AVRRST
                    avrrstPin(false);
                    ftdiuart.purge();
                    ftdiuart.uart_and_cbus_mode(1<<1, 1<<1);
                }
            };
    }

    public OutputStream getOutputStream() { return ftdiuart.getOutputStream(); }
    public InputStream  getInputStream() { return ftdiuart.getInputStream(); }

    public void selfTest() throws Exception {
        boolean pin;
        System.out.print("smoke check: ");

        // correct preamble
        getConfigStream();
        sendConfigBits(Integer.parseInt("00000000", 2), 8);
        sendConfigBits(Integer.parseInt("10110111", 2), 8);
        sendConfigBits(0,1);
        flush();
        try { Thread.sleep(100); } catch (Exception e) { }
        pin = initPin();
        System.out.print((pin ? green(" [pass]") : red(" [FAIL]")));

        // preamble shifted one bit earlier than it should be
        getConfigStream();
        sendConfigBits(Integer.parseInt("0000000",  2), 7);
        sendConfigBits(Integer.parseInt("10110111", 2), 8);
        sendConfigBits(0, 2);
        flush();
        try { Thread.sleep(100); } catch (Exception e) { }
        pin = initPin();
        System.out.print((pin ? red(" [FAIL]") : green(" [pass]")));

        // preamble shifted one bit later than it should be
        getConfigStream();
        sendConfigBits(Integer.parseInt("000000000", 2), 9);
        sendConfigBits(Integer.parseInt("10110111",  2), 8);
        //sendConfigBits(0, 1);
        flush();
        try { Thread.sleep(100); } catch (Exception e) { }
        pin = initPin();
        System.out.print((pin ? red(" [FAIL]") : green(" [pass]")));

        // plain 'ol bogus preamble
        getConfigStream();
        sendConfigBits(Integer.parseInt("00000000", 2), 8);
        sendConfigBits(Integer.parseInt("11110111", 2), 8);
        sendConfigBits(0, 1);
        flush();
        try { Thread.sleep(100); } catch (Exception e) { }
        pin = initPin();
        System.out.print((pin ? red(" [FAIL]") : green(" [pass]")));

        System.out.println();
    }

    // Private //////////////////////////////////////////////////////////////////////////////

    private void flush() throws IOException { ftdiuart.getOutputStream().flush(); }

    private int dbits = 0;
    private void setDBusLine() throws IOException {
        ftdiuart.getOutputStream().write((byte)dbits);
    }
    private void clearDBusLines() throws IOException {
        dbits = 0;
        setDBusLine();
    }
    private void setDBusLine(int bit, boolean val) throws IOException {
        dbits = val ? (dbits | (1 << bit)) : (dbits & (~(1 << bit)));
        setDBusLine();
    }

    private void sendConfigBits(int dat, int numbits) throws IOException {
        for(int i=(numbits-1); i>=0; i--) {
            boolean bit = (dat & (1<<i)) != 0;
            configDataPin(bit);
            cclkPin(true);
            dbits &= ~(1<<6);  // let the clock fall with the next data bit, whenever it goes out
        }
    }

    // tricky: RESET has a weak pull-up, and is wired to a CBUS line.  So,
    //         we can pull it down (assert reset) from uart-mode, or we can
    //         let it float upward from either mode.
    private void resetPin(boolean on) throws IOException {
        ftdiuart.uart_and_cbus_mode(1<<1, on ? (1<<1) : 0);
        flush();
        if (on) {
            ftdiuart.dbus_mode(dmask);
            flush();
        }
    }

    private void releaseConPin() throws IOException {
        dmask &= ~(1<<0);
        ftdiuart.dbus_mode(dmask);
        flush();
    }

    private void conPin(boolean on) throws IOException {
        dmask |= (1<<0);
        ftdiuart.dbus_mode(dmask);
        setDBusLine(0, on);
        flush();
    }

    private void avrrstPin(boolean on) throws IOException { setDBusLine(7, on); }
    private void cclkPin(boolean on)    throws IOException { setDBusLine(6, on); }
    private void configDataPin(boolean on)   throws IOException { setDBusLine(5, on); }
    private boolean initPin()       throws IOException { flush(); return (ftdiuart.readPins() & (1<<4))!=0; }

    private static String red(Object o) { return "\033[31m"+o+"\033[0m"; }
    private static String green(Object o) { return "\033[32m"+o+"\033[0m"; }
}
