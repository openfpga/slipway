package edu.berkeley.obits.device.atmel;

import com.atmel.fpslic.*;
import edu.berkeley.obits.*;
import java.util.*;
import java.io.*;
import org.ibex.util.Log;

/** a dummy implementation of FpslicDevice */
public class FakeFpslic extends FpslicDevice {

    public FakeFpslic(int width, int height) { super(width, height); }

    public void mode4(int z, int y, int x, int d) { }
    public byte mode4(int z, int y, int x) {
        // FIXME
        return 0;
    }
    public void flush() { }
    public void reset() { }

}
