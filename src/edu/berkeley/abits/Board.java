package edu.berkeley.abits;

import edu.berkeley.abits.*;
import java.io.*;

/** interface for controlling an FPGA on a development board */
public interface Board {

    /** "deepest" possible reset of the FPGA */
    public void reset() throws IOException;

    /** return an OutputStream to which a configuration bitstream may be written */
    public OutputStream getConfigStream() throws IOException;

    /** InputStream for communicating with the device after configuration */
    public InputStream  getInputStream() throws IOException;

    /** OutputStream for communicating with the device after configuration */
    public OutputStream getOutputStream() throws IOException;

    /** causes the device to perform a self-test */
    public void selfTest(SelfTestResultListener resultListener) throws Exception;

    /** returns the actual device on the board */
    public Device getDevice();

    public static interface SelfTestResultListener {
        public void reportTestResult(int testNumber, int totalNumberOfTests, boolean didPass);
    }
}
