package edu.berkeley.obits.device.atmel;
import com.ftdi.usb.*;
import java.io.*;

public class ChipImpl extends FtdiChip implements Chip {

    private int dmask =
        //(1<<0) |
        (1<<1) |
        (1<<2) |
        //(1<<3) |
        //(1<<4) |
        (1<<5) |
        (1<<6) |
        (1<<7);

    public ChipImpl() {
        super();
        doReset();
    }

    public void doReset() {

        dmask =
            (1<<0) |
            (1<<1) |
            (1<<2) |
            //(1<<3) |
            //(1<<4) |
            (1<<5) |
            (1<<6) |
            (1<<7);

        flush();
        //purge();

        dbangmode(dmask);
        flush();

        clk(false);
        data(false);
        con(false);
        flush();
        //try { Thread.sleep(500); } catch (Exception e) { }

        reset(false);
        flush();
        try { Thread.sleep(500); } catch (Exception e) { }

        reset(true);
        flush();
        try { Thread.sleep(500); } catch (Exception e) { }
    }

    int porte = 0;
    public void porte(int pin, boolean b) {
        porte = (~(1<<pin)) | (b ? (1<<pin) : 0);
        if (pin==4) {
            dbang(2, b);
            flush();
        }
    }

    public void config(boolean bit) { config(bit?1:0, 1); }
    public void config(int dat) { config(dat, 8); }
    public void config(int dat, int numbits) {
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
    public void reset(boolean on) {
        bits = on ? (1<<1) : 0;
        mask = ((1<<0) | (1<<1));
        uart();
        flush();
        if (on) {
            mask = (1<<0);
            uart();
            flush();
            dbangmode(dmask);
            flush();
        }
    }

    public void avrrst(boolean on) { dbang(7, on); }
    public void clk(boolean on)    { dbang(6, on); }
    public void data(boolean on)   { dbang(5, on); }

    public boolean initErr()       { flush(); return (readPins() & (1<<4))!=0; }
    public boolean con() {
        flush();
        dmask &= ~(1<<0);
        dbangmode(dmask);
        return (readPins() & (1<<0)) != 0;
    }
    public void con(boolean on) {
        flush();
        dmask |= (1<<0);
        dbangmode(dmask);
        dbang(0, on);
    }
}
