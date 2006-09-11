package edu.berkeley.obits.device.atmel;
import com.ftdi.usb.*;
import java.io.*;

public class ChipImpl extends Chip {

    public void doReset() {
        dbangmode();
        clk(false);
        data(false);
        con(false);
        super.doReset();
        dmask &= ~(1<<7);
        dbangmode();
    }
    public void int3(boolean on) { throw new RuntimeException("not connected"); }
    public boolean porte(int pin) { throw new RuntimeException("not connected"); }

    int porte = 0;
    public void porte(int pin, boolean b) {
        porte = (~(1<<pin)) | (b ? (1<<pin) : 0);
        if (pin==4) {
            dbang(2, b);
            flush();
        }
    }

    public void config(boolean bit) { config(bit?1:0, 1); }

    private SWIGTYPE_p_ftdi_context context;
    private int bits = 0;

    public ChipImpl() {
        context = example.new_ftdi_context();

        int result = example.ftdi_init(context);
        if (result != 0)
            throw new RuntimeException("ftdi_initErr() returned " + result);

        result = example.ftdi_usb_open(context, 0x6666, 0x3133);
        if (result != 0)
            throw new RuntimeException("ftdi_usb_open() returned " + result);

        result = example.ftdi_set_baudrate(context, 750 * 1000);
        //result = example.ftdi_set_baudrate(context, 1000 * 1000);
        if (result != 0)
            throw new RuntimeException("ftdi_set_baudrate() returned " + result);
        result = example.ftdi_set_line_property(context, 8, 0, 0);
        if (result != 0)
            throw new RuntimeException("ftdi_set_baudrate() returned " + result);

        doReset();
    }

    public synchronized int readPins() {
        byte[] b = new byte[1];
        int result = example.ftdi_read_pins(context, b);
        if (result != 0)
            throw new RuntimeException("ftdi_read_pins() returned " + result);
        return b[0];
    }

    private OutputStream os = new ChipOutputStream();
    private InputStream  is = new ChipInputStream();

    public OutputStream getOutputStream() {
        return os;
    }
    public InputStream  getInputStream() {
        //example.ftdi_read_data_set_chunksize(context, 32);
        return is;
    }

    public class ChipInputStream extends InputStream {
        public int available() throws IOException {
            // FIXME
            return 0;
        }
        public long skip(long l) throws IOException {
            throw new RuntimeException("not supported");
        }
        public int read() throws IOException {
            System.out.println("read()");
            byte[] b = new byte[1];
            int result = 0;
            while(result==0)
                result = read(b, 0, 1);
            if (result==-1)
                throw new IOException("ftdi_read_pins() returned " + result);
            return b[0] & 0xff;
        }
        public int read(byte[] b, int off, int len) throws IOException {
            // FIXME: blocking reads?
            int result = 0;
            while(true) {
                if (len==0) return 0;
                    byte[] b0 = new byte[len];
                    synchronized(ChipImpl.this) {
                        result = example.ftdi_read_data(context, b0, len);
                    }
                    if (result == -1)
                        throw new IOException("ftdi_read_pins() returned " + result);
                    if (result>0) {
                        System.arraycopy(b0, 0, b, off, result);
                        return result;
                    }
                try { Thread.sleep(50); } catch (Exception e) { e.printStackTrace(); } 
            }
        }
    }

    public class ChipOutputStream extends OutputStream {
        public void write(int b) throws IOException {
            byte[] d = new byte[1];
            d[0] = (byte)b;
            write(d, 0, 1);
        }
        public void write(byte[] b, int off, int len) throws IOException {
            byte[] b2 = new byte[64];
            while(len > 0) {
                System.arraycopy(b, off, b2, 0, Math.min(b2.length, len));
                synchronized(ChipImpl.this) {
                    int result = example.ftdi_write_data(context, b2, Math.min(b2.length, len));
                    if (result < 0)
                        throw new IOException("ftdi_write_data() returned " + result);
                    off += result;
                    len -= result;
                }
            }
        }
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    protected void flush() {
        byte[] bytes = baos.toByteArray();
        baos = new ByteArrayOutputStream();
        dbang(bytes, bytes.length);
    }
    public boolean buffered = false;
    public void buffered() { buffered = true; }
    public void buffered(boolean buf) { buffered = buf; }
    public void config(int dat) { config(dat, 8); }
    public void config(int dat, int numbits) {
        for(int i=(numbits-1); i>=0; i--) {
            boolean bit = (dat & (1<<i)) != 0;
            data(bit);
            clk(true);
            clk(false);
        }
    }

    public void reset(boolean on) {
        bits = on ? (1<<1) : 0;
        cbangmode();
        //dbang(0, on);
    }
    public void avrrst(boolean on) {
        dbang(7, on);
    }
    public boolean initErr() { return (readPins() & (1<<4))!=0; }
    public void clk(boolean on) { dbang(6, on); }
    public void data(boolean on) { dbang(5, on); }

    public boolean con() {

        /*
        mask &= ~(1<<0);
        cbangmode();
        boolean ret = (readPins() & (1<<0)) != 0;
        dbangmode();
        return ret;
        */



        dmask &= ~(1<<0);
        dbangmode();
        return (readPins() & (1<<0)) != 0;

    }
    public void con(boolean on) {

        /*
        mask |= (1<<0);
        bits = on ? (1<<0) : 0;
        cbangmode();
        */


        dmask |= (1<<0);
        dbangmode();
        dbang(0, on);

    }

    private static int mask =
        (1<<0) |
        (1<<1)// |
        //(1<<2) |
        //(1<<3)
        ;

    private static int dmask =
        //(1<<0) |

        (1<<1) |
        (1<<2) |

        //(1<<3) |
        //(1<<4) |
        (1<<5) |
        (1<<6) |
        (1<<7);

    public synchronized void purge() {
        example.ftdi_usb_purge_buffers(context);

        int result = example.ftdi_setflowctrl(context, (1 << 8));
        if (result != 0)
            throw new RuntimeException("ftdi_setflowcontrol() returned " +
                                       result);
    }
    public synchronized void uart() {
        int result = example.ftdi_set_bitmode(context, (short)0, (short)0x00);
        if (result != 0)
            throw new RuntimeException("ftdi_set_bitmode() returned " + result);

        result = example.ftdi_setflowctrl(context, (1 << 8));
        if (result != 0)
            throw new RuntimeException("ftdi_setflowcontrol() returned " +
                                       result);

    }
    public synchronized void dbangmode() {
        int result = example.ftdi_set_bitmode(context, (short)dmask, (short)0x01);
        if (result != 0)
            throw new RuntimeException("ftdi_set_bitmode() returned " + result);
    }

    private synchronized void cbangmode() {
        int result = example.ftdi_set_bitmode(context, (short)((mask << 4) | bits), (short)0x20);
        if (result != 0)
            throw new RuntimeException("ftdi_set_bitmode() returned " + result);
    }

    private int dbits = 0;
    private synchronized void dbang(int bit, boolean val) {
        dbits = val ? (dbits | (1 << bit)) : (dbits & (~(1 << bit)));
        if (buffered) {
            baos.write((byte)dbits);
        } else {
            dbang((byte)dbits);
        }
    }
    int write = 0;
    private synchronized void dbang(byte by) {
        byte[] b = new byte[1];
        b[0] = by;
        int result = example.ftdi_write_data(context, b, 1);
        if (result != 1)
            throw new RuntimeException("ftdi_write_data() returned " + result);
    }
    int queued = 0;
    private synchronized void dbang(byte[] b, int len) {
        example.ftdi_write_data(context, b, len);
    }
}
