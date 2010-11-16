/*
 * AwtTerminal.java
 * 
 * Copyright (c) 2010 VDP <vdp DOT kindle AT gmail.com>.
 * 
 * This file is part of MidpSSH.
 * 
 * MidpSSH is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MidpSSH is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MidpSSH.  If not, see <http ://www.gnu.org/licenses/>.
 */
package awt;

//import app.session.Session;
import gui.Redrawable;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import terminal.VT320;

/**
 * A rewrite of {@link terminal.Terminal} that uses the AWT UI widgets.
 * 
 * @author VDP <vdp DOT kindle AT gmail.com>
 */
public class AwtTerminal extends Canvas
        implements Redrawable, KeyListener {

    // ------------------------------- Constants
    private static final int ROT_NORMAL = 0;
    private static final int ROT_90 = 2;
    private static final int FONT_SIZE = 20;

    // If more than UPDATE_THRESHOLD paint operations are needed the
    // whole screen is refreshed instead.
    private static final int UPDATE_THRESHOLD = 4;

    // ------------------------------- Data fields
    private AwtSession session;
    private Font font;
    /** the VDU buffer */
    protected VT320 buffer;
    /** first top and left character in buffer, that is displayed */
    protected int top, left;
    protected int width, height;
    private int fontWidth, fontHeight, fontAscent;
    protected int rotated;
    /** display size in characters */
    public int rows, cols;
    private Image backingStore = null;
    public Color fgcolor = new Color(0x000000);
    public Color bgcolor = new Color(0xffffff);
    private final Object paintMutex = new Object();

    /** local copy of the lines that were displayed on the previous refresh */
    private char[][] prevChars;
    private int[][] prevAttrs;
    
    /** A buffer to hold the characters of the line currently rendered */
    private char[] renderChars;

    /** previous cursor positions */
    int prevCursorX, prevCursorY;

    private boolean invalid = true;

    Rectangle[] dirtyRects = new Rectangle[UPDATE_THRESHOLD];

    /**
     * @param buffer
     */
    public AwtTerminal(VT320 buffer, AwtSession session) {
        this.buffer = buffer;
        buffer.setDisplay(this);
        rotated = ROT_NORMAL;
        initFont();
        //top = 0;
        //left = 0;
        this.session = session;
        this.addComponentListener(new ComponentAdapter() {

            public void componentResized(ComponentEvent e) {
                sizeChanged();
                redraw();
            }
        });

        this.prevCursorX = buffer.cursorX;
        this.prevCursorY = buffer.cursorY;

        this.addKeyListener(this);

        setBackground(bgcolor);

        sizeChanged();

        for (int d = 0; d < dirtyRects.length; d++)
            dirtyRects[d] = new Rectangle();
    }

    public void update(Graphics g) {
        paint(g);
        //super.update(g);
    }

    protected void sizeChanged() {
        width = getWidth();
        height = getHeight();
        if (rotated != ROT_NORMAL) {
            width = getHeight();
            height = getWidth();
        }
        cols = width / fontWidth;
        rows = height / fontHeight;
        if (width > 0 && height > 0) {
            //System.out.println("Backing store created");
            backingStore = createImage(width, height);
        }

        int virtualCols = cols;
        int virtualRows = rows;

        this.renderChars = new char[cols];

        buffer.setScreenSize(virtualCols, virtualRows);
    }

    private void doDisconnect() {
        session.disconnect();
    }

    /**
     * Finds all screen regions that needs an update
     *
     * Those rectangles (un)covered by the cursor are also included.
     *
     * @param  rects the result will be recorded here
     * @return the number of valid dirty rectangles
     */
    private int findDirtyRects() {
        boolean isPrevCursorUpdated = false;
        boolean isCursorUpdated = false;
        int cursorCountDn = 2;
        if (buffer.cursorX == prevCursorX &&
                buffer.cursorY == prevCursorY) {

            cursorCountDn = 0;
            isPrevCursorUpdated = true;
            isCursorUpdated = true;
        }

        int nDirty = 0;

        int nRows = buffer.charArray.length;
        int nCols = buffer.charArray[0].length;
        //int startVisible = buffer.windowBase;
        //int endVisible = buffer.windowBase + buffer.height;

        // Init 'previous' buffers
        if (prevChars == null ||
                prevChars.length != nRows ||
                prevChars[0].length != nCols) {

            prevChars = new char[nRows][];
            prevAttrs = new int[nRows][];
            for (int r=0; r < nRows; r++) {
                prevChars[r] = (char[]) buffer.charArray[r].clone();
                prevAttrs[r] = (int[]) buffer.charAttributes[r].clone();
            }
        }

        boolean searchFurther = true;

        for (int r=0; r < nRows; r++) {
            int startDirty = nCols;
            int endDirty = 0;

            char[] prevLine = prevChars[r];
            int[] prevLineAttrs = prevAttrs[r];
            char[] curLine = buffer.charArray[r];
            int[] curLineAttrs = buffer.charAttributes[r];

            for (int c=0; c < nCols; c++) {
                if(prevLine[c] != curLine[c] ||
                   prevLineAttrs[c] != curLineAttrs[c]) {
                    if (c < startDirty)
                        startDirty = c;

                    endDirty = c;

                    prevLine[c] = curLine[c];
                    prevLineAttrs[c] = curLineAttrs[c];
                }
            }

            if (searchFurther && startDirty <= endDirty) {
                // prevCursorY == r-startVisible ??? (is it absolute or relative)
                if (cursorCountDn != 0 && prevCursorY == r &&
                    prevCursorX <= endDirty && prevCursorX >= startDirty) {
                    isPrevCursorUpdated = true;
                    --cursorCountDn;
                }

                if (cursorCountDn != 0 && buffer.cursorY == r &&
                    buffer.cursorX <= endDirty && buffer.cursorX >= startDirty) {
                    isCursorUpdated = true;
                    --cursorCountDn;
                }

                if (cursorCountDn + nDirty >= UPDATE_THRESHOLD)  {
                    searchFurther = false; // (probably) doesn't make sense to continue
                    continue;
                }

                this.dirtyRects[nDirty++].setBounds(startDirty, r, endDirty-startDirty+1, 1);
                //System.out.println("Dirty rect: " + nDirty + " cursorCountDn: " + cursorCountDn);
            }
        }

        if (!isPrevCursorUpdated) {
            this.dirtyRects[nDirty++].setBounds(prevCursorX, prevCursorY, 1, 1);
        }

        if (!isCursorUpdated) {
            this.dirtyRects[nDirty++].setBounds(buffer.cursorX, buffer.cursorY, 1, 1);
        }

        prevCursorX = buffer.cursorX;
        prevCursorY = buffer.cursorY;

        return nDirty;
    }

    private String attrString(int attr) {
        StringBuffer sb = new StringBuffer();
        sb.append("[FG:").append(Integer.toString((attr&VT320.COLOR_FG) >> 4));
        sb.append(" BG:").append(Integer.toString((attr&VT320.COLOR_BG) >> 8));
        if (0 != (attr&VT320.BOLD))
            sb.append(',').append("BLD");
        if (0 != (attr&VT320.INVERT))
            sb.append(',').append("INV");
        if (0 != (attr&VT320.NORMAL))
            sb.append(',').append("NRML");
        if (0 != (attr&VT320.UNDERLINE))
            sb.append(',').append("ULN");
        if (0 != (attr&VT320.LOW))
            sb.append(',').append("LOW");

        sb.append(',').append("RAW:").append(Integer.toHexString(attr));

        sb.append(']');

        return sb.toString();
    }

    /**
     * Draws a string with homogenous attributes
     */
    private void drawSpan(Graphics g, int line, int start, int end) {

        String as = attrString(buffer.charAttributes[line][start]);
        System.out.println("Span[" + line + ":" + start + "-" + end + as + "] " + new String(renderChars));
        int attr = prevAttrs[line][start];
        Color fg = fgcolor;
        Color bg = bgcolor;
        Font curFont = font;

        if ((attr & VT320.INVERT) != 0) {
            bg = fgcolor;
            fg = bgcolor;
        }

        // clear the background
        int x1 = start * fontWidth;
        int y1 = line * fontHeight;
        int w = (end - start + 1) * fontWidth;
        int h = fontHeight;
        g.setColor(bg);
        g.fillRect(x1, y1, w, h);

        g.setColor(fg);
        g.setFont(font);
        g.drawChars(renderChars, start, end - start + 1, x1, y1 + fontAscent);

        if (buffer.cursorY == line &&
                buffer.cursorX >= start && buffer.cursorX <= end)
            g.fillRect((buffer.cursorX - left) * fontWidth,
                        (buffer.cursorY - top + buffer.screenBase - buffer.windowBase) * fontHeight, fontWidth,
                        fontHeight);
    }

    /**
     * Draws a single line of text.
     *
     * @param line   The line from the buffer to be rendered
     * @param offset offset of the first character from the line to rendered
     * @param len the length of the subsring to be rendered
     */
    private void drawLine(Graphics g, int line, int offset, int len) {
        renderChars = (char[]) prevChars[line].clone();
        int[] curAttrs = buffer.charAttributes[line];
        int attr;
        int start = offset;
        int end = offset;

        while (end < offset + len) {
            attr = curAttrs[start];
            while (curAttrs[end] == attr) {
                if (renderChars[end] < ' ')
                    renderChars[end] = ' ';

                if (end >= offset+len-1)
                    break;

                end++;
            }

            //System.out.println("Span[" + line + ":" + start + "-" + end + "] " + new String(renderChars));
            drawSpan(g, line, start, end);
            start = end+1;
            end = start;
        }
    }

    private void redrawAll(Graphics dblBuf) {
        
        int nRows = buffer.charArray.length;
        int nCols = buffer.charArray[0].length;
        for (int r=0; r < nRows; r++) {
            drawLine(dblBuf, r, 0, nCols);
        }
    }

    private void redrawFast(Graphics gScreen, Graphics gBuff, int nDirty) {
        for (int d = 0; d < nDirty; d++) {
            Rectangle rc = dirtyRects[d];
            renderChars = (char[]) prevChars[rc.y].clone();
            for (int i = rc.x; i < rc.x + rc.width; i++) {
                if (renderChars[i] < ' ')
                    renderChars[i] = ' ';
            }
            drawLine(gBuff, rc.y, rc.x, rc.width);
            
            int x1 = rc.x * fontWidth;
            int x2 = (rc.x + rc.width)*fontWidth + 1;
            int y1 = rc.y * fontHeight;
            int y2 = y1 + fontHeight + 1;
            //System.out.println("Fast line " + rc.y + " [" + rc.x + ":" + (rc.x+rc.width-1) + "]" + new String(prevChars[rc.y]));
            //System.out.println("x1:" + x1 + " x2:" + x2 + " y1:" + y1 + " y2:" + y2);
            gScreen.drawImage(backingStore, x1, y1, x2, y2, x1, y1, x2, y2, null);
        }
    }

    public void paint(Graphics g) {
        
        synchronized (paintMutex) {
            if (invalid) {
                int nDirty = findDirtyRects();
                Graphics dblBuf = this.backingStore.getGraphics();

                if (nDirty >= UPDATE_THRESHOLD) {
                    redrawAll(dblBuf);
                    g.drawImage(backingStore, 0, 0, null);
                }
                else 
                    redrawFast(g, dblBuf, nDirty);

                invalid = false;
            }
            else {
                g.drawImage(backingStore, 0, 0, null);
            }
        }
    }

   
    public void redraw() {
        synchronized (paintMutex) {
            invalid = true;
            repaint();
        }
    }

    private void initFont() {
        initSystemFont(FONT_SIZE);

    }

    private void initSystemFont(int size) {
        font = new Font("Monospaced", Font.PLAIN, size);
        FontMetrics fm = getToolkit().getFontMetrics(font);
        fontHeight = fm.getHeight();
        fontWidth = fm.charWidth('W');
        fontAscent = fm.getAscent();
    }

    public void keyTyped(KeyEvent e) {
        buffer.keyTyped(0, e.getKeyChar(), 0);
        //System.out.println("Code: " + e.getKeyCode());
    }

    public void keyPressed(KeyEvent e) {
        //System.out.println(e.getKeyText(e.getKeyCode()));
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_ENTER) {
            buffer.keyPressed(VT320.VK_ENTER, 0);
            System.out.println("VK_ENTER: " + code);
        } else if (code == KeyEvent.VK_BACK_SPACE) {
            buffer.keyPressed(VT320.VK_BACK_SPACE, 0);
        } else if (code == KeyEvent.VK_LEFT) {
            buffer.keyPressed(VT320.VK_LEFT, 0);
        } else if (code == KeyEvent.VK_RIGHT) {
            buffer.keyPressed(VT320.VK_RIGHT, 0);
        } else if (code == KeyEvent.VK_UP) {
            buffer.keyPressed(VT320.VK_UP, 0);
        } else if (code == KeyEvent.VK_DOWN) {
            buffer.keyPressed(VT320.VK_DOWN, 0);
        } else if (code == KeyEvent.VK_TAB) {
            // setFocusTraversalKeysEnabled(false) should be set in order to work
            buffer.keyPressed(VT320.VK_TAB, 0);
        } else if (code == KeyEvent.VK_PAGE_DOWN) {
            buffer.keyPressed(VT320.VK_PAGE_DOWN, 0);
        } else if (code == KeyEvent.VK_PAGE_UP) {
            buffer.keyPressed(VT320.VK_PAGE_UP, 0);
        }
    }

    public void keyReleased(KeyEvent arg0) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }
}

