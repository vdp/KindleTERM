/*
 * KindleTerminal.java
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

package kindle;

import awt.AwtSession;
import com.amazon.kindle.kindlet.event.KindleKeyCodes;
import com.amazon.kindle.kindlet.ui.KRepaintManager;
import com.amazon.kindle.kindlet.ui.KTextArea;
import com.amazon.kindle.kindlet.ui.KindletUIResources;
import com.amazon.kindle.kindlet.ui.KindletUIResources.KColorName;
import com.amazon.kindle.kindlet.ui.KindletUIResources.KFontFamilyName;
import com.amazon.kindle.kindlet.ui.KindletUIResources.KFontStyle;
import gui.Redrawable;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import org.apache.log4j.Logger;
import terminal.VT320;

/**
 * The terminal emulation window.
 * 
 * This is the component that renders the emulation buffer
 * and accepts the user keyboard.
 *
 * @author VDP <vdp DOT kindle AT gmail.com>
 */
public class KindleTerminal 
        extends KTextArea
        implements Redrawable, KeyListener {

    private static final int DIRTY_UNDEFINED = -1;

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
    public Color fgcolor;
    public Color bgcolor;
    private final Object paintMutex = new Object();

    /** local copy of the lines that were displayed on the previous refresh */
    private char[][] prevChars;
    private int[][] prevAttrs;

    /** A buffer to hold the characters of the line currently rendered */
    private char[] renderChars;

    /** previous cursor positions */
    int prevCursorX, prevCursorY;

    private boolean invalid = true;

    private boolean symbolActive = false;

    int nDirty;
    Rectangle[] dirtyRects = new Rectangle[UPDATE_THRESHOLD];
    int currentDirty = DIRTY_UNDEFINED;

    private KindletUIResources resources;

    private Logger log;

    private Redrawer redrawer;

    /**
     * @param buffer
     */
    public KindleTerminal(VT320 buffer, AwtSession session, KindletUIResources rsrc) {

        this.log = Logger.getLogger(KindleTerminal.class.getName());
        
        this.buffer = buffer;
        this.resources = rsrc;
        
        rotated = ROT_NORMAL;
        
        initFont();

        this.session = session;

        fgcolor = resources.getColor(KColorName.BLACK);
        bgcolor = resources.getColor(KColorName.WHITE);

        this.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                sizeChanged();
                redraw();
            }
        });

        this.prevCursorX = buffer.cursorX;
        this.prevCursorY = buffer.cursorY;

        this.addKeyListener(this);

        //setBackground(bgcolor);

        sizeChanged();

        for (int d = 0; d < dirtyRects.length; d++)
            dirtyRects[d] = new Rectangle();

        this.redrawer = new Redrawer(50, 1000, this);
        redrawer.start();

        buffer.setDisplay(this);
    }

    public void update(Graphics g) {
        log.debug("update() called");
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

        int numDirty = 0;

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

                if (cursorCountDn + numDirty >= UPDATE_THRESHOLD)  {
                    searchFurther = false; // (probably) doesn't make sense to continue
                    continue;
                }

                this.dirtyRects[numDirty++].setBounds(startDirty, r, endDirty-startDirty+1, 1);
                //System.out.println("Dirty rect: " + nDirty + " cursorCountDn: " + cursorCountDn);
            }
        }

        if (!isPrevCursorUpdated) {
            this.dirtyRects[numDirty++].setBounds(prevCursorX, prevCursorY, 1, 1);
        }

        if (!isCursorUpdated) {
            this.dirtyRects[numDirty++].setBounds(buffer.cursorX, buffer.cursorY, 1, 1);
        }

        prevCursorX = buffer.cursorX;
        prevCursorY = buffer.cursorY;

        return numDirty;
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

        //String as = attrString(buffer.charAttributes[line][start]);
        //System.out.println("Span[" + line + ":" + start + "-" + end + as + "] " + new String(renderChars));
        int attr = buffer.charAttributes[line][start];
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
                        (buffer.cursorY - top + buffer.screenBase - buffer.windowBase) * fontHeight, 4,
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
        renderChars = (char[]) buffer.charArray[line].clone();
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

//    private void redrawFast(Graphics gScreen, Graphics gBuff, int nDirty) {
//        for (int d = 0; d < nDirty; d++) {
//            Rectangle rc = dirtyRects[d];
//            renderChars = (char[]) prevChars[rc.y].clone();
//            for (int i = rc.x; i < rc.x + rc.width; i++) {
//                if (renderChars[i] < ' ')
//                    renderChars[i] = ' ';
//            }
//            drawLine(gBuff, rc.y, rc.x, rc.width);
//
//            int x1 = rc.x * fontWidth;
//            int x2 = (rc.x + rc.width)*fontWidth + 1;
//            int y1 = rc.y * fontHeight;
//            int y2 = y1 + fontHeight + 1;
//            //System.out.println("Fast line " + rc.y + " [" + rc.x + ":" + (rc.x+rc.width-1) + "]" + new String(prevChars[rc.y]));
//            //System.out.println("x1:" + x1 + " x2:" + x2 + " y1:" + y1 + " y2:" + y2);
//            gScreen.drawImage(backingStore, x1, y1, x2, y2, x1, y1, x2, y2, null);
//        }
//    }

    public void paint(Graphics g) {

        synchronized (paintMutex) {
            if (invalid) {
                Graphics dblBuf = this.backingStore.getGraphics();

                if (currentDirty == DIRTY_UNDEFINED) {
                    log.debug("Fullscreen update");
                    redrawAll(dblBuf);
                    g.drawImage(backingStore, 0, 0, null);
                }
                else {
                    log.debug("Fast redrawing rectangle " + currentDirty);
                    Rectangle rc = dirtyRects[currentDirty];
                    renderChars = (char[]) prevChars[rc.y].clone();
                    for (int i = rc.x; i < rc.x + rc.width; i++) {
                        if (renderChars[i] < ' ') {
                            renderChars[i] = ' ';
                        }
                    }
                    drawLine(dblBuf, rc.y, rc.x, rc.width);

                    int x1 = rc.x * fontWidth;
                    int x2 = (rc.x + rc.width) * fontWidth + 1;
                    int y1 = rc.y * fontHeight;
                    int y2 = y1 + fontHeight + 1;
                    //System.out.println("Fast line " + rc.y + " [" + rc.x + ":" + (rc.x+rc.width-1) + "]" + new String(prevChars[rc.y]));
                    //System.out.println("x1:" + x1 + " x2:" + x2 + " y1:" + y1 + " y2:" + y2);
                    g.drawImage(backingStore, x1, y1, x2, y2, x1, y1, x2, y2, null);
                }

                paintMutex.notifyAll();
                invalid = false;
            }
            else {
                log.debug("System re-paint()");
                g.drawImage(backingStore, 0, 0, null);
            }
        }
    }


    public void redraw() {
        redrawer.requestRedraw();
    }

    private void initFont() {
        font = resources.getFont(KFontFamilyName.MONOSPACE,
                FONT_SIZE, KFontStyle.BOLD);
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
        if (code == KindleKeyCodes.VK_SYMBOL) {
            symbolActive = !symbolActive;
        } else if (code == KindleKeyCodes.VK_BACK && symbolActive) {
            symbolActive = false;
        } else if (code == KindleKeyCodes.VK_FIVE_WAY_SELECT && symbolActive) {
            symbolActive = false;
        } else if (symbolActive) {
            return;
        } else if (code == KeyEvent.VK_BACK_SPACE) {
            buffer.keyPressed(VT320.VK_BACK_SPACE, 0);
        }
        else if (code == KindleKeyCodes.VK_FIVE_WAY_UP) {
            buffer.keyPressed(VT320.VK_UP, 0);
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

    public void setText(String arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getText() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isEditable() {
        //throw new UnsupportedOperationException("Not supported yet.");
        return true;
    }

    public void setEditable(boolean arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void kill() {
        redrawer.kill();
    }


    /**
     * Rate limits the repaints and makes them sequential.
     */
    private class Redrawer extends Thread {

        private final static long SCHED_UNDEFINED = -1;

        private boolean killed;

        private Component component;

        private long squelchTime;
        private long maxSquelch;

        private long lastRepaint;
        private long lastRequest;

        private final Object scheduleLock = new Object();
        private long scheduledTime;

        private Logger log;

        /**
         * Constructor
         *
         * @param squelchTime if new redraw request is received within squelchTime
         *                    milliseconds from the previous redraw request
         *                    the repaint is delayed for another squelchTime ms,
         *                    unless maxSquelch milliseconds are already passed
         *                    since the last repaint
         *
         * @param maxSquelch  new repaint request is issued if maxSquelch ms are
         *                    passed since the last repaint even if the requests
         *                    should be rate-limited according to squelchTime
         */
        public Redrawer(int squelchTime, int maxSquelch, Component component) {
            this.squelchTime = squelchTime;
            this.maxSquelch = maxSquelch;
            this.component = component;

            lastRepaint = SCHED_UNDEFINED;
            lastRequest = SCHED_UNDEFINED;
            scheduledTime = SCHED_UNDEFINED;

            killed = false;

            this.log = Logger.getLogger(KindleTerminal.Redrawer.class.getName());
        }

        public void requestRedraw() {
            synchronized (scheduleLock) {
                long time = System.currentTimeMillis();
                long dPaint = time - lastRepaint;
                long dRequest = time - lastRequest;
//                if (lastRepaint == SCHED_UNDEFINED || dPaint > maxSquelch) {
//                    log.debug("Redraw request should be satisfied NOW");
//                    scheduledTime = time; // repaint NOW
//                    scheduleLock.notifyAll();
//                }
//                else if (lastRequest == SCHED_UNDEFINED || dRequest < squelchTime) {
//                    log.debug("Redraw request is subject to rate limit");
//                    scheduledTime = time + squelchTime;
//                    scheduleLock.notifyAll();
//                }

                if (lastRequest == SCHED_UNDEFINED || dRequest < squelchTime) {
                    if (lastRepaint == SCHED_UNDEFINED || dPaint > maxSquelch) {
                        log.debug("Redraw request cannot be squelched anymore. Redrawing NOW");
                        scheduledTime = time; // repaint NOW
                        scheduleLock.notifyAll();
                    }
                    else {
                        log.debug("Redraw request is subject to rate limit");
                        scheduledTime = time + squelchTime;
                        scheduleLock.notifyAll();
                    }
                } else {
                    log.debug("Redraw request is not subject to rate limit. Redrawing NOW");
                    scheduledTime = time; // repaint NOW
                    scheduleLock.notifyAll();
                }

                lastRequest = time;
            }
        }

        public void kill() {
            synchronized (scheduleLock) {
                log.debug("Redrawer kill requested");
                this.killed = true;
                scheduleLock.notifyAll();
            }
        }

        private void processRepaint() {
            synchronized (paintMutex) {
                nDirty = findDirtyRects();
                if (nDirty >= UPDATE_THRESHOLD) {
                    log.debug("Fullscreen repaint scheduled");
                    currentDirty = KindleTerminal.DIRTY_UNDEFINED;
                    KindleTerminal.this.invalid = true;
                    EventQueue.invokeLater(new Runnable() {

                        public void run() {
                            synchronized (paintMutex) {
                                //log.debug("Fullscreen updater");
                                KRepaintManager rm = KRepaintManager.currentManager(component);
                                rm.addDirtyRegion(component, 0, 0, width, height);
                                rm.paintDirtyRegions(false);
                                paintMutex.notifyAll();
                            }
                        }
                    });

                    try {
                        paintMutex.wait();
                    } catch (InterruptedException ie) {
                        log.warn(ie.toString());
                    }

                    log.debug("Fullscreen repaint finished");
                } // nDirty >= UPDATE_THRESHOLD
                else if (nDirty > 0) {
                    log.debug("Partial update scheduled. Dirty rects: " + nDirty);
                    final KRepaintManager rm = KRepaintManager.currentManager(component);
                    for (int i = 0; i < nDirty; i++) {
                        KindleTerminal.this.invalid = true;
                        KindleTerminal.this.currentDirty = i;
                        log.debug("Current dirty rect: " + dirtyRects[currentDirty]);
                        EventQueue.invokeLater(new Runnable() {

                            public void run() {
                                Rectangle rect = dirtyRects[KindleTerminal.this.currentDirty];
                                int x = rect.x * fontWidth;
                                int y = rect.y * fontHeight;
                                int w = rect.width * fontWidth;
                                int h = rect.height * fontHeight;
                                rm.addDirtyRegion(component, x, y, w, h);
                                rm.paintDirtyRegions(false);
                            }
                        });
                
                        try {
                            paintMutex.wait();
                        } catch (InterruptedException ie) {
                            log.warn(ie.toString());
                        }

                        log.debug("Dirty rect " + dirtyRects[currentDirty] + " repaint finished");
                    }
                }

                currentDirty = DIRTY_UNDEFINED;
            } // synchronized (paintMutex)

            synchronized (scheduleLock) {
                lastRepaint = System.currentTimeMillis();
            }
        }

        public void run() {

            MAINLOOP:
            while (!killed) {
                synchronized (scheduleLock) {

                    long time = System.currentTimeMillis();
                    while (scheduledTime == SCHED_UNDEFINED || time < scheduledTime) {

                        if (killed)
                            break MAINLOOP;

                        try {
                            if (scheduledTime == SCHED_UNDEFINED) {
                                log.debug("No repaint is currently scheduled. Going to sleep");
                                scheduleLock.wait();
                            }
                            else {
                                long waitTime = scheduledTime - time;
                                log.debug("Repaint cheduled in " + waitTime);
                                scheduleLock.wait(waitTime);
                            }
                        } catch (InterruptedException ie) {
                            log.warn(ie.toString());
                        }

                        time = System.currentTimeMillis();
                    } // while

                    scheduledTime = SCHED_UNDEFINED;
                } // synchronized (scheduleLock)

                processRepaint();
            }

        }

    }
}

