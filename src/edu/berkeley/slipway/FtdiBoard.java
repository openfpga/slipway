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
        chip = new FpslicBoot(new FpslicBootPinsUsb(new FtdiUart(0x6666, 0x3133, 1500 * 1000)));
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

    public void reset() throws IOException {
        chip.reset();
    }

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

    public static String pad(String s, int i) {
        if (s.length() >= i) return s;
        return "0"+pad(s, i-1);
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
                break;
            }
        }

        // FIXME: what if init() is called twice?
        new Thread() {
            public void run() {
                while(true) {
                    try {
                        byte b = in.readByte();
                        ByteCallback bc = (ByteCallback)callbacks.remove(0);
                        bc.call(b);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    public synchronized void scanFPGA(boolean on) throws IOException {
        if (on) {
            out.writeByte(3);
            out.flush();
        } else {
            // FIXME
        }
    }
    // fixme!
    public static int retval = 0;
    public synchronized int readCount() {
        try {
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
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static interface ByteCallback {
        public void call(byte b) throws Exception;
    }

    private Vector callbacks = new Vector();

    public synchronized void readBus(ByteCallback bc) throws IOException {
        callbacks.add(bc);
        out.writeByte(2);
        out.flush();
    }

    public synchronized void readInterrupts(ByteCallback bc) throws IOException {
        callbacks.add(bc);
        out.writeByte(6);
        out.flush();
    }

    private byte[][][] cache = new byte[24][][];
    public /*synchronized*/ byte mode4(int z, int y, int x) {
        if (cache[x]==null) return 0;
        if (cache[x][y]==null) return 0;
        return cache[x][y][z];
    }

    int lastz = 0;
    int lastx = 0;
    int lasty = 0;
    public static int save = 0;
    public static int saveof = 0;
    public /*synchronized*/ void mode4(int z, int y, int x, int d) {
        try {
            out.writeByte(1);
            out.writeByte(z);
            out.writeByte(y);
            out.writeByte(x);
            saveof++;
            lastz = z;
            lastx = x;
            lasty = y;
            out.writeByte(d);

            if (cache[x & 0xff]==null) cache[x & 0xff] = new byte[24][];
            if (cache[x & 0xff][y & 0xff]==null) cache[x & 0xff][y & 0xff] = new byte[256];
            cache[x & 0xff][y & 0xff][z & 0xff] = (byte)(d & 0xff);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public /*synchronized*/ void flush() {
        try {
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String pad(int i, String s) { if (s.length()>i) return s; return "0"+pad((i-1),s); }

}
