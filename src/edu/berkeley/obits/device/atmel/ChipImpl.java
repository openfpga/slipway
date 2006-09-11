package edu.berkeley.obits.device.atmel;
import com.ftdi.usb.*;
import java.io.*;

public class ChipImpl extends FtdiChip implements Chip {

    public ChipImpl() {
        super();
        doReset();
    }

    public void doReset() {
        dbangmode();
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
        dbangmode();
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

    public void reset(boolean on) {
        bits = on ? (1<<1) : 0;
        cbangmode();
        //dbang(0, on);
    }
    public void avrrst(boolean on) { dbang(7, on); }
    public boolean initErr()       { return (readPins() & (1<<4))!=0; }
    public void clk(boolean on)    { dbang(6, on); }
    public void data(boolean on)   { dbang(5, on); }

    public boolean con() {

        /*
        mask &= ~(1<<0);
        cbangmode();
        boolean ret = (readPins() & (1<<0)) != 0;
        dbangmode();
        return ret;
        */



        dmask &= ~(1<<0);
        dbangmode();
        return (readPins() & (1<<0)) != 0;

    }
    public void con(boolean on) {

        /*
        mask |= (1<<0);
        bits = on ? (1<<0) : 0;
        cbangmode();
        */


        dmask |= (1<<0);
        dbangmode();
        dbang(0, on);

    }

}
