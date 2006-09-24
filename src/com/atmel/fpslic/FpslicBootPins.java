package com.atmel.fpslic;
import com.ftdi.usb.*;
import java.io.*;

/**
 * Pin-level access to the bootstrap interface of the FPSLIC
 */
public interface FpslicBootPins {

    public void    avrrstPin(boolean on)      throws IOException;
    public void    cclkPin(boolean on)        throws IOException;
    public void    configDataPin(boolean on)  throws IOException;
    public void    resetPin(boolean on)       throws IOException;
    public boolean initPin()                  throws IOException;
    public void    releaseConPin()            throws IOException;
    public void    conPin(boolean on)         throws IOException;

    public void    flush()                    throws IOException;
    public void    purge()                    throws IOException;
    public void    close()                    throws IOException;

    public InputStream  getUartInputStream()  throws IOException;
    public OutputStream getUartOutputStream() throws IOException;

}

