package edu.berkeley.cs.obits.device.atmel;

import edu.berkeley.cs.obits.*;

public interface AtmelDevice extends Device {
    public void mode4(int z, int y, int x, int d) throws DeviceException;
}
