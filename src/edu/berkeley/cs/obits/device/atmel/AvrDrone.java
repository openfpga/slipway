package edu.berkeley.cs.obits.device.atmel;

import edu.berkeley.cs.obits.*;
import org.ibex.util.Log;
import java.io.*;
import java.util.*;
import gnu.io.*;

/** the "host" side of the AVR Drone; see AvrDrone.c for the other side */
public class AvrDrone implements AtmelDevice {

    final DataInputStream in;

    final DataOutputStream out;

    final SerialPort sp;

    public AvrDrone(SerialPort sp) throws IOException, UnsupportedCommOperationException, InterruptedException, DeviceException {
        this.sp = sp;
        sp.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        sp.setFlowControlMode(sp.FLOWCONTROL_RTSCTS_OUT);
        this.out = new DataOutputStream(sp.getOutputStream());
        this.in = new DataInputStream(sp.getInputStream());
        Log.debug(this, "consuming any leftover data on the serial port");
        while(in.available() > 0) in.read();
        reset();
        Log.debug(this, "waiting for device to identify itself");
        if (in.readByte() != (byte)'O')  throw new RuntimeException("didn't get the proper signature");
        if (in.readByte() != (byte)'B')  throw new RuntimeException("didn't get the proper signature");
        if (in.readByte() != (byte)'I')  throw new RuntimeException("didn't get the proper signature");
        if (in.readByte() != (byte)'T')  throw new RuntimeException("didn't get the proper signature");
        if (in.readByte() != (byte)'S')  throw new RuntimeException("didn't get the proper signature");
        if (in.readByte() != (byte)'\n') throw new RuntimeException("didn't get the proper signature");
        Log.info(this, "device correctly identified itself; ready for operation");
    }

    public void reset() throws DeviceException {
        try {
            Log.info(this, "resetting device");
            sp.setDTR(true);
            Thread.sleep(500);
            sp.setDTR(false);
            Thread.sleep(3000);
        } catch (InterruptedException e) { throw new DeviceException(e); }
    }

    /** issue a command to the device in Mode4 format; see Gosset's documentation for further details */
    public void mode4(int z, int y, int x, int d) throws DeviceException {
        try {
            Log.debug(this, "writing configuration frame [zyxd]: " +
                      pad(2, Integer.toString(z, 16)) + " " +
                      pad(2, Integer.toString(y, 16)) + " " +
                      pad(2, Integer.toString(x, 16)) + " " +
                      pad(2, Integer.toString(d, 16))
                      );
            out.writeByte(1);
            out.writeByte(z);
            out.writeByte(y);
            out.writeByte(x);
            out.writeByte(d);
        } catch (IOException e) { throw new DeviceException(e); }
    }

    public void flush() throws DeviceException {
        try {
            out.flush();
        } catch (IOException e) { throw new DeviceException(e); }
    }

    private String pad(int i, String s) { if (s.length()>i) return s; return "0"+pad((i-1),s); }

}
