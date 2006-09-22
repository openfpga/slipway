package com.atmel.fpslic;

import com.ftdi.usb.*;
import java.io.*;

/**
 * "Raw" access to an <i>unconfigured</i> FPSLIC -- used to load the initial bitstream.
 */
public interface FpslicRaw {

    public InputStream  getInputStream() throws IOException;
    public OutputStream getOutputStream() throws IOException;
    public OutputStream getConfigStream() throws IOException;
    public void         reset() throws IOException;
    public void         selfTest() throws Exception;

}
