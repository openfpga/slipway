package com.ftdi.usb;
import java.io.*;

/**
 *  A Java wrapper around libftdi.
 *
 *  Note: blocking reads are currently implemented by busy-waiting.
 *  This is really ugly.  Check the linux kernel source to see how to
 *  get libftdi to do it properly.
 *
 *  Flow control is also not properly supported.
 */
public class FtdiUart {

    private SWIGTYPE_p_ftdi_context context = example.new_ftdi_context();

    public OutputStream getOutputStream() { return out; }
    public InputStream  getInputStream() { return in; }

    public FtdiUart(int vendor, int product, int baud) {
        example.ftdi_init(context);
        example.ftdi_usb_open(context, vendor, product);
        example.ftdi_usb_reset(context);
        example.ftdi_set_baudrate(context, baud);
        example.ftdi_set_line_property(context, 8, 0, 0);
        purge();
    }

    /**
     *  Switch to uart mode, with read/write access to four CBUS lines.
     *  This function is used to write to the CBUS lines (re-invoke it to change their state).
     *  I think readPins() is used to read from them, but I'm not sure.
     *
     *  @param cbus_mask a four-bit mask; set bit=1 to write to a CBUS line, bit=0 to read from it
     *  @param cbus_bits a four-bit mask; the bits to assert on the write-enabled CBUS lines
     */
    public synchronized void uart_and_cbus_mode(int cbus_mask, int cbus_bits) {
        example.ftdi_set_bitmode(context, (short)((cbus_mask << 4) | cbus_bits), (short)0x20);
    }

    /**
     *  Switch to dbus mode; CBUS lines will be released (ie they will float).
     *  Use getInputStream()/getOutputStream() to read/write the eight DBUS lines.
     * 
     *  @param dbus_mask an eight-bit mask; set bit=1 to write to a DBUS line, bit=0 to read from it
     */
    public synchronized void dbus_mode(int dbus_mask) {
        example.ftdi_set_bitmode(context, (short)dbus_mask, (short)0x01);
    }

    /** returns the instantaneous value present on the DBUS pins */
    public synchronized int readPins() {
        try {
            getOutputStream().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] b = new byte[1];
        example.ftdi_read_pins(context, b);
        return b[0];
    }

    /** purge the on-chip buffers */
    public synchronized void purge() {
        example.ftdi_usb_purge_buffers(context);
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
