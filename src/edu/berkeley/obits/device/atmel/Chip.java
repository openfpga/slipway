package edu.berkeley.obits.device.atmel;
import com.ftdi.usb.*;
import java.io.*;

public abstract class Chip {
    
    public void doReset() {
        flush();
        buffered(false);


        reset(false);
        //avrrst(false);
        try { Thread.sleep(200); } catch (Exception e) { }
        reset(true);
        //avrrst(true);
        try { Thread.sleep(200); } catch (Exception e) { }
    }

    public abstract void reset(boolean on);
    public abstract void avrrst(boolean on);
    public abstract void int3(boolean on);

    public abstract void config(boolean bit);
    public abstract void config(int data, int numbits);

    public abstract boolean initErr();
    public abstract boolean porte(int pin);
    public abstract void porte(int pin, boolean b);

    public abstract void    con(boolean b);
    public abstract boolean con();

    //remove
    public abstract void buffered();
    public abstract void buffered(boolean buf);
    public abstract void flush();
    public abstract int readPins();
}
