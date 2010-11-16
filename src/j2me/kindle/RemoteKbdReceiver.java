/*
 * ProxyKbdReceiver.java
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

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import org.apache.log4j.Logger;

/**
 * Listens on a UDP socket for keyboard events
 * and forwards them to the terminal window.
 */
public class RemoteKbdReceiver extends Thread {

    private boolean stopping;
    //private DatagramSocket socket;
    //private DatagramPacket packet;
    private ServerSocket sockServ;
    private Socket sock;
    byte[] readBuf = new byte[1024];
    private Logger log;
    private KeyListener keyListener;
    private Component receiver;
    private Thread watchdog;
    private RKbdStatusListener rkbdListener;

    public RemoteKbdReceiver(int port, KeyListener listener, Component receiver) {
        stopping = false;
        log = Logger.getLogger(RemoteKbdReceiver.class.getName());
        this.keyListener = listener;
        this.receiver = receiver;
        try {
            sockServ = new ServerSocket(port);
        } catch (Exception ex) {
            log.error(ex.toString());
        }

        watchdog = new Thread(new Runnable() {

            public void run() {
                long sleepTime = 5 * 1000;
                while (!stopping) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ie) {
                        log.debug("Watchdog thread interrupted");
                    }

                    try {
                        if (sock != null)
                            sock.getOutputStream().write(1);
                    } catch (Exception e) {
                        if (sock != null) {
                            try {
                                sock.close();
                                sock = null;
                            } catch (IOException ex) {
                                log.warn(ex.toString());
                            }
                        }
                        log.debug("Remote keyboard probably disconnected ...");
                    }
                }
            }
        });

        watchdog.start();
    }

    public void kill() {
        try {
            stopping = true;
            if (sockServ != null)
                sockServ.close();

            if (sock != null && !sock.isClosed()) {
                sock.close();
            }

            watchdog.interrupt();
        } catch (IOException ex) {
            log.error(ex.getMessage());
        }
    }

    public void run() {
        try {
            while (!stopping) {
                if (sock == null) {
                    log.info("Remote keyboard disconnected");
                    if (rkbdListener != null) {
                        rkbdListener.statusChanged(false);
                    }

                    sock = sockServ.accept();
                    
                    log.info("New remote keyboard attached");
                    if (rkbdListener != null) {
                        rkbdListener.statusChanged(true);
                    }
                }

                int i = 0;
                try {
                    int b = sock.getInputStream().read();
                while (b != 0) {
                    readBuf[i++] = (byte) b;
                    b = sock.getInputStream().read();
                }
                }
                catch (SocketException se) {
                    log.info("RKeyboard read() interrupted: " + se.toString());
                    sock = null;
                }
                catch (NullPointerException npe) {
                    log.debug(npe.toString());
                }
                
                String strEvent = new String(readBuf, 0, i);
                System.out.println("Received: " + strEvent);
                ProxyKeyEvent pe = ProxyKeyEvent.makeEvent(strEvent, receiver, 0);
                int id = pe.getID();
                if (id == KeyEvent.KEY_PRESSED) {
                    keyListener.keyPressed(pe);
                }
                else if (id == KeyEvent.KEY_RELEASED) {
                    keyListener.keyReleased(pe);
                }
                else {
                    keyListener.keyTyped(pe);
                }
            }
        } catch (IOException ex) {
            if (!stopping) {
                log.error(ex.toString());
            }
        }
    }

    public void setRKbdStatusListener(RKbdStatusListener rkbdListener) {
        this.rkbdListener = rkbdListener;
    }

    /**
     * The objects interested whether a remote keyboard is attached
     * should implement this interface and register it with RemoteKbdReceiver
     */
    public interface RKbdStatusListener {
        public void statusChanged(boolean isAttached);
    }
}
