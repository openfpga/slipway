package edu.berkeley.slipway.mpar;
import com.atmel.fpslic.*;
import java.awt.*;
import byucc.edif.tools.merge.*;
import byucc.edif.*;
import java.io.*;
import java.util.*;
import edu.berkeley.slipway.*;
import edu.berkeley.abits.*;
import com.atmel.fpslic.*;
import static com.atmel.fpslic.FpslicConstants.*;
import static edu.berkeley.slipway.mpar.PhysicalDevice.*;
import static edu.berkeley.slipway.mpar.NetList.*;

public class Placement {

    public final NetList netlist;
    public final PhysicalDevice pd;

    public HashMap<PhysicalDevice.PhysicalCell, NetList.Node> cellToNode =
        new HashMap<PhysicalDevice.PhysicalCell, NetList.Node>();
    public HashMap<NetList.Node, PhysicalDevice.PhysicalCell> nodeToCell =
        new HashMap<NetList.Node, PhysicalDevice.PhysicalCell>();

    public Placement(NetList netlist, PhysicalDevice pd) {
        this.netlist = netlist;
        this.pd = pd;
    }

    public void unplace(PhysicalDevice.PhysicalCell pc) {
        NetList.Node n = cellToNode.get(pc);
        if (n != null) unplace(n);
    }
    public void unplace(NetList.Node n) {
        PhysicalDevice.PhysicalCell pc = nodeToCell.get(n);
        cellToNode.remove(pc);
        nodeToCell.remove(n);
    }
    public void place(NetList.Node n, PhysicalDevice.PhysicalCell pc) {
        if (n==null) return;
        if (pc==null) return;
        unplace(n);
        unplace(pc);
        cellToNode.put(pc, n);
        nodeToCell.put(n, pc);
    }

    public void setPlacement() {
        for(PhysicalDevice.PhysicalCell pc : cellToNode.keySet()) {
            NetList.Node node = cellToNode.get(pc);
            pc.place(node);
        }
    }

    public boolean isPlaced(NetList.Node n) { return nodeToCell.get(n) != null; }
    public boolean isPlaced(PhysicalDevice.PhysicalCell pc) { return cellToNode.get(pc) != null; }

    public PhysicalDevice.PhysicalCell nodeToCell(NetList.Node n) { return nodeToCell.get(n); }
    public NetList.Node cellToNode(PhysicalDevice.PhysicalCell pc) { return cellToNode.get(pc); }

    public void random(Random rand) {
        for(NetList.Node n : netlist.nodes) {
            while(true) {
                PhysicalDevice.PhysicalCell pc = pd.randomCell(rand);
                if (isPlaced(pc)) continue;
                place(n, pc);
                break;
            }
        }
    }

}