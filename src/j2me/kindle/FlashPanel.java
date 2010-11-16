/*
 * FlashPanel.java
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

import com.amazon.kindle.kindlet.ui.KPanel;
import com.amazon.kindle.kindlet.ui.KRepaintManager;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.lang.reflect.InvocationTargetException;
import org.apache.log4j.Logger;

/**
 *
 * @author VDP <vdp DOT kindle AT gmail.com>
 */
public class FlashPanel extends KPanel {

    /** The object used for logging */
    private Logger logger;
    /** indicator for flashing panel while repainting */
    private boolean m_doFlash;

    public FlashPanel(Logger log) {
        super();
        this.logger = log;
    }

    /**
     * {@inheritDoc}
     */
    public void paint(final Graphics g) {
        if (m_doFlash) {
            g.setColor(Color.white);
            g.clearRect(0, 0, this.getWidth(), this.getHeight());
            m_doFlash = false;
        }
        else {
            super.paint(g);
        }
    }

    /**
     * Overloads repaint that flashes the screen by doing two paints.
     * It first paints the panel black and then paints the contents again flashing the screen
     * @param flashingRepaint sets the repaint to cause a flash or do a regular paint.
     */
    public void repaint(final boolean flashingRepaint) {
        m_doFlash = flashingRepaint;
        if (m_doFlash) {
            final FlashPanel currentFlashingPanel = this;
            EventQueue.invokeLater(new Runnable() {

                /** {@inheritDoc} */
                public void run() {
                    try {
                        m_doFlash = true;
                        KRepaintManager.paintImmediately(currentFlashingPanel, true);
                    } catch (final InterruptedException e1) {
                        logger.error("Error occured while repainting FlashingPanel", e1);
                    } catch (final InvocationTargetException e2) {
                        logger.error("Error occured while repainting FlashingPanel", e2);
                    }
                }
            });
        }
        super.repaint();
    }

    /**
     * {@inheritDoc}
     */
    public void repaint() {
        repaint(false);
    }
}
