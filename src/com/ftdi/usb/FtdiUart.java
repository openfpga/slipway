package com.ftdi.usb;
import java.io.*;

public class FtdiUart {

    protected int bits = 0;
    protected SWIGTYPE_p_ftdi_context context = example.new_ftdi_context();

    public OutputStream getOutputStream() { return out; }
    public InputStream  getInputStream() { return in; }

    public FtdiUart(int vendor, int product) {
        example.ftdi_init(context);
        example.ftdi_usb_open(context, vendor, product);
        example.ftdi_usb_reset(context);
        //example.ftdi_set_baudrate(context, 750 * 1000);
        example.ftdi_set_baudrate(context, 1500 * 1000);
        //example.ftdi_set_baudrate(context, 750 * 1000 * 4);
        example.ftdi_set_line_property(context, 8, 0, 0);
        purge();
    }

    public synchronized int readPins() {
        flush();
        byte[] b = new byte[1];
        example.ftdi_read_pins(context, b);
        return b[0];
    }

    public void flush() {
        try {
            getOutputStream().flush();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    protected static int mask =
        (1<<0) |
        (1<<1)// |
        //(1<<2) |
        //(1<<3)
        ;

    public synchronized void purge() {
        example.ftdi_usb_purge_buffers(context);
    }
    public synchronized void uart() {
        example.ftdi_set_bitmode(context, (short)((mask << 4) | bits), (short)0x20);
        //example.ftdi_setflowctrl(context, (1 << 8));
    }
    public synchronized void dbangmode(int dmask) {
        example.ftdi_set_bitmode(context, (short)dmask, (short)0x01);
    }

    protected int dbits = 0;

    protected synchronized void dbang(int bit, boolean val) {
        dbits = val ? (dbits | (1 << bit)) : (dbits & (~(1 << bit)));
        try {
            out.write((byte)dbits);
        } catch (IOException e) { throw new RuntimeException(e); }
    }

 
    private final InputStream in = new InputStream() {
            public int available() throws IOException {
                // FIXME
                return 0;
            }
            public int read() throws IOException {
                byte[] b = new byte[1];
                int result = 0;
                while(result==0) result = read(b, 0, 1);
                return b[0] & 0xff;
            }
            public int read(byte[] b, int off, int len) throws IOException {
                // FIXME: blocking reads?
                int result = 0;
                while(true) {
                    if (len==0) return 0;
                    byte[] b0 = new byte[len];
                    synchronized(FtdiUart.this) {
                        result = example.ftdi_read_data(context, b0, len);
                    }
                    if (result>0) {
                        System.arraycopy(b0, 0, b, off, result);
                        return result;
                    }
                    try { Thread.sleep(50); } catch (Exception e) { e.printStackTrace(); } 
                }
            }
        };

    private final OutputStream out = new BufferedOutputStream(new OutputStream() {
            public void write(int b) throws IOException {
                byte[] d = new byte[1];
                d[0] = (byte)b;
                write(d, 0, 1);
            }
            public void write(byte[] b, int off, int len) throws IOException {
                byte[] b2 = new byte[64];
                while(len > 0) {
                    System.arraycopy(b, off, b2, 0, Math.min(b2.length, len));
                    int result;
                    synchronized(FtdiUart.this) {
                        result = example.ftdi_write_data(context, b2, Math.min(b2.length, len));
                    }
                    off += result;
                    len -= result;
                }
            }
        });
}
