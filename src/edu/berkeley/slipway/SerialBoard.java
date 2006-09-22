package edu.berkeley.slipway;

import edu.berkeley.obits.*;
import org.ibex.util.Log;
import java.io.*;
import java.util.*;
import gnu.io.*;

public class SerialBoard implements Board {

    private final SerialPort sp;
    private final DataInputStream in;
    private final DataOutputStream out;

    public SerialBoard(SerialPort sp) throws IOException, UnsupportedCommOperationException, InterruptedException {
        this.sp = sp;
        sp.setSerialPortParams(38400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        sp.setFlowControlMode(sp.FLOWCONTROL_RTSCTS_OUT);
        sp.setInputBufferSize(1024);
        this.out = new DataOutputStream(sp.getOutputStream());
        this.in = new DataInputStream(sp.getInputStream());
        Log.debug(this, "consuming any leftover data on the serial port");
        while(in.available() > 0) in.read();
        reset();
    }

    public void reset() {
        try {
            Log.info(this, "resetting device");
            sp.setDTR(true);
            sp.setRTS(true);
            Thread.sleep(500);
            Log.info(this, "deasserting reset signal");
            sp.setDTR(false);
            sp.setRTS(false);
            Thread.sleep(100);
        } catch (InterruptedException e) { throw new RuntimeException(e); }
    }

    public void boot(Reader r) throws Exception {
        throw new Error("not implemented");
    }

    public InputStream getInputStream() { return in; }
    public OutputStream getOutputStream() { return out; }

    /*
    public static SerialPort detectObitsPort() throws Exception {
        Enumeration e = CommPortIdentifier.getPortIdentifiers();
        while(e.hasMoreElements()) {
            CommPortIdentifier cpi = (CommPortIdentifier)e.nextElement();
            Log.info(Demo.class, "trying " + cpi.getName());
            if (cpi.getName().startsWith("/dev/cu.usbserial-"))
                return new RXTXPort(cpi.getName());
            if (cpi.getName().startsWith("/dev/ttyS0"))
                return new RXTXPort(cpi.getName());
        }
        Log.info(Demo.class, "returning null...");
        return null;
    }
    */
}
