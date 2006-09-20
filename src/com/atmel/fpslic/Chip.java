package com.atmel.fpslic;

import com.ftdi.usb.*;
import java.io.*;

public interface Chip {
    
    public void    doReset() throws IOException;
    public void    reset(boolean on) throws IOException;
    public void    avrrst(boolean on) throws IOException;
    public void    config(boolean bit) throws IOException;
    public void    config(int data, int numbits) throws IOException;
    public boolean initErr() throws IOException;

    public void    con(boolean b) throws IOException;
    public boolean con() throws IOException;
    public boolean rcon() throws IOException;

    //remove
    public void    flush() throws IOException;
    public InputStream getInputStream();
    public OutputStream getOutputStream();
    public void selfTest() throws Exception;
    public OutputStream getConfigStream() throws IOException;

}
