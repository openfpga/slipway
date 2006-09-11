package edu.berkeley.obits.device.atmel;
import com.ftdi.usb.*;
import java.io.*;

public class ChipImpl extends FtdiChip implements Chip {

    public ChipImpl() {
        super();
        doReset();
    }

    public void doReset() {
        dbangmode(dmask);
        clk(false);
        data(false);

        con(false);
        flush();
        buffered(false);
        reset(false);
        //avrrst(false);
        try { Thread.sleep(200); } catch (Exception e) { }
        reset(true);
        //avrrst(true);
        try { Thread.sleep(200); } catch (Exception e) { }

        dmask &= ~(1<<7);
        dbangmode(dmask);
    }

    int porte = 0;
    public void porte(int pin, boolean b) {
        porte = (~(1<<pin)) | (b ? (1<<pin) : 0);
        if (pin==4) {
            dbang(2, b);
            flush();
        }
    }


    //

    public void buffered() { buffered = true; }
    public void buffered(boolean buf) { buffered = buf; }
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

    protected int dmask =
        //(1<<0) |
        (1<<1) |
        (1<<2) |
        //(1<<3) |
        //(1<<4) |
        (1<<5) |
        (1<<6) |
        (1<<7);

    public void reset(boolean on) {
        bits = on ? (1<<1) : 0;
        uart();
    }
    public void avrrst(boolean on) { dbang(7, on); }
    public void clk(boolean on)    { dbang(6, on); }
    public void data(boolean on)   { dbang(5, on); }

    public boolean initErr()       { return (readPins() & (1<<4))!=0; }
    public boolean con() {
        dmask &= ~(1<<0);
        dbangmode(dmask);
        return (readPins() & (1<<0)) != 0;
    }
    public void con(boolean on) {
        dmask |= (1<<0);
        dbangmode(dmask);
        dbang(0, on);
    }
}
