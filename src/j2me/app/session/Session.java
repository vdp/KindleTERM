/* This file is part of "MidpSSH".
 * Copyright (c) 2004 Karl von Randow.
 * 
 * MidpSSH is based upon Telnet Floyd and FloydSSH by Radek Polak.
 *
 * --LICENSE NOTICE--
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * --LICENSE NOTICE--
 *
 */
package app.session;

import gui.Activatable;
import gui.MainMenu;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.StreamConnection;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;

import terminal.Terminal;
import terminal.VT320;
import app.SessionSpec;
import app.Settings;

/**
 * @author Karl von Randow
 * 
 */
public abstract class Session implements Activatable {
	/**
	 * After this count of millisends without communication on connection we will
	 * send something just to keep connection alive
	 */
	public static final int keepAliveTime = 1000 * 60 * 1; // 1 minute

	protected VT320 emulation;

	protected SessionIOHandler filter;

	private Terminal terminal;

	private boolean disconnecting, erroredDisconnect;
	
	private boolean forcedDisconnect;
	
	private boolean pollingIO;

	/**
	 * Holds the socket connetion object (from the Generic Connection Framework)
	 * that is the basis of this connection.
	 */
	private StreamConnection connection;

	/**
	 * Holds the InputStream associated with the socket.
	 */
	private InputStream in;

	/**
	 * Holds the OutputStream associated with the socket.
	 */
	private OutputStream out;

	private SessionSpec spec;

//#ifdef readwriteio
    private Thread readWriter;
//#else
	private Thread reader, writer;
//#endif
    
	/**
	 * We will collect here data for writing. Data will be sent when nothing is
	 * in input stream, otherwise midlet hung up.
	 * 
	 * @see #run
	 */
	private byte[] outputBuffer = new byte[16]; // this will grow if needed
	
	private Object writerMutex = new Object();

	/**
	 * Number of bytes to be written, from output array, because it has fixed
	 * lenght.
	 */
	private int outputCount = 0;

	private int bytesWritten = 0, bytesRead = 0;

	public Session() {
		emulation = new VT320() {
			public void sendData( byte[] b, int offset, int length ) throws IOException {
				filter.handleSendData( b, offset, length );
			}
            
//#ifdef midp2
            public void beep() {
                MainMenu.getDisplay().vibrate(200);
            }
//#endif
		};
		pollingIO = Settings.pollingIO;
		terminal = new Terminal( emulation, this );
//#ifdef readwriteio
        readWriter = new ReadWriter();
//#else
		reader = new Reader();
		writer = new Writer();
//#endif
	}

	protected void connect( SessionSpec spec, SessionIOHandler filter ) {
		this.spec = spec;
		this.filter = filter;

//#ifdef readwriteio
        readWriter.start();
//#else
		writer.start();
//#endif
	}

	protected abstract int defaultPort();

	/*
	 * (non-Javadoc)
	 * 
	 * @see telnet.TelnetIOListener#receiveData(byte[])
	 */
	protected void receiveData( byte[] buffer, int offset, int length ) throws IOException {
		if ( buffer != null && length > 0 ) {
			try {
				emulation.putString( new String( buffer, offset, length ) );
			}
			catch ( Exception e ) {

			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see telnet.TelnetIOListener#sendData(byte[])
	 */
	protected void sendData( byte[] b, int offset, int length ) throws IOException {
//#ifndef noiosync
		synchronized ( writerMutex ) {
//#endif
		    if ( outputCount + length > outputBuffer.length ) {
				byte[] newOutput = new byte[outputCount + length];
				System.arraycopy( outputBuffer, 0, newOutput, 0, outputCount );
				outputBuffer = newOutput;
			}
			System.arraycopy( b, offset, outputBuffer, outputCount, length );
			outputCount += length;
			
//#ifndef noiosync
            writerMutex.notify();
		}
//#endif
	}
	
	public void typeString( String str ) {
	    emulation.stringTyped( str );
	}
	
	public void typeChar( char c, int modifiers ) {
		emulation.keyTyped( 0, c, modifiers );
	}
	
	public void typeKey( int keyCode, int modifiers ) {
	    emulation.keyPressed( keyCode, modifiers );
	}
	
	private boolean connect() throws IOException {
        String host = spec.host;
        
		emulation.putString( "Connecting to " + host + "..." );
		
		if ( host.indexOf( ':' ) == -1 ) {
			host += ":" + defaultPort();
		}

		StringBuffer conn;

    	String httpProxy = Settings.httpProxy;
        if (httpProxy.length() == 0 || Settings.httpProxyMode == 0) {
        	conn = new StringBuffer("socket://").append(host);
    		//#ifdef blackberry
    		customiseConnectionString(conn);
    		//#endif
			connection = (StreamConnection) Connector.open( conn.toString(), Connector.READ_WRITE, false );
			in = connection.openDataInputStream();
			out = connection.openDataOutputStream();
        }
        else {
        	/* Connect using HTTP proxy */
        	emulation.putString("\r\nUsing HTTP Proxy...");
        	
        	int id = new Random().nextInt();
        	conn = new StringBuffer("http://").append(httpProxy).append('/').append(id).append('/').append(host);
    		//#ifdef blackberry
    		customiseConnectionString(conn);
    		//#endif
    		
    		if (Settings.httpProxyMode == 1) {
	        	HttpConnection outbound = (HttpConnection) Connector.open(conn.toString(), Connector.READ_WRITE, false);
	    		outbound.setRequestMethod(HttpConnection.POST);
	    		out = outbound.openOutputStream();
	    		HttpConnection inbound = (HttpConnection) Connector.open(conn.toString(), Connector.READ_WRITE, false);
	    		inbound.setRequestProperty("X-MidpSSH-Persistent", "true");
	    		in = inbound.openInputStream();
    		}
    		else {
    			out = new HttpOutboundStream(conn.toString());
    			in = new HttpInboundStream(conn.toString());
    		}
        }
        
		emulation.putString( "OK\r\n" );

		return true;
	}
	
	//#ifdef blackberry
	private void customiseConnectionString(StringBuffer conn) {
		//#ifdef blackberryconntypes
        if ( spec.blackberryConnType == SessionSpec.BLACKBERRY_CONN_TYPE_PROXY ) {
            conn.append(";deviceside=false");
        }
        else if ( spec.blackberryConnType == SessionSpec.BLACKBERRY_CONN_TYPE_DEVICESIDE ) {
            conn.append(";deviceside=true");
        } else if ( spec.blackberryConnType == SessionSpec.BLACKBERRY_CONN_TYPE_WIFI ) {
            conn.append(";DeviceSide=True;ConnectionUID=S TCP-WiFi;ConnectionSetup=delayed;retrynocontext=true");
        }
        //#endif
        //#ifdef blackberryenterprise
        conn.append(";deviceside=false");
        //#endif
	}
	//#endif

//#ifdef readwriteio
    private void readWrite() throws IOException {
        byte [] buf = new byte[512];
        
        while (!disconnecting) {
            boolean doneSomething = false;
            
            /* Check if there is any data to read */
            int a = in.available();
            if (a > 0) {
                /* Read only as much data as is available */
                int n = in.read( buf, 0, Math.min( a, buf.length ) );
                bytesRead += n;
                filter.handleReceiveData( buf, 0, n );
                doneSomething = true;
            }
        
            /* Check if there is any data to write */
            synchronized (writerMutex) {
                if (outputCount > 0) {
                    bytesWritten += outputCount;
                    out.write( outputBuffer, 0, outputCount );
                    out.flush();
                    outputCount = 0;
                    doneSomething = true;
                }
            }
            
            if (!doneSomething) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    
                }
            }
        }
    }
//#else
    /**
	 * Continuously read from remote host and display the data on screen.
	 */
	private void read() throws IOException {
	    byte [] buf;

//#ifndef slowreadio
		buf = new byte[512]; // try a smaller buffer, maybe works better on some phones
		
		int n = 0;
		while ( n != -1 ) {
			bytesRead += n;
		    filter.handleReceiveData( buf, 0, n );
			
//#ifndef io.no_available
			int a = in.available();
            if (pollingIO) {
	            while (a == 0 && !disconnecting) {
	                try {
	                    Thread.sleep(100);
	                }
	                catch (InterruptedException e) {
	                    
	                }
	                a = in.available();
	            }
            }
            
            // Read at least 1 byte, and at most the number of bytes available
			n = in.read( buf, 0, Math.max( 1, Math.min( a, buf.length ) ) );
//#else
            n = in.read(buf, 0, buf.length);
//#endif
		}
//#else
	    buf = new byte[1];
//#ifdef debugging
		emulation.putString( "Waiting for first byte.\r\n" );
//#endif
	    
	    int c = in.read();
//#ifdef debugging
		emulation.putString( "Read first byte.\r\n" );
//#endif
	    while ( c != -1 ) {
	        bytesRead++;
	        buf[0] = (byte) ( c & 0xff );
		    filter.handleReceiveData( buf, 0, 1 );
			c = in.read();
	    }
//#endif
	}
	
	private void write() throws IOException {
		final byte [] empty = new byte[0];
		
		while ( !disconnecting ) {
//#ifndef noiosync
			synchronized ( writerMutex ) {
//#else
			    int sleepCount = 0;
//#endif
			    while ( outputCount == 0 && !disconnecting ) {
					try {
//#ifndef noiosync
					    writerMutex.wait( keepAliveTime );
//#else
					    Thread.sleep(100);
                        if (sleepCount++ < 600) {
                            continue; // to avoid the keep-alive send below
                        }
                        else {
                            sleepCount = 0; // and go ahead and send keep-alive
                        }
//#endif
					}
					catch ( InterruptedException e ) {
					}
					if ( outputCount == 0 && !disconnecting ) {
						// No data to send after timeout so send an empty array through the filter which will trigger the
						// sending of a NOOP (see TelnetSession and SshSession) - this has the effect of a keepalive
						//emulation.putString( "NOOP\r\n" );
					    filter.handleSendData( empty, 0, 0 );
					}
				}
				
				if ( !disconnecting ) {
				    //#ifdef noiosync
					/* If we aren't syncing I/O then we should make a copy of relevant data structures so that
					 * we don't stumble if they are changed while we're sending, this is quite likely to happen
					 * as sending can be a slow operation.
					 */
				    byte [] outputBuffer = new byte[ outputCount ];
				    int outputCount = this.outputCount;
				    this.outputCount = 0;
				    System.arraycopy( this.outputBuffer, 0, outputBuffer, 0, outputCount );
				    //#endif
				    
					bytesWritten += outputCount;
					out.write( outputBuffer, 0, outputCount );
					out.flush();
					outputCount = 0;
				}
//#ifndef noiosync
			}
//#endif
		}
	}
//#endif
    
	private void handleException( String where, Throwable t ) {
		if ( !disconnecting ) {
			Alert alert = new Alert( "Session Error" );
			alert.setType( AlertType.ERROR );
			alert.setTimeout(Alert.FOREVER);

			String msg = t.getMessage();
			if ( msg == null )
				msg = t.toString();

			alert.setString( where + ": " + msg );
			MainMenu.setDisplay( alert );
		}
	}

	/**
	 * @return Returns the terminal.
	 */
	public Terminal getTerminal() {
		return terminal;
	}

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
		if ( !disconnecting ) {
//#ifndef noiosync
			synchronized ( writerMutex ) {
//#endif
				disconnecting = true;
				try {
					if ( in != null ) in.close();
					if ( out != null ) out.close();
					if ( connection != null ) connection.close();
				}
				catch ( IOException e ) {
					handleException( "Disconnect", e );
				}

//#ifndef noiosync
				writerMutex.notify();
			}
//#endif
		}
	}
	
	private String bytesToString( int bytes ) {
		if ( bytes < 1024 ) {
			return bytes + " bytes";
		}
		else if ( bytes < 1024 * 1024 ) {
			return to2dp( bytes * 100 / 1024 ) + " KB";
		}
		else {
			return to2dp( bytes * 100 / ( 1024 * 1024 ) ) + " MB";
		}
	}
	
	private String to2dp( int i ) {
		String str = "" + i;
		return str.substring( 0, str.length() - 2 ) + "." + str.substring( str.length() - 2 );
	}
	
	private void sessionReport() {
        if ( !erroredDisconnect ) {
    		String report = 
    			"IN: " + bytesToString( bytesRead ) + "\nOUT: " +
    			bytesToString( bytesWritten ) + "\nTOTAL: " +
    			bytesToString( bytesRead + bytesWritten );
    		Alert alert = new Alert( "Session Report" );
    		alert.setType( AlertType.INFO );
    	
    		alert.setString( report );
    		alert.setTimeout( Alert.FOREVER );
    		
    		if ( forcedDisconnect ) {
    		    MainMenu.alertBackToMain( alert );
    		}
    		else {
    		    MainMenu.alert( alert, terminal );
    		}
        }
	}
	
	public void goMainMenu() {
		MainMenu.goMainMenu();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gui.Activatable#activate()
	 */
	public void activate() {
		terminal.activate();
	}
	
	public void activate( Activatable back ) {
		activate();
	}
    
//#ifdef readwriteio
    private class ReadWriter extends Thread {
        public void run() {
            try {
                connect();
                terminal.connected();
                readWrite();
                
                doDisconnect();
                terminal.disconnected();
                sessionReport();
            }
            catch ( Exception e ) {
                handleException( "ReadWriter", e );
                erroredDisconnect = true;
                doDisconnect();
                terminal.disconnected();
            }
        }
    }
//#else
	private class Reader extends Thread {
		public void run() {
			try {
//#ifdef debugging
				emulation.putString( "Reader started.\r\n" );
//#endif
				read();
				doDisconnect();
			}
			catch ( Exception e ) {
				handleException( "Reader", e );
                erroredDisconnect = true;
				doDisconnect();
			}
		}
	}
	
	private class Writer extends Thread {
		public void run() {
			try {
				connect();
				terminal.connected();
				reader.start();
				write();
				
				doDisconnect();
				terminal.disconnected();
				sessionReport();
			}
			catch ( Exception e ) {
				handleException( "Writer", e );
                erroredDisconnect = true;
				doDisconnect();
				terminal.disconnected();
			}
		}
	}
//#endif
}