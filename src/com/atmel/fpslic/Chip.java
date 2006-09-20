package com.atmel.fpslic;

import com.ftdi.usb.*;
import java.io.*;

public interface Chip {
    
    public void    doReset();
    public void    reset(boolean on);
    public void    avrrst(boolean on);
    public void    config(boolean bit);
    public void    config(int data, int numbits);
    public boolean initErr();

    public void    con(boolean b);
    public boolean con();
    public boolean rcon();

    //remove
    public void flush();
    public int readPins();
}
