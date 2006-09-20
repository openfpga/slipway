package com.atmel.fpslic;
import com.ftdi.usb.*;
import java.io.*;

public class ChipImpl extends FtdiUart implements Chip {

    private int dmask =
        (1<<0) |
        (1<<1) |
        (1<<2) |
        //(1<<3) |
        //(1<<4) |
        (1<<5) |
        (1<<6) |
        (1<<7);

    public ChipImpl() throws IOException {
        super(0x6666, 0x3133, 1500 * 1000);
        doReset();
    }

    public void flush() throws IOException { getOutputStream().flush(); }

    protected int dbits = 0;
    protected synchronized void dbang(int bit, boolean val) throws IOException {
        dbits = val ? (dbits | (1 << bit)) : (dbits & (~(1 << bit)));
        try {
            getOutputStream().write((byte)dbits);
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    public void doReset() throws IOException {

        dmask =
            (1<<0) |
            (1<<1) |
            (1<<2) |
            //(1<<3) |
            //(1<<4) |
            (1<<5) |
            (1<<6) |
            (1<<7);
        avrrst(false);

        flush();
        //purge();

        dbus_mode(dmask);
        flush();

        clk(false);
        data(false);
        con(false);
        flush();
        //try { Thread.sleep(500); } catch (Exception e) { }

        reset(false);
        flush();
        try { Thread.sleep(500); } catch (Exception e) { }
        if (initErr()) throw new RuntimeException("INIT was still high after pulling RESET low");

        reset(true);
        flush();
        try { Thread.sleep(500); } catch (Exception e) { }
        if (!initErr()) throw new RuntimeException("INIT was still low after releasing RESET");

        con(false);
    }

    public void config(boolean bit) throws IOException { config(bit?1:0, 1); }
    public void config(int dat) throws IOException { config(dat, 8); }
    public void config(int dat, int numbits) throws IOException {
        for(int i=(numbits-1); i>=0; i--) {
            boolean bit = (dat & (1<<i)) != 0;
            data(bit);
            clk(true);
            clk(false);
        }
    }

    // tricky: RESET has a weak pull-up, and is wired to a CBUS line.  So,
    //         we can pull it down (assert reset) from uart-mode, or we can
    //         let it float upward from either mode.
    public void reset(boolean on) throws IOException {
        uart_and_cbus_mode(1<<1, on ? (1<<1) : 0);
        flush();
        if (on) {
            dbus_mode(dmask);
            flush();
        }
    }

    public void avrrst(boolean on) throws IOException { dbang(7, on); }
    public void clk(boolean on)    throws IOException { dbang(6, on); }
    public void data(boolean on)   throws IOException { dbang(5, on); }

    public boolean initErr()       throws IOException { flush(); return (readPins() & (1<<4))!=0; }

    public boolean con() throws IOException {
        flush();
        //dmask &= ~(1<<0);
        dbus_mode(dmask);
        return (readPins() & (1<<0)) != 0;
    }
    public boolean rcon() throws IOException {
        flush();
        dmask &= ~(1<<0);
        dbus_mode(dmask);
        return (readPins() & (1<<0)) != 0;
    }
    public void con(boolean on) throws IOException {
        flush();
        dmask |= (1<<0);
        dbang(0, on);
        dbus_mode(dmask);
    }

    public OutputStream getConfigStream() throws IOException {
        doReset();
        config(0,10);
        con();
        return new OutputStream() {
                int bytes = 0;
                public void write(int in) throws IOException {
                    bytes++;
                    for(int i=7; i>=0; i--) {
                        config((((in & 0xff) & (1<<i))!=0)?1:0, 1);
                    }
                }
                public void write(byte[] b, int off, int len) throws IOException {
                    for(int i=off; i<off+len; i++)
                        write(b[i]);
                }
                public void flush() throws IOException {
                    ChipImpl.this.flush();
                    rcon();
                }
                public void close() throws IOException {
                    flush();
                    if (!initErr())
                        throw new RuntimeException("initialization failed at " + bytes);
                    for(int i=0; i<100; i++) {
                        flush();
                        if (!initErr())
                            throw new RuntimeException("initialization failed at " + bytes);
                        try { Thread.sleep(20); } catch (Exception e) { }
                        config(0,1);
                    }
                    avrrst(false);
                    try { Thread.sleep(100); } catch (Exception e) { }
                    purge();
                    uart_and_cbus_mode(1<<1, 1<<1);
                }
            };
    }

    public static String red(Object o) { return "\033[31m"+o+"\033[0m"; }
    public static String green(Object o) { return "\033[32m"+o+"\033[0m"; }
    public void selfTest() throws Exception {
        ChipImpl d = this;
        boolean pin;
        d.doReset();
        d.config(0,3);
        d.con();
        d.config(0,7);
        d.flush();
        //d.flush();
        d.config(Integer.parseInt("10110111", 2), 8);
        d.config(0,1);
        d.flush();
        try { Thread.sleep(100); } catch (Exception e) { }
        pin = d.initErr();
        System.out.println("good preamble   => " + pin + " " + (pin ? green("good") : red("BAD")));

        d.doReset();
        try { Thread.sleep(100); } catch (Exception e) { }
        d.config(0,3);
        d.con();
        d.config(0,6);
        d.flush();
        //d.flush();
        d.config(Integer.parseInt("10110111", 2), 8);
        d.config(0, 2);
        d.flush();
        try { Thread.sleep(100); } catch (Exception e) { }
        pin = d.initErr();
        System.out.println("bad preamble #2 => " + pin + " " + (pin ? red("BAD") : green("good")));

        d.doReset();
        try { Thread.sleep(100); } catch (Exception e) { }
        d.config(0,3);
        d.con();
        d.config(0,7);
        d.flush();
        //d.flush();
        d.config(Integer.parseInt("11110111", 2), 8);
        d.config(0, 1);
        d.flush();
        try { Thread.sleep(100); } catch (Exception e) { }
        pin = d.initErr();
        System.out.println("bad preamble #1 => " + pin + " " + (pin ? red("BAD") : green("good")));
    }
}
