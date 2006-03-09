package edu.berkeley.obits.device.atmel;

import edu.berkeley.obits.*;
import org.ibex.util.Log;
import java.io.*;
import java.util.*;
import gnu.io.*;

/** the "host" side of the AVR Drone; see AvrDrone.c for the other side */
public class AvrDrone extends AtmelDevice {

    final DataInputStream in;

    final DataOutputStream out;

    final SerialPort sp;

    public AvrDrone(SerialPort sp) throws IOException, UnsupportedCommOperationException, InterruptedException, DeviceException {
        this.sp = sp;
        sp.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        sp.setFlowControlMode(sp.FLOWCONTROL_RTSCTS_OUT);
        //sp.setFlowControlMode(sp.FLOWCONTROL_NONE);
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
            sp.setRTS(true);
            Thread.sleep(500);
            Log.info(this, "deasserting reset signal");
            sp.setDTR(false);
            sp.setRTS(false);
            Thread.sleep(100);
        } catch (InterruptedException e) { throw new DeviceException(e); }
    }

    private byte[][][] cache = new byte[24][][];
    public byte mode4(int z, int y, int x) throws DeviceException {
        if (cache[x]==null) return 0;
        if (cache[x][y]==null) return 0;
        return cache[x][y][z];
    }
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
            if (cache[x & 0xff]==null) cache[x & 0xff] = new byte[24][];
            if (cache[x & 0xff][y & 0xff]==null) cache[x & 0xff][y & 0xff] = new byte[256];
            cache[x & 0xff][y & 0xff][z & 0xff] = (byte)(d & 0xff);
        } catch (IOException e) { throw new DeviceException(e); }
    }

    public void flush() throws DeviceException {
        try {
            out.flush();
        } catch (IOException e) { throw new DeviceException(e); }
    }

    private String pad(int i, String s) { if (s.length()>i) return s; return "0"+pad((i-1),s); }

}
