package com.atmel.fpslic;
import com.ftdi.usb.*;
import java.io.*;

// TODO: more state checking (ie must have reset high before uart-mode, etc)

/**
 * Pin-level access to the bootstrap port of an FPSLIC via an FTDI USB-UART
 */
public class FpslicBootPinsUsb implements FpslicBootPins {

    private FtdiUart ftdiuart;

    public FpslicBootPinsUsb(FtdiUart ftdiuart) {
        this.ftdiuart = ftdiuart;
    }

    public void avrrstPin(boolean on)     throws IOException { setDBusLine(7, on); }
    public void cclkPin(boolean on)       throws IOException { setDBusLine(6, on); }
    public void configDataPin(boolean on) throws IOException { setDBusLine(5, on); }
    public boolean initPin()              throws IOException { flush(); return (ftdiuart.readPins() & (1<<4))!=0; }

    // tricky: RESET has a weak pull-up, and is wired to a CBUS line.  So,
    //         we can pull it down (assert reset) from uart-mode, or we can
    //         let it float upward from either mode.
    public void resetPin(boolean on) throws IOException {
        ftdiuart.uart_and_cbus_mode(1<<1, on ? (1<<1) : 0);
        flush();
        if (on) {
            ftdiuart.dbus_mode(dmask);
            flush();
        }
    }

    public void flush() throws IOException { ftdiuart.getOutputStream().flush(); }

    private int dbits = 0;
    public void setDBusLine() throws IOException {
        ftdiuart.getOutputStream().write((byte)dbits);
    }
    public void clearDBusLines() throws IOException {
        dbits = 0;
        setDBusLine();
    }
    public void setDBusLine(int bit, boolean val) throws IOException {
        dbits = val ? (dbits | (1 << bit)) : (dbits & (~(1 << bit)));
        setDBusLine();
    }
    
    public void releaseConPin() throws IOException {
        dmask &= ~(1<<0);
        ftdiuart.dbus_mode(dmask);
        flush();
    }

    public void conPin(boolean on) throws IOException {
        dmask |= (1<<0);
        ftdiuart.dbus_mode(dmask);
        setDBusLine(0, on);
        flush();
    }

    public void close() throws IOException {
        // switching to uart mode will implicitly release AVRRST
        avrrstPin(false);
        ftdiuart.purge();
        ftdiuart.uart_and_cbus_mode(1<<1, 1<<1);
        ftdiuart.purge();
    }
    
    private int dmask =
        (1<<0) |
        (1<<1) |
        (1<<2) |
        //(1<<3) |
        //(1<<4) |
        (1<<5) |
        (1<<6) |
        (1<<7);

    public InputStream  getUartInputStream() throws IOException { return ftdiuart.getInputStream(); }
    public OutputStream getUartOutputStream() throws IOException { return ftdiuart.getOutputStream(); }
}

