package edu.berkeley.cs.obits;

import java.util.*;

/** a physical or virtual reconfigurable device */
public interface Device {

    /** reset the device */
    public void reset() throws DeviceException;

    /** flush any commands issued so far, blocking until they have taken effect */
    public void flush() throws DeviceException;

    public static class DeviceException extends Exception {
        public DeviceException(String s) { super(s); }
        public DeviceException(Throwable t) { super(t); }
    }

}
