package edu.berkeley.slipway;

import com.ftdi.usb.*;
import com.atmel.fpslic.*;
import edu.berkeley.obits.*;
import org.ibex.util.*;
import java.io.*;
import java.util.*;
import gnu.io.*;

public class FtdiBoard extends Fpslic implements Board {

    static {
        System.load(new File("build/"+System.mapLibraryName("FtdiUartNative")).getAbsolutePath());
    }

    private final FpslicBoot chip;
    private final DataInputStream in;
    private final DataOutputStream out;

    public InputStream getInputStream() { return in; }
    public OutputStream getOutputStream() { return out; }

    public FtdiBoard() throws Exception {
        super(24, 24);
        chip = new FpslicBoot(new FpslicBootPinsUsb(new FtdiUart(0x6666, 0x3133, 1500 * 1000/2)));
        String bstFile = this.getClass().getName();
        bstFile = bstFile.substring(0, bstFile.lastIndexOf('.'));
        bstFile = bstFile.replace('.', '/')+"/slipway_drone.bst";
        boot(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(bstFile)));
        in = new DataInputStream(chip.getInputStream());
        out = new DataOutputStream(chip.getOutputStream());
        for(int i=0; i<255; i++) out.write(0);
        out.flush();
        init();
    }

    public void reset() throws IOException { chip.reset(); }

    public void boot(Reader r) throws Exception {
        chip.selfTest();

        int total = 75090/9;
        OutputStream os = new ProgressOutputStream("bootstrap bitstream:", chip.getConfigStream(), total);
        BufferedReader br = new BufferedReader(r);

        int bytes = 0;
        while(true) {
            String s = br.readLine();
            if (s==null) break;
            bytes++;
            os.write((byte)Integer.parseInt(s, 2));
            if ((bytes % 1000)==0) os.flush();
        }
        os.close();
    }


    // AvrDrone leftovers //////////////////////////////////////////////////////////////////////////////

    private void init() throws IOException {
        byte[] bytes = new byte[6];
        int i=0;

        out.write(0);
        out.flush();

        // read any crap that might be left in the buffer
        while(true) {
            System.arraycopy(bytes, 1, bytes, 0, 5);
            bytes[5] = in.readByte();
            i++;
            System.out.print("\rsignature: read \"" + new String(bytes) + "\"                   ");
            if (bytes[0] == (byte)'O' &&
                bytes[1] == (byte)'B' &&
                bytes[2] == (byte)'I' &&
                bytes[3] == (byte)'T' &&
                bytes[4] == (byte)'S') {
                System.out.println("\rsignature: got proper signature                  ");
                chip.purge();
                break;
            }
        }

        // FIXME: what if init() is called twice?
        new Thread() {
            public void run() {
                while(true) {
                    try {
                        while(callbacks.size() == 0) Thread.sleep(50);
                        byte b = in.readByte();
                        ByteCallback bc = (ByteCallback)callbacks.remove(0);
                        //System.out.println("readback " + b + " in " + (System.currentTimeMillis()-bc.time));
                        bc.call(b);
                        synchronized(lock) {
                            lock.notifyAll();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }


    // Programming ///////////////////////////////////////////////////////////////////////////////

    private byte[][][] cache = new byte[24][][];
    public byte mode4(int z, int y, int x) {
        if (cache[x]==null) return 0;
        if (cache[x][y]==null) return 0;
        return cache[x][y][z];
    }

    public synchronized void mode4(int z, int y, int x, int d) {
        try {
            if (cache[x & 0xff]==null) cache[x & 0xff] = new byte[24][];
            if (cache[x & 0xff][y & 0xff]==null) cache[x & 0xff][y & 0xff] = new byte[256];
            cache[x & 0xff][y & 0xff][z & 0xff] = (byte)(d & 0xff);

            out.writeByte(1);
            out.writeByte(z);
            out.writeByte(y);
            out.writeByte(x);
            out.writeByte(d);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void flush() {
        try { out.flush(); } catch (IOException e) { throw new RuntimeException(e); } }


    // Callbacks //////////////////////////////////////////////////////////////////////////////

    private Vector callbacks = new Vector();

    private Object lock = new Object();
    private static final int limit = 40;

    private void enqueue(ByteCallback bcb) {
        synchronized(lock) {
            try {
                while (callbacks.size() >= limit) {
                    lock.wait(100);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        bcb.time = System.currentTimeMillis();
        callbacks.add(bcb);
    }

    public static abstract class ByteCallback {
        public int result;
        public long time;
        public abstract void call(byte b) throws Exception;
    }

    public synchronized int readCount() {
        try {
            ByteCallback bc = new ByteCallback() {
                    public synchronized void call(byte b) throws Exception {
                        result =
                            ((b & 0xff) << 24) |
                            ((in.read() & 0xff) << 16) |
                            ((in.read() & 0xff) << 8) |
                            ((in.read() & 0xff) << 0);
                        timer =
                            ((in.read() & 0xff) << 24) |
                            ((in.read() & 0xff) << 16) |
                            ((in.read() & 0xff) << 8) |
                            ((in.read() & 0xff) << 0);
                        //System.out.println("timer => " + Integer.toString(timer, 16));
                        this.notify();
                    }
                };
            synchronized(bc) {
                enqueue(bc);
                out.writeByte(3);
                out.flush();
                bc.wait();
            }
            return bc.result;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public int timer = 0;
    public synchronized void readBus(ByteCallback bc) throws IOException {
        enqueue(bc);
        out.writeByte(2);
        out.flush();
    }

    /*
    public synchronized void readInterrupts(ByteCallback bc) throws IOException {
        enqueue(bc);
        out.writeByte(3);
        out.flush();
    }
    */

    // Util //////////////////////////////////////////////////////////////////////////////

    private String pad(int i, String s) { if (s.length()>i) return s; return "0"+pad((i-1),s); }
    public static String pad(String s, int i) {
        if (s.length() >= i) return s;
        return "0"+pad(s, i-1);
    }

}
