/*
 * RemoteKeyboard.java
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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Captures the key events on a host machine and relays them
 * to the Kindle in plain text format.
 *
 * @author VDP <vdp DOT kindle AT gmail.com>.
 */
public class RemoteKeyboard implements KeyListener {

    public static void main(String[] args) {
//        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().
//                getDefaultScreenDevice();
//        if (device.isFullScreenSupported()) {
//            System.out.println("Fullscreen supported");
//            device.setFullScreenWindow(frame);
//        }
//        else {
//            System.err.println("Full screen not supported");
//        }
        FileInputStream fis = null;
        try {
            final Frame frame = new Frame("Type here");
            frame.setPreferredSize(new Dimension(800, 600));
            frame.setSize(new Dimension(800, 600));

            fis = new FileInputStream("remote_keyboard.properties");
            Properties props = new Properties();
            props.load(fis);
            String host = props.getProperty("host");
            int port = Integer.parseInt(props.getProperty("port"));

            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    frame.dispose();
                    System.exit(0);
                }
            });

            RemoteKeyboard rk = new RemoteKeyboard(host, port);
            frame.addKeyListener(rk);
            frame.setFocusTraversalKeysEnabled(false);
            frame.setVisible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private String host;
    private int port;

    private Socket socket;
    private OutputStream out;


    public RemoteKeyboard(String targetHost, int targetPort) {
        this.host = targetHost;
        this.port= targetPort;

        connect();
        Thread reader = new Thread(new Runnable() {

            public void run() {
                while (true) {
                    try {
                        if (socket != null) {
                            socket.getInputStream().read();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        });
    }

    private boolean connect() {
        boolean ok = true;
        try {
            System.out.println("Connecting to " + host + ':' + port + " ...");
            socket = new Socket(host, port);
            out = socket.getOutputStream();
            System.out.println("OK");
        } catch (Exception ex) {
            System.out.println("Failed to connect");
            ok = false;
        }

        return ok;
    }

    private void sendEvent(KeyEvent event) {
        try {
            ProxyKeyEvent pe = new ProxyKeyEvent(event);
            System.out.println("Key Event: " + pe);
            //ProxyKeyEvent reconstructed = ProxyKeyEvent.makeEvent(pe.toString(), event.getComponent(), 0);
            //System.out.println("Reconstructed: " + reconstructed);
            byte[] eventBytes = pe.toString().getBytes();
            try {
                out.write(eventBytes);
                out.write(0);
                out.flush();
            }
            catch (Exception ioe) {
                if (socket != null) socket.close();
                connect();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void keyTyped(KeyEvent ke) {
        sendEvent(ke);
    }

    public void keyPressed(KeyEvent ke) {
        sendEvent(ke);
    }

    public void keyReleased(KeyEvent ke) {
        sendEvent(ke);
    }
}
