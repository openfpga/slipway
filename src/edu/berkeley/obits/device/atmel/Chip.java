package edu.berkeley.obits.device.atmel;
import com.ftdi.usb.*;
import java.io.*;

public interface Chip {
    
    public void doReset();


    public void reset(boolean on);
    public void avrrst(boolean on);

    public void config(boolean bit);
    public void config(int data, int numbits);

    public boolean initErr();
    public void    porte(int pin, boolean b);

    public void    con(boolean b);
    public boolean con();

    //remove
    public void buffered();
    public void buffered(boolean buf);
    public void flush();
    public int readPins();
}
