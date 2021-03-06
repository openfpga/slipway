package edu.berkeley.slipway.demos;

import edu.berkeley.slipway.*;
import com.atmel.fpslic.*;
import static com.atmel.fpslic.FpslicConstants.*;
import static com.atmel.fpslic.FpslicUtil.*;
import edu.berkeley.slipway.gui.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.color.*;
import org.ibex.util.*;
import java.io.*;
import java.util.*;
import static edu.berkeley.slipway.demos.Demo.*;
import static edu.berkeley.slipway.demos.ExperimentUtils.*;

public class DemoVisualizer extends Frame implements KeyListener, MouseMotionListener, MouseListener {
    public static final int WIDTH = 40;
    public static final int HEIGHT = 40;
    public static final int LW = 15;
    public static final int LH = 15;
    public static final Color RED  = new Color(0xaa, 0x55, 0x55);
    public static final Color BLUE = new Color(0x55, 0x55, 0xaa);
    private final FpslicDevice dev;
    private final SlipwayBoard board;
    int selx = -1;
    int sely = -1;
    public DemoVisualizer(final FpslicDevice dev, final SlipwayBoard board) {
        this.dev = dev;
        this.board = board;
        show();
        addMouseMotionListener(this);
        addMouseListener(this);
        addKeyListener(this);
        new Thread() {
            public void run() {
                try {
                    while(true) {
                        Thread.sleep(500);
                        if (!enabled) continue;
                        /*
                          Fpslic.Cell cell = dev.cell(21, 22);
                          cell.xlut(0xff);
                          cell.ylut(0xff);
                        */
                        keyPressed(null);
                        /*
                          cell.xlut(0x00);
                          cell.ylut(0x00);
                        */
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
    public boolean enabled = false;
    public void mouseClicked(MouseEvent e) { }
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) { }
    public void keyTyped(KeyEvent k) { }
    public void keyReleased(KeyEvent k) { }
    public void keyPressed(KeyEvent keyevent) {
        boolean scan = false;
        switch(keyevent==null ? '_' : keyevent.getKeyChar()) {
            case 'C': {
                if (selx==-1 || sely==-1) break;
                FpslicDevice.Cell cell = dev.cell(selx, sely);
                cell.ylut(0xB2);
                drawCell(getGraphics(), selx, sely);
                break;
            }
            case '0': {
                if (selx==-1 || sely==-1) break;
                FpslicDevice.Cell cell = dev.cell(selx, sely);
                cell.xlut(0x00);
                cell.ylut(0x00);
                drawCell(getGraphics(), selx, sely);
                dev.flush();
                break;
            }
            case '1': {
                if (selx==-1 || sely==-1) break;
                FpslicDevice.Cell cell = dev.cell(selx, sely);
                cell.xlut(0xff);
                cell.ylut(0xff);
                drawCell(getGraphics(), selx, sely);
                dev.flush();
                break;
            }
            case 'i': {
                System.out.println("interrupt_count: " + board.readInterruptCount());
                break;
            }
            case 'x': {
                masterx+=2;
                if (mullers) {
                    if (masterx <= 22) {
                        int mx = masterx;
                        System.out.println("low => " + mx);
                        copy(dev.cell(mx, Demo.yofs-2), NORTH, NORTH);
                        copy(dev.cell(mx, Demo.yofs-3), NORTH, NORTH);
                        //dev.cell(mx, Demo.yofs-3).ylut(~dev.cell(mx, Demo.yofs-3).ylut());
                        //dev.cell(mx, Demo.yofs-3).xlut(~dev.cell(mx, Demo.yofs-3).xlut());
                    } else {
                        int mx = 23-(masterx-23);
                        System.out.println("high => " + mx);
                        copy(dev.cell(mx, Demo.yofs), NW, NW);//NORTH, NORTH);
                        copy(dev.cell(mx, Demo.yofs-1), NORTH, NORTH);
                        //for(int x=mx-1; x>=1; x--)
                        //copy(dev.cell(x, Demo.yofs), EAST, EAST);
                        for(int y=Demo.yofs+1; y<=23; y++)
                            copy(dev.cell(1, y), SOUTH, SOUTH);
                        //dev.cell(mx, Demo.yofs-1).ylut(~dev.cell(mx, Demo.yofs-1).ylut());
                        //dev.cell(mx, Demo.yofs-1).xlut(~dev.cell(mx, Demo.yofs-1).xlut());
                    }
                } else {
                    if (masterx <= 22) {
                        int mx = masterx;
                        System.out.println("low => " + mx);
                        copy(dev.cell(mx, Demo.yofs-2), SOUTH, SOUTH);
                        copy(dev.cell(mx, Demo.yofs-3), NORTH, NORTH);
                        dev.cell(mx, Demo.yofs-3).ylut(~dev.cell(mx, Demo.yofs-3).ylut());
                        dev.cell(mx, Demo.yofs-3).xlut(~dev.cell(mx, Demo.yofs-3).xlut());
                    } else {
                        int mx = 23-(masterx-23);
                        System.out.println("high => " + mx);
                        copy(dev.cell(mx, Demo.yofs), SOUTH, SOUTH);
                        /*
                          copy(dev.cell(mx, Demo.yofs-1), NORTH, NORTH);
                        */
                        copy(dev.cell(mx, Demo.yofs-1), NORTH, SW);
                        boolean left = true;
                        FpslicDevice.Cell lc = null;
                        for(int k=0; k<10; k++) {
                            int y = Demo.yofs-2-(k*2);
                            copy(dev.cell(left?(mx-1):mx, y),        SOUTH, left?NE:NW);
                            copy(lc = dev.cell(left?(mx-1):mx, y-1), NORTH, left?SE:SW); 
                            left = !left;
                        }
                        copy(lc, NORTH, NORTH);

                        //for(int x=mx-1; x>=1; x--)
                        //copy(dev.cell(x, Demo.yofs), EAST, EAST);
                        //for(int y=Demo.yofs+1; y<=23; y++)
                        //copy(dev.cell(1, y), SOUTH, SOUTH);

                        if (mx<21) {
                            dev.cell(mx+2, Demo.yofs).ylut(0x00);
                            dev.cell(mx+2, Demo.yofs).xlut(0x00);
                        }

                        /*
                          dev.cell(mx, Demo.yofs-1).ylut(~LUT_Z);
                          dev.cell(mx, Demo.yofs-1).xlut(LUT_Z);
                          loopback(dev.cell(mx, Demo.yofs-1), YLUT);
                        */
                        dev.cell(mx, Demo.yofs).ylut(~LUT_SELF);
                        dev.cell(mx, Demo.yofs).xlut(~LUT_OTHER);
                    }
                }
                break;
            }
            case ' ': {
                //enabled = !enabled;
                scan = true;
                break;
            }
            case '4': {
                //enabled = !enabled;
                try {
                    for(int cap=0; cap<15; cap++) {
                        drain(dev, board);
                        try { Thread.sleep(100); } catch (Exception e) { }
                        //showit(dev, board, this);
                        fill(dev, board, cap);
                        board.readInterruptCount();
                        long now = System.currentTimeMillis();
                        try { Thread.sleep(4000); } catch (Exception e) { }
                        int count = board.readInterruptCount();
                        long now2 = System.currentTimeMillis();
                        System.out.println(cap + " ,  " + (((float)count * (2*2*2*2*2*2*2*2*2*1000))/(now2-now)));
                    }
                } catch (Exception e) { e.printStackTrace(); }
                break;
            }
        } 
        if (!scan) return;
        showit(dev, board, this);
    }
    public void mousePressed(MouseEvent e) { }
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        if (selx >= 0 && selx < 24 && sely >= 0 && sely < 24) {
            int cx = selx;
            int cy = sely;
            FpslicDevice.Cell cell = dev.cell(cx, cy);
            selx = -1;
            sely = -1;
        }
        selx = (x-20)/(WIDTH+2);
        sely = (23 - (y-20)/(HEIGHT+2))+1;
    }
    public void mouseDragged(MouseEvent e) { mousePressed(e); }
    public void paint(Graphics g) {
        System.out.println("paintall");
        g.setColor(Color.white);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setFont(new Font("sansserif", Font.BOLD, 24));
        for(int x=0; x<24; x++)
            for(int y=0; y<24; y++)
                drawCell(g,x,y);
        for(int x=0; x<=23; x+=4)
            for(int y=23; y>=0; y-=4) 
                drawSector(g, dev.cell(x, y).sector());
        refresh();
    }
    public void refresh() { }
    public static int left(FpslicDevice.Cell cell) { return (cell.col)   *(WIDTH+2)+20; }
    public static int top(FpslicDevice.Cell cell)  { return (23-cell.row)*(HEIGHT+2)+60; }
    public void drawSector(Graphics g, FpslicDevice.Sector sector) {
        g.setColor(Color.gray);
        ((Graphics2D)g).setStroke(new BasicStroke(1));
        int px = ((sector.col)*(WIDTH+2))+20-1;
        int py = ((23-(sector.row+3))*(HEIGHT+2))+60-1;
        g.drawRect(px, py, (WIDTH+2)*4+2, (HEIGHT+2)*4+2);
        /*
          for(int dir=0; dir<2; dir++) {
          boolean h = dir==0;
          for(int y=h?sector.row:sector.col; y<(h?sector.row+4:sector.col+4); y++)
          for(int plane=0; plane<=4; plane++) {
          FpslicDevice.Cell cell      = h ? dev.cell(sector.col,   y) : dev.cell(y, sector.row);
          FpslicDevice.Cell cell_east = h ? dev.cell(sector.col-1, y) : dev.cell(y, sector.row-1);
          FpslicDevice.Cell cell_west = h ? dev.cell(sector.col+4, y) : dev.cell(y, sector.row+4);
          boolean draw = false;
          if (h) {
          if (cell_east!=null &&
          (cell_east.hwire(plane).drives(cell.hwire(plane)) ||
          cell_east.hwire(plane).drives(cell.hwire(plane))))
          draw = true;
          if (cell_west!=null &&
          (cell_west.hwire(plane).drives(cell.hwire(plane)) ||
          cell_west.hwire(plane).drives(cell.hwire(plane))))
          draw = true;
          } else {
          if (cell_east!=null &&
          (cell_east.vwire(plane).drives(cell.vwire(plane)) ||
          cell_east.vwire(plane).drives(cell.vwire(plane))))
          draw = true;
          if (cell_west!=null &&
          (cell_west.vwire(plane).drives(cell.vwire(plane)) ||
          cell_west.vwire(plane).drives(cell.vwire(plane))))
          draw = true;
          }
          if (!draw)
          for(int x=h?sector.col:sector.row; x<(h?sector.col+4:sector.row+4); x++)
          if (((h ? dev.cell(x,y).hx(plane) : dev.cell(y,x).vx(plane))) ||
          (h?dev.cell(x,y).out(plane):dev.cell(y,x).out(plane)))
          draw = true;
          if (draw) {
          g.setColor(new Color(0xff, 0x00, 0xff));
          if (h) {
          g.drawLine(left(cell),
          top(cell)+3,
          left(cell) + 4*(WIDTH+2),
          top(cell)+3
          );
          } else {
          g.drawLine(left(cell)+3,
          top(cell) + (HEIGHT+2),
          left(cell)+3,
          top(cell) - 3*(HEIGHT+2)
          );
          }
          }
          }
          }
        */
    }
    public void drawCell(Graphics g, int cx, int cy) {
        int x = (cx*(WIDTH+2))+20;
        int y = ((23-cy)*(HEIGHT+2))+60;
        if (g.getClipBounds() != null && !g.getClipBounds().intersects(new Rectangle(x, y, x+WIDTH, y+HEIGHT))) return;
        drawCell(g, cx, cy, Color.white);
    }
    public void drawCell(Graphics g, int cx, int cy, Color bg) {
        int x = (cx*(WIDTH+2))+20;
        int y = ((23-cy)*(HEIGHT+2))+60;

        //System.out.println("drawcell " + cx + "," + cy);
        FpslicDevice.Cell cell = dev.cell(cx, cy);
        g.setColor(bg);
        g.fillRect(x, y, WIDTH, HEIGHT);

        g.setColor((selx==cx && sely==cy) ? Color.red : Color.black);
        g.drawRect(x, y, WIDTH, HEIGHT);

        //g.setColor((selx==cx && sely==cy) ? Color.red : Color.gray);
        //g.drawRect(x+(WIDTH-(LW*2))/2-1,    y+(HEIGHT-LW)/2-1, LW*2+1, LH+1);

        //g.setColor(RED);
        //g.fillRect(x+(WIDTH-(LW*2))/2,    y+(HEIGHT-LW)/2, LW,   LH);
        //g.setColor(Color.white);
        //g.drawString("1", x+(WIDTH-(LW*2))/2,    y+(HEIGHT-LW)/2);

        //g.setColor(BLUE);
        //g.fillRect(x+(WIDTH-(LW*2))/2+LW, y+(HEIGHT-LW)/2, LW,   LH);
        //g.setColor(Color.white);
        //g.drawString("0", x+(WIDTH-(LW*2))/2+LW,    y+(HEIGHT-LW)/2);

        /*
          g.setColor(BLUE);
          ((Graphics2D)g).setStroke(new BasicStroke((float)1));
          switch(cell.yi()) {
          case NORTH: g.drawLine(x+WIDTH/2+5,  y-10,        x+WIDTH/2+5, y+HEIGHT/2); break;
          case SOUTH: g.drawLine(x+WIDTH/2-5,  y+HEIGHT+10, x+WIDTH/2-5, y+HEIGHT/2); break;
          case EAST:  g.drawLine(x+WIDTH+10, y+HEIGHT/2+5,  x+WIDTH/2, y+HEIGHT/2+5); break;
          case WEST:  g.drawLine(x-10,       y+HEIGHT/2-5,  x+WIDTH/2, y+HEIGHT/2-5); break;
          case NONE:  break;
          }
          g.setColor(RED);
          ((Graphics2D)g).setStroke(new BasicStroke((float)1));
          switch(cell.xi()) {
          case NW: g.drawLine(x-10+3,       y-10,        x+WIDTH/2+3, y+HEIGHT/2); break;
          case SW: g.drawLine(x-10-3,       y+HEIGHT+10, x+WIDTH/2-3, y+HEIGHT/2); break;
          case NE: g.drawLine(x+WIDTH+10+3, y-10,        x+WIDTH/2+3, y+HEIGHT/2); break;
          case SE: g.drawLine(x+WIDTH+10-3, y+HEIGHT+10, x+WIDTH/2-3, y+HEIGHT/2); break;
          case NONE:  break;
          }
          ((Graphics2D)g).setStroke(new BasicStroke(1));
        */
        /*
          if (selx==cx && sely==cy) {
          int xp = 23 * (WIDTH+2) + 100;
          int yp = 100;
          g.setColor(Color.white);
          g.fillRect(xp, yp, 300, 1000);
          g.setColor(Color.black);
          g.drawString("Cell " + cx + "," + cy,       xp, (yp+=15));
          //g.drawString("X-Lut: " + bin8(cell.xlut()), xp, (yp+=15));
          g.drawString("X-Lut: " + cell.printXLut(), xp, (yp+=15));
          //g.drawString("Y-Lut: " + bin8(cell.ylut()), xp, (yp+=15));
          g.drawString("Y-Lut: " + cell.printYLutX(), xp, (yp+=15));
          }
        */
        if ((cell.ylut()&0xff)==0xff && (cell.xlut()&0xff)==0xff) {
            g.setColor(new Color(0x00, 0x00, 0xff));
            g.drawString("1", left(cell) + 12, top(cell) + 30);
        }
        if ((cell.ylut()&0xff)==0x00 && (cell.xlut()&0xff)==0x00) {
            g.setColor(new Color(0x00, 0x00, 0xff));
            g.drawString("0", left(cell) + 12, top(cell) + 30);
        }
        if ((cell.ylut()&0xff)==0xB2) {
            //System.out.println("muller @ " + cell);
            //g.setColor(RED);
            //g.drawString("X="+(x?"1":"0"), left(cell) + 10, top(cell) + 20);
            g.setColor(new Color(0x00, 0xaa, 0x00));
            g.drawString("C", left(cell) + 12, top(cell) + 30);
        }

    }

    public static void showit(FpslicDevice dev, SlipwayBoard board, final DemoVisualizer vis) {
        final Color red = new Color(0xff, 0x99, 0x99);
        final Color green = new Color(0x99, 0xff, 0x99);
        final Graphics g = vis.getGraphics();
        g.setFont(new Font("sansserif", Font.BOLD, 24));
        for(int xx=0; xx<=dev.getWidth()-2; xx++) {
            for(int yy=dev.getHeight()-1; yy>=0; yy--) {
                final int x = xx;
                final int y = yy;
                final FpslicDevice.Cell cell = dev.cell(x, y);
                if ((cell.ylut()&0xff)!=0xB2) continue;
                SlipwayBoard.ByteCallback bc = new SlipwayBoard.ByteCallback() {
                        public void call(byte b) throws Exception {
                            boolean v = (b & 0x80) != 0;
                            vis.drawCell(g, x, y, v?red:green);
                        }
                    };
                try {
                    ExperimentUtils.scan(cell, YLUT, true);
                    board.readFpgaData(bc);
                    ExperimentUtils.scan(cell, NONE, false);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

}
