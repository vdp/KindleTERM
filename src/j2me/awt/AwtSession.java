/*
 * AwtSession.java
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

import app.session.SessionIOHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import terminal.VT320;

/**
 * Reimplements {@link app.session.Session} using J2SE(not J2ME) means.
 * 
 * @author VDP <vdp DOT kindle AT gmail.com>
 */
public abstract class AwtSession {

    /**
     * After this count of millisends without communication on connection we will
     * send something just to keep connection alive
     */
    public static final int keepAliveTime = 1000 * 60 * 1; // 1 minute
    protected VT320 emulation;
    protected SessionIOHandler filter;
    //private AwtTerminal terminal;
    private boolean disconnecting, erroredDisconnect;
    private boolean forcedDisconnect;
    private boolean pollingIO;
    /**
     * Holds the socket connetion object (from the Generic Connection Framework)
     * that is the basis of this connection.
     */
    private Socket connection;
    /**
     * Holds the InputStream associated with the socket.
     */
    private InputStream in;
    /**
     * Holds the OutputStream associated with the socket.
     */
    private OutputStream out;
    //private SessionSpec spec;
    protected String host, username, password;
    protected int port;
    protected boolean usePublicKey = false;
    
    private Thread reader, writer;
     /**
     * We will collect here data for writing. Data will be sent when nothing is
     * in input stream, otherwise midlet hung up.
     *
     * @see #run
     */
    private byte[] outputBuffer = new byte[16]; // this will grow if needed
    private final Object writerMutex = new Object();
    /**
     * Number of bytes to be written, from output array, because it has fixed
     * lenght.
     */
    private int outputCount = 0;
    private int bytesWritten = 0, bytesRead = 0;

    private Logger log;

    public AwtSession() {
        log = Logger.getLogger(AwtSession.class.getName());
        emulation = new VT320() {

            public void sendData(byte[] b, int offset, int length) throws IOException {
                filter.handleSendData(b, offset, length);
            }

            public void beep() {
                //MainMenu.getDisplay().vibrate(200);
            }
        };
        pollingIO = true; //Settings.pollingIO;
        //terminal = new AwtTerminal(emulation, this);
        reader = new Reader();
        writer = new Writer();
    }

    protected void connect(String host, int port ,
            String username, String password,
            SessionIOHandler filter) {

        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;

        this.filter = filter;

        writer.start();
    }

    protected abstract int defaultPort();

    /*
     * (non-Javadoc)
     *
     * @see telnet.TelnetIOListener#receiveData(byte[])
     */
    protected void receiveData(byte[] buffer, int offset, int length) throws IOException {
        if (buffer != null && length > 0) {
            try {
                emulation.putString(new String(buffer, offset, length));
            } catch (Exception e) {
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see telnet.TelnetIOListener#sendData(byte[])
     */
    protected void sendData(byte[] b, int offset, int length) throws IOException {
        synchronized (writerMutex) {
            if (outputCount + length > outputBuffer.length) {
                byte[] newOutput = new byte[outputCount + length];
                System.arraycopy(outputBuffer, 0, newOutput, 0, outputCount);
                outputBuffer = newOutput;
            }
            System.arraycopy(b, offset, outputBuffer, outputCount, length);
            outputCount += length;

            writerMutex.notify();
        }
    }

    public void typeString(String str) {
        emulation.stringTyped(str);
    }

    public void typeChar(char c, int modifiers) {
        emulation.keyTyped(0, c, modifiers);
    }

    public void typeKey(int keyCode, int modifiers) {
        emulation.keyPressed(keyCode, modifiers);
    }

    private boolean connect() throws IOException {

        emulation.putString("Connecting to " + host + ':' + port + " ...");

        connection = new Socket(host, port); //(StreamConnection) Connector.open(conn.toString(), Connector.READ_WRITE, false);
        log.debug("Socket timeout is " + connection.getSoTimeout());
        connection.setSoTimeout(0);
        in = connection.getInputStream(); //openDataInputStream();
        out = connection.getOutputStream(); //openDataOutputStream();
        
        emulation.putString("OK\r\n");

        return true;
    }

    /**
     * Continuously read from remote host and display the data on screen.
     */
    private void read() throws IOException {
        byte[] buf;

        buf = new byte[512]; // try a smaller buffer, maybe works better on some phones

        int n = 0;
        while (n != -1) {
            bytesRead += n;
            filter.handleReceiveData(buf, 0, n);

            int a = in.available();
            if (pollingIO) {
                while (a == 0 && !disconnecting) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                    a = in.available();
                }
            }

            // Read at least 1 byte, and at most the number of bytes available
            n = in.read(buf, 0, Math.max(1, Math.min(a, buf.length)));
        }
    }

    private void write() throws IOException {
        final byte[] empty = new byte[0];

        while (!disconnecting) {
            synchronized (writerMutex) {
                while (outputCount == 0 && !disconnecting) {
                    try {
                        writerMutex.wait(keepAliveTime);
                    } catch (InterruptedException e) {
                    }
                    if (outputCount == 0 && !disconnecting) {
                        // No data to send after timeout so send an empty array through the filter which will trigger the
                        // sending of a NOOP (see TelnetSession and SshSession) - this has the effect of a keepalive
                        //emulation.putString( "NOOP\r\n" );
                        filter.handleSendData(empty, 0, 0);
                    }
                }

                if (!disconnecting) {
                    bytesWritten += outputCount;
                    out.write(outputBuffer, 0, outputCount);
                    out.flush();
                    outputCount = 0;
                }
            }
        }
    }

    private void handleException(String where, Throwable t) {
        if (!disconnecting) {
            log.error("Exception in " + where + ": " + t.toString());
            StackTraceElement[] stack = t.getStackTrace();
            for (int i = 0; i < stack.length; i++)
                log.error(stack[i].toString());
        }
        else {
            log.debug("Exception in " + where + ": " + t.getMessage());
        }
    }

    public VT320 getEmulation() {
        return emulation;
    }

    /**
     * @return Returns the terminal.
     */
//    public AwtTerminal getTerminal() {
//        return terminal;
//    }

    /*
     * (non-Javadoc)
     *
     * @see telnet.Session#disconnect()
     */
    public void disconnect() {
        forcedDisconnect = true;
        doDisconnect();
    }

    private void doDisconnect() {
        if (!disconnecting) {
            synchronized (writerMutex) {
                disconnecting = true;
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                    if (connection != null) {
                        connection.close();
                    }
                } catch (IOException e) {
                    handleException("Disconnect", e);
                }

                writerMutex.notify();
            }

        }
    }

    private String bytesToString(int bytes) {
        if (bytes < 1024) {
            return bytes + " bytes";
        } else if (bytes < 1024 * 1024) {
            return to2dp(bytes * 100 / 1024) + " KB";
        } else {
            return to2dp(bytes * 100 / (1024 * 1024)) + " MB";
        }
    }

    private String to2dp(int i) {
        String str = "" + i;
        return str.substring(0, str.length() - 2) + "." + str.substring(str.length() - 2);
    }

    private class Reader extends Thread {

        public void run() {
            try {
                log.debug("Reader thread started");
                emulation.putString("Reader started.\r\n");
                read();
                doDisconnect();
                log.debug("Reader thread exited normally");
            } catch (Exception e) {
                handleException("Reader", e);
                erroredDisconnect = true;
                //doDisconnect();
            }
        }
    }

    private class Writer extends Thread {

        public void run() {
            try {
                log.debug("Writer thread started");
                connect();
                reader.start();
                emulation.putString("Writer started \r\n");
                write();
                doDisconnect();
                log.debug("Writer thread exited normally");
            } catch (Exception e) {
                handleException("Writer", e);
                erroredDisconnect = true;
                //doDisconnect();
            }
        }
    }
}
