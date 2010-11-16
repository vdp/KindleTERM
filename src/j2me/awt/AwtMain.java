/*
 * AwtMain.java
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

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import kindle.RemoteKbdReceiver;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import terminal.VT320;

/**
 * AWT interface on top of MidpSSH infrastructure
 *
 * @author VDP <vdp DOT kindle AT gmail.com>
 */
public class AwtMain {

    private Frame root;
    private AwtSshSession session;
    private Logger log;

    private void configLog() {
        PropertyConfigurator.configure("conf/log4j.properties");
    }

    public AwtMain(String title) {
        configLog();
        this.log = Logger.getLogger(AwtMain.class.getName());
        log.debug("Log started");
        //System.out.println("Work dir:" + System.getProperty("user.dir"));
        root = new Frame(title);
        root.setSize(800, 600);
        root.addWindowListener(
                new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        System.exit(0);
                    }
                }
        );
        Panel panel = new Panel();
        Button b1 = new Button("Does nothing");
        panel.add(b1);
        //root.add(panel, BorderLayout.NORTH);
        //Settings.defaults();
        this.session = new AwtSshSession();
        AwtTerminal term = new AwtTerminal(session.getEmulation(), session);

        RemoteKbdReceiver rk = new RemoteKbdReceiver(3333, term, term);
        rk.start();

        root.add(term, BorderLayout.CENTER);
        session.connect("localhost", 22, "user", "pass");
        //System.out.print("Connect ...\n");
        term.setFocusable(true);
        term.setFocusTraversalKeysEnabled(false);
        term.setPreferredSize(new Dimension(1200, 950));

        root.pack();
        root.setVisible(true);
        //printTerminal(session.getTerminal().buffer);
    }

    public void printTerminal(VT320 buf) {
        for (int i = 0; i < buf.charArray.length; i++){
            String line = new String(buf.charArray[i]);
            System.out.println(line + "\n");
        }
  
    }

    public static void main(String[] args) {
        AwtMain am = new AwtMain("Test");
    }
}
