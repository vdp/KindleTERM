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

import awt.AwtSshSession;
import com.amazon.kindle.kindlet.AbstractKindlet;
import com.amazon.kindle.kindlet.KindletContext;
import com.amazon.kindle.kindlet.ui.KButton;
import com.amazon.kindle.kindlet.ui.KLabel;
import com.amazon.kindle.kindlet.ui.KMenu;
import com.amazon.kindle.kindlet.ui.KMenuItem;
import com.amazon.kindle.kindlet.ui.KOptionPane;
import com.amazon.kindle.kindlet.ui.KPanel;
import com.amazon.kindle.kindlet.ui.KPasswordField;
import com.amazon.kindle.kindlet.ui.KTextField;
import com.amazon.kindle.kindlet.ui.border.KLineBorder;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Implementation of the lifecycle of the KindleTerm.
 * 
 * @author VDP <vdp DOT kindle AT gmail.com>
 */
public class TermKindlet extends AbstractKindlet
    implements RemoteKbdReceiver.RKbdStatusListener {

    private static final String SESSION_PROPERTIES = "session.properties";

    private KindletContext ctx;
    private Container root;
    private KindleTerminal term;
    private KPanel connectionDlg;
    private AwtSshSession session;
    private Logger log;
    private RemoteKbdReceiver remoteKeyboard;
    
    private String host = "Host",
                   user = "User",
                   pass = "Pass";
    private int port = 22;

    private void makeConnectDialog() {
        connectionDlg = new KPanel();

        if (!loadSessionConfig()) {
            host = "hostname";
            port = 22;
            user = "username";
            pass = "password";
        }

        final KLabel labelHost = new KLabel("Host: ");
        final KTextField tfHost = new KTextField(host);
        final KLabel labelPort = new KLabel("Port: ");
        final KTextField tfPort = new KTextField(String.valueOf(port));
        final KLabel labelUser = new KLabel("Username: ");
        final KTextField tfName = new KTextField(user);
        final KLabel labelPass = new KLabel("Password: ");
        final KPasswordField tfPass = new KPasswordField(pass);

        KButton ok = new KButton("OK");
        ok.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                host = tfHost.getText();
                user = tfName.getText();
                pass = tfPass.getText();
                port = Integer.parseInt(tfPort.getText());

                root.remove(connectionDlg);
                connectionDlg = null;
                session = new AwtSshSession();
                session.connect(host, port, user, pass);

                term = new KindleTerminal(session.getEmulation(),
                        session, ctx.getUIResources());
                term.setFocusable(true);
                term.setFocusTraversalKeysEnabled(false);
                root.add(term, BorderLayout.CENTER);
                term.requestFocus();

                ctx.setSubTitle('[' + host + ']');

                remoteKeyboard = new RemoteKbdReceiver(3333, term, term);
                remoteKeyboard.setRKbdStatusListener(TermKindlet.this);
                remoteKeyboard.start();

                Thread cfgSaver = new Thread(new Runnable() {

                    public void run() {


                        int choice = KOptionPane.CANCEL_OPTION;
                        try {
                            choice = KOptionPane.showConfirmDialog(root,
                                    "Do you want to save the session options",
                                    "Save session?", KOptionPane.OK_CANCEL_OPTION);
                        } catch (InterruptedException ie) {
                            log.warn(ie.toString());
                        }

                        if (choice == KOptionPane.OK_OPTION) {
                            saveSessionConfig();
                        }
                        else {
                            File config = new File(ctx.getHomeDirectory(), SESSION_PROPERTIES);
                            config.delete();
                        }
                    }
                });
                cfgSaver.start();
            }
        });

        connectionDlg.setLayout(new GridBagLayout());
        KLineBorder border = new KLineBorder(1, true);

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.WEST;
        gbc.ipady = 10;
        gbc.ipadx = 10;

        gbc.gridwidth = GridBagConstraints.RELATIVE;
        connectionDlg.add(labelHost, gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        tfHost.setBorder(border);
        connectionDlg.add(tfHost, gbc);

        gbc.gridwidth = GridBagConstraints.RELATIVE;
        connectionDlg.add(labelPort, gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        tfPort.setBorder(border);
        connectionDlg.add(tfPort, gbc);

        gbc.gridwidth = GridBagConstraints.RELATIVE;
        connectionDlg.add(labelUser, gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        tfName.setBorder(border);
        connectionDlg.add(tfName, gbc);

        gbc.gridwidth = GridBagConstraints.RELATIVE;
        connectionDlg.add(labelPass, gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        tfPass.setBorder(border);
        connectionDlg.add(tfPass, gbc);

        gbc.insets = new Insets(100, 0, 0, 0);
        gbc.anchor = GridBagConstraints.CENTER;
        connectionDlg.add(ok, gbc);
    }

    private KMenu createMenu() {
        KMenu menu = new KMenu();
        KMenuItem itemConnect = new KMenuItem("Connect ...");
        itemConnect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                root.add(connectionDlg, BorderLayout.CENTER);
            }

        });

        return menu;
    }

    private boolean loadSessionConfig() {
        try {
            File propFile = new File(ctx.getHomeDirectory(), SESSION_PROPERTIES);
            if (!propFile.exists())
                return false;
            FileInputStream fis = new FileInputStream(propFile);
            Properties props = new Properties();
            props.load(fis);
            host = props.getProperty("host");
            port = Integer.parseInt(props.getProperty("port"));
            user = props.getProperty("user");
            pass = props.getProperty("pass");
            fis.close();
        } catch (Exception e) {
            log.error(e.toString());
        }

        return true;
    }

    private void saveSessionConfig() {
        try {
            File propFile = new File(ctx.getHomeDirectory(), SESSION_PROPERTIES);
            propFile.delete();
            propFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(propFile);
            Properties props = new Properties();
            props.setProperty("host", host);
            props.setProperty("port", String.valueOf(port));
            props.setProperty("user", user);
            props.setProperty("pass", pass);
            props.store(fos, "Session config");
            fos.close();
        } catch (Exception e) {
            log.error(e.toString());
        }
    }

    public void create(KindletContext context) {
        try {
            this.ctx = context;
            this.root = ctx.getRootContainer();

            File logCfg = new File(ctx.getHomeDirectory(), "log4j.properties");
            PropertyConfigurator.configure(logCfg.getPath());
            this.log = Logger.getLogger(TermKindlet.class.getName());

            makeConnectDialog();
            root.add(connectionDlg, BorderLayout.CENTER);
                       
            root.validate();
            root.setVisible(true);
            //ctx.setMenu(createMenu());
//            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
//                public boolean dispatchKeyEvent(final KeyEvent key) {
//                    return false;
//                }
//            });

            

            log.debug("kindlet's create() finished OK");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void destroy() {
        if (session != null)
            session.disconnect();

        if (remoteKeyboard != null)
            remoteKeyboard.kill();

        if (term != null)
            term.kill();
    }

    public void start() {
        super.start();
    }

    public void stop() {
        
    }

    public void statusChanged(boolean isAttached) {
        String title = " [" + host + ']';
        if (isAttached)
            title += " (RKbd)";

        ctx.setSubTitle(title);
    }
}
