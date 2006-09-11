package edu.berkeley.obits.device.atmel;
import com.ftdi.usb.*;
import java.io.*;

public class FtdiChip {

    protected int bits = 0;
    protected SWIGTYPE_p_ftdi_context context = example.new_ftdi_context();

    public FtdiChip() {
        example.ftdi_init(context);
        example.ftdi_usb_open(context, 0x6666, 0x3133);
        example.ftdi_set_baudrate(context, 750 * 1000);
        example.ftdi_set_line_property(context, 8, 0, 0);
    }

    public synchronized int readPins() {
        byte[] b = new byte[1];
        example.ftdi_read_pins(context, b);
        return b[0];
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    public void flush() {
        byte[] bytes = baos.toByteArray();
        baos = new ByteArrayOutputStream();
        dbang(bytes, bytes.length);
    }

    public boolean buffered = false;
    protected static int mask =
        (1<<0) |
        (1<<1)// |
        //(1<<2) |
        //(1<<3)
        ;

    protected static int dmask =
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
        example.ftdi_setflowctrl(context, (1 << 8));
    }
    public synchronized void uart() {
        cbangmode();
    }
    public synchronized void dbangmode() {
        example.ftdi_set_bitmode(context, (short)dmask, (short)0x01);
    }

    protected synchronized void cbangmode() {
        example.ftdi_set_bitmode(context, (short)((mask << 4) | bits), (short)0x20);
        example.ftdi_setflowctrl(context, (1 << 8));
    }

    protected int dbits = 0;
    protected synchronized void dbang(int bit, boolean val) {
        dbits = val ? (dbits | (1 << bit)) : (dbits & (~(1 << bit)));
        if (buffered) {
            baos.write((byte)dbits);
        } else {
            dbang((byte)dbits);
        }
    }

    protected synchronized void dbang(byte by) {
        byte[] b = new byte[1];
        b[0] = by;
        example.ftdi_write_data(context, b, 1);
    }
    protected synchronized void dbang(byte[] b, int len) {
        example.ftdi_write_data(context, b, len);
    }

    private OutputStream os = new ChipOutputStream();
    private InputStream  is = new ChipInputStream();
    public OutputStream getOutputStream() { return os; }
    public InputStream  getInputStream() { return is; }
    
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
            return b[0] & 0xff;
        }
        public int read(byte[] b, int off, int len) throws IOException {
            // FIXME: blocking reads?
            int result = 0;
            while(true) {
                if (len==0) return 0;
                    byte[] b0 = new byte[len];
                    synchronized(FtdiChip.this) {
                        result = example.ftdi_read_data(context, b0, len);
                    }
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
                synchronized(FtdiChip.this) {
                    int result = example.ftdi_write_data(context, b2, Math.min(b2.length, len));
                    off += result;
                    len -= result;
                }
            }
        }
    }
}
