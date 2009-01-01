package edu.berkeley.slipway;

import java.io.*;
import java.util.*;
import com.ftdi.usb.*;
import com.atmel.fpslic.*;
import edu.berkeley.abits.*;
import org.ibex.util.*;

// FEATURE: more state checking (ie must have reset high before uart-mode, etc)

/**
 * Slipway board (Fpslic via FTDI USB-UART, running <tt>SlipwaySlave.c</tt>)
 */
public class SlipwayBoard extends FpslicBoard {

    // Private Variables //////////////////////////////////////////////////////////////////////////////

    private final DataInputStream in;
    private final DataOutputStream out;
    private final FpslicDevice device;
    private final FtdiUart ftdiuart;
    private       boolean initialized = false;


    // Accessors //////////////////////////////////////////////////////////////////////////////////////

    public InputStream getInputStream() { return in; }
    public OutputStream getOutputStream() { return out; }

    /** just a different return type for <tt>getDevice()</tt> */
    public FpslicDevice getFpslicDevice() { return (FpslicDevice)getDevice(); }
    public Device getDevice() { return device; }


    // Methods //////////////////////////////////////////////////////////////////////////////////////

    /** initialize assuming default USB settings and only one board connected to the system, perform self-test */
    public SlipwayBoard() throws Exception { this(true); }

    /** initialize assuming default USB settings and only one board connected to the system */
    public SlipwayBoard(boolean selfTest) throws Exception {
        this(new FtdiUart(0x6666, 0x3133, 1500 * 1000/2), selfTest);
    }

    /** initialize with a custom USB interface */
    public SlipwayBoard(FtdiUart ftdiuart, boolean selfTest) throws Exception {
        this.ftdiuart = ftdiuart;
        device = new SlipwayFpslicDevice(24, 24);
        String bstFile = this.getClass().getName();
        bstFile = bstFile.substring(0, bstFile.lastIndexOf('.'));
        bstFile = bstFile.replace('.', '/')+"/slipway_drone.bst";
        if (selfTest) selfTest();
        boot(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(bstFile)));
        in = new DataInputStream(ftdiuart.getInputStream());
        out = new DataOutputStream(ftdiuart.getOutputStream());
        for(int i=0; i<255; i++) out.write(0);
        out.flush();
        init(selfTest);
    }

    private void selfTest() throws IOException {
        System.err.print("smoke check: ");
        selfTest(new edu.berkeley.abits.Board.SelfTestResultListener() {
                public void reportTestResult(int testNumber, int totalNumberOfTests, boolean didPass) {
                    System.err.print(didPass ? " \033[32m[pass]\033[0m " : " \033[31m[FAIL]\033[0m ");
                }
            });
        System.err.println();
    }
    
    private void boot(Reader r) throws IOException {
        int total = 75090/9;
        OutputStream os = new ProgressOutputStream("bootstrap bitstream:", getConfigStream(), total);
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

    private void init(boolean verbose) throws IOException {
        if (initialized) throw new Error("cannot initialize twice");
        initialized = true;
        byte[] bytes = new byte[6];
        int i=0;

        out.write(0);
        out.flush();

        // read any garbage that might be left in the buffer
        while(true) {
            System.arraycopy(bytes, 1, bytes, 0, 5);
            bytes[5] = in.readByte();
            i++;
            if (verbose) System.err.print("\rsignature: read \"" + new String(bytes) + "\"                   ");
            if (bytes[0] == (byte)'O' &&
                bytes[1] == (byte)'B' &&
                bytes[2] == (byte)'I' &&
                bytes[3] == (byte)'T' &&
                bytes[4] == (byte)'S') {
                if (verbose) System.err.println("\rsignature: got proper signature                  ");
                purge();
                break;
            }
        }

        new Thread() {
            public void run() {
                while(true) {
                    try {
                        while(callbacks.size() == 0) Thread.sleep(50);
                        byte b = in.readByte();
                        ByteCallback bc = (ByteCallback)callbacks.remove(0);
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


    // Device //////////////////////////////////////////////////////////////////////////////

    public class SlipwayFpslicDevice extends FpslicDevice {
        private final byte[][][] cache = new byte[24][][];

        // FEATURE: autodetect width/height by querying device id from the chip
        public SlipwayFpslicDevice(int width, int height) {
            super(width, height);
        }

        public synchronized byte mode4(int z, int y, int x) {
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
            try {
                SlipwayBoard.this.flush();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    // Callbacks //////////////////////////////////////////////////////////////////////////////

    private static final int CALLBACK_LIMIT = 40;
    private Vector callbacks = new Vector();
    private Object lock = new Object();
    private int timer = 0;

    private void enqueue(ByteCallback bcb) {
        synchronized(lock) {
            try {
                while (callbacks.size() >= CALLBACK_LIMIT) {
                    lock.wait(100);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        callbacks.add(bcb);
    }

    public static abstract class ByteCallback {
        public int result;
        public abstract void call(byte b) throws Exception;
    }

    /** synchronously returns the number of interrupts triggered since the last call to <tt>readInterruptCount()</tt> */
    public synchronized int readInterruptCount() {
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

    /** returns the number of milliseconds elapsed between the previous call to readInterruptCount() and the one before it */
    public synchronized int readInterruptCountTime() {
        return timer;
    }

    /** reads the values on the 8-bit bus connecting the fpga to the AVR */
    public synchronized void readFpgaData(ByteCallback bc) throws IOException {
        enqueue(bc);
        out.writeByte(2);
        out.flush();
    }


    // Pin Implementations //////////////////////////////////////////////////////////////////////////////

    private int dbits = 0;
    private int dmask =
        (1<<0) |
        (1<<1) |
        (1<<2) |
        //(1<<3) |
        //(1<<4) |
        (1<<5) |
        (1<<6) |
        (1<<7);

    protected void flush() {
        try {
            if (out!=null) out.flush();
            ftdiuart.getOutputStream().flush(); 
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void avrrstPin(boolean on)     throws IOException { setDBusLine(7, on); }
    protected void cclkPin(boolean on)       throws IOException { setDBusLine(6, on); }
    protected void configDataPin(boolean on) throws IOException { setDBusLine(5, on); }
    protected boolean initPin()              throws IOException { flush(); return (ftdiuart.readPins() & (1<<4))!=0; }

    // tricky: RESET has a weak pull-up, and is wired to a CBUS line.  So,
    //         we can pull it down (assert reset) from uart-mode, or we can
    //         let it float upward from either mode.
    protected void resetPin(boolean on) throws IOException {
        ftdiuart.uart_and_cbus_mode(1<<1, on ? (1<<1) : 0);
        flush();
        if (on) {
            ftdiuart.dbus_mode(dmask);
            flush();
        }
    }

    protected void setDBusLine() throws IOException {
        OutputStream os = ftdiuart.getOutputStream();
        os.write((byte)dbits);
    }
    protected void clearDBusLines() throws IOException {
        dbits = 0;
        setDBusLine();
    }
    protected void setDBusLine(int bit, boolean val) throws IOException {
        dbits = val ? (dbits | (1 << bit)) : (dbits & (~(1 << bit)));
        setDBusLine();
    }
    protected void releaseConPin() throws IOException {
        dmask &= ~(1<<0);
        ftdiuart.dbus_mode(dmask);
        flush();
    }
    protected void conPin(boolean on) throws IOException {
        dmask |= (1<<0);
        ftdiuart.dbus_mode(dmask);
        setDBusLine(0, on);
        flush();
    }
    public void close() throws IOException {
        // switching to uart mode will implicitly release AVRRST
        avrrstPin(false);
        ftdiuart.purge();
        ftdiuart.uart_and_cbus_mode(1<<1, 1<<1);
        ftdiuart.purge();
    }
    protected void purge() throws IOException {
        ftdiuart.purge();
    }
    

    // Util //////////////////////////////////////////////////////////////////////////////

    private static String pad(int i, String s) {
        if (s.length()>i) return s;
        return "0"+pad((i-1),s);
    }
    private static String pad(String s, int i) {
        if (s.length() >= i) return s;
        return "0"+pad(s, i-1);
    }
}

