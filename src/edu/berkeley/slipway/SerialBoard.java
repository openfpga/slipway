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
}
