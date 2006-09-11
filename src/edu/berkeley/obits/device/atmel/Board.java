package edu.berkeley.obits.device.atmel;

import edu.berkeley.obits.*;
import org.ibex.util.Log;
import java.io.*;
import java.util.*;
import gnu.io.*;

public abstract class Board {

    public abstract void reset();
    public abstract void boot(Reader r) throws Exception;
    public abstract InputStream getInputStream();
    public abstract OutputStream getOutputStream();

}
