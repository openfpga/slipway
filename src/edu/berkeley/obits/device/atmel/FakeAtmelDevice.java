package edu.berkeley.obits.device.atmel;

import edu.berkeley.obits.*;
import java.util.*;
import java.io.*;
import org.ibex.util.Log;

public class FakeAtmelDevice extends AtmelDevice {

    public void mode4(int z, int y, int x, int d) { }
    public byte mode4(int z, int y, int x) {
        // FIXME
        return 0;
    }
    public void flush() { }
    public void reset() { }

}
