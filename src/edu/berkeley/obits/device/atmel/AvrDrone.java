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
    final boolean isFake;

    public AvrDrone() { sp = null; in = null; out = null; isFake = true; } 

    public AvrDrone(InputStream is, OutputStream os) throws IOException {
        this.out = new DataOutputStream(os);
        this.in = new DataInputStream(is);
        this.sp = null;
        isFake = false;
        init();
    } 
    
    public AvrDrone(SerialPort sp) throws IOException, UnsupportedCommOperationException, InterruptedException, DeviceException {
        this.sp = sp;
        //sp.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        sp.setSerialPortParams(38400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        sp.setFlowControlMode(sp.FLOWCONTROL_RTSCTS_OUT);
        sp.setInputBufferSize(1024);
        //sp.setFlowControlMode(sp.FLOWCONTROL_NONE);
        this.out = new DataOutputStream(sp.getOutputStream());
        this.in = new DataInputStream(sp.getInputStream());
        Log.debug(this, "consuming any leftover data on the serial port");
        while(in.available() > 0) in.read();
        reset();
        isFake = false;
        init();
    }

    private void init() throws IOException {
        Log.debug(this, "waiting for device to identify itself");
        if (in.readByte() != (byte)'O')  throw new RuntimeException("didn't get the proper signature");
        if (in.readByte() != (byte)'B')  throw new RuntimeException("didn't get the proper signature");
        if (in.readByte() != (byte)'I')  throw new RuntimeException("didn't get the proper signature");
        if (in.readByte() != (byte)'T')  throw new RuntimeException("didn't get the proper signature");
        if (in.readByte() != (byte)'S')  throw new RuntimeException("didn't get the proper signature");
        if (in.readByte() != (byte)'\n') throw new RuntimeException("didn't get the proper signature");
        Log.info(this, "device correctly identified itself; ready for operation");
    }

    public synchronized void scanFPGA(boolean on) throws DeviceException {
        if (isFake) return;
        try {
            if (on) {
                out.writeByte(3);
                out.flush();
            } else {
                // FIXME
            }
        } catch (IOException e) { throw new DeviceException(e); }
    }
    // fixme!
    public static int retval = 0;
    public synchronized int readCount() throws DeviceException {
        if (isFake) return 0;
        try {
            if (reader != null) {
                reader.start();
                reader = null;
            }
            ByteCallback bc = new ByteCallback() {
                    public synchronized void call(byte b) throws Exception {
                        retval =
                            ((b & 0xff) << 24) |
                            ((in.read() & 0xff) << 16) |
                            ((in.read() & 0xff) << 8) |
                            ((in.read() & 0xff) << 0);
                        this.notify();
                    }
                };
            synchronized(bc) {
                callbacks.add(bc);
                out.writeByte(6);
                out.flush();
                bc.wait();
            }
            return retval;
        } catch (Exception e) { throw new DeviceException(e); }
    }

    public static interface ByteCallback {
        public void call(byte b) throws Exception;
    }

    private Vector callbacks = new Vector();

    private Thread reader = new Thread() {
            public void run() {
                System.out.println("*** reader thread begun");
                while(true) {
                    try {
                        byte b = isFake ? 0 : in.readByte();
                        ByteCallback bc = (ByteCallback)callbacks.remove(0);
                        bc.call(b);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };

    public synchronized void readBus(ByteCallback bc) throws DeviceException {
        try {
            callbacks.add(bc);
            if (!isFake) {
                out.writeByte(2);
                out.flush();
            }
            if (reader != null) {
                reader.start();
                reader = null;
            }
        } catch (IOException e) { throw new DeviceException(e); }
    }

    public synchronized void readInterrupts(ByteCallback bc) throws DeviceException {
        try {
            callbacks.add(bc);
            if (!isFake) {
                out.writeByte(6);
                out.flush();
            }
            if (reader != null) {
                reader.start();
                reader = null;
            }
        } catch (IOException e) { throw new DeviceException(e); }
    }

    public synchronized void reset() throws DeviceException {
        if (sp==null) return;
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
    public synchronized byte mode4(int z, int y, int x) throws DeviceException {
        if (cache[x]==null) return 0;
        if (cache[x][y]==null) return 0;
        return cache[x][y][z];
    }

    int lastz = 0;
    int lastx = 0;
    int lasty = 0;
    public static int save = 0;
    public static int saveof = 0;
    public synchronized void mode4(int z, int y, int x, int d) throws DeviceException {
        try {
            /*
            Log.info(this, "writing configuration frame [zyxd]: " +
                      pad(1, Integer.toString(z&0xff, 16)) + " " +
                      pad(1, Integer.toString(y&0xff, 16)) + " " +
                      pad(1, Integer.toString(x&0xff, 16)) + " " +
                      pad(1, Integer.toString(d&0xff, 16))
                      );
            */
            boolean zchange = z!=lastz;
            boolean ychange = y!=lasty;
            boolean xchange = x!=lastx;
            boolean zinc    = z==lastz+1;
            boolean yinc    = y==lasty+1;
            boolean xinc    = x==lastx+1;
            boolean zdec    = z==lastz-1;
            boolean ydec    = y==lasty-1;
            boolean xdec    = x==lastx-1;
            
            //System.out.println(zchange + " " + ychange + " " + xchange);
            if (!isFake) {
                /*
                out.writeByte(0x80
                              | (zinc?0x40:zdec?0x04:zchange?0x44:0x00)
                              | (yinc?0x20:ydec?0x02:ychange?0x22:0x00)
                              | (xinc?0x10:xdec?0x01:xchange?0x11:0x00));
                if (!zinc && !zdec && zchange) out.writeByte(z); else save++;
                if (!yinc && !ydec && ychange) out.writeByte(y); else save++;
                if (!xinc && !xdec && xchange) out.writeByte(x); else save++;
                */
                out.writeByte(1);
                out.writeByte(z);
                out.writeByte(y);
                out.writeByte(x);
                saveof++;
                lastz = z;
                lastx = x;
                lasty = y;
                out.writeByte(d);
            }
            if (cache[x & 0xff]==null) cache[x & 0xff] = new byte[24][];
            if (cache[x & 0xff][y & 0xff]==null) cache[x & 0xff][y & 0xff] = new byte[256];
            cache[x & 0xff][y & 0xff][z & 0xff] = (byte)(d & 0xff);
        } catch (IOException e) { throw new DeviceException(e); }
    }

    public synchronized void flush() throws DeviceException {
        if (isFake) return;
        try {
            out.flush();
        } catch (IOException e) { throw new DeviceException(e); }
    }

    private String pad(int i, String s) { if (s.length()>i) return s; return "0"+pad((i-1),s); }

}
