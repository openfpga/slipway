package edu.berkeley.obits.device.atmel;
import com.ftdi.usb.*;
import java.io.*;

public class FtdiChip {
    protected SWIGTYPE_p_ftdi_context context;
    protected int bits = 0;
    public FtdiChip() {
        context = example.new_ftdi_context();

        int result = example.ftdi_init(context);
        if (result != 0)
            throw new RuntimeException("ftdi_initErr() returned " + result);

        result = example.ftdi_usb_open(context, 0x6666, 0x3133);
        if (result != 0)
            throw new RuntimeException("ftdi_usb_open() returned " + result);

        int result = example.ftdi_set_baudrate(context, 750 * 1000);
        if (result != 0)
            throw new RuntimeException("ftdi_set_baudrate() returned " + result);

        result = example.ftdi_set_line_property(context, 8, 0, 0);
        if (result != 0)
            throw new RuntimeException("ftdi_set_baudrate() returned " + result);
    }

    public synchronized int readPins() {
        byte[] b = new byte[1];
        int result = example.ftdi_read_pins(context, b);
        if (result != 0)
            throw new RuntimeException("ftdi_read_pins() returned " + result);
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

        int result = example.ftdi_setflowctrl(context, (1 << 8));
        if (result != 0)
            throw new RuntimeException("ftdi_setflowcontrol() returned " +
                                       result);
    }
    public synchronized void uart() {

        result = example.ftdi_set_bitmode(context, (short)0, (short)0x00);
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

    protected synchronized void cbangmode() {
        int result = example.ftdi_set_bitmode(context, (short)((mask << 4) | bits), (short)0x20);
        if (result != 0)
            throw new RuntimeException("ftdi_set_bitmode() returned " + result);
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
    int write = 0;
    protected synchronized void dbang(byte by) {
        byte[] b = new byte[1];
        b[0] = by;
        int result = example.ftdi_write_data(context, b, 1);
        if (result != 1)
            throw new RuntimeException("ftdi_write_data() returned " + result);
    }
    int queued = 0;
    protected synchronized void dbang(byte[] b, int len) {
        example.ftdi_write_data(context, b, len);
    }
}
