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

import java.io.IOException;

import telnet.Dimension;
import telnet.TelnetProtocolHandler;
import app.SessionSpec;
import app.Settings;

/**
 * @author Karl von Randow
 * 
 */
public class TelnetSession extends Session implements SessionIOHandler {
	public TelnetSession() {
		super();
		
		// Default to true in telnet and then turn off when told to
		emulation.setLocalEcho( true );
	}
	
	public void connect( SessionSpec spec ) {
        telnet = new TelnetProtocolHandler() {
            /** get the current terminal type */
            public String getTerminalType() {
                if ( Settings.terminalType.length() > 0 ) {
                    return Settings.terminalType;
                }
                else {
                    return emulation.getTerminalID();
                }
            }

            /** get the current window size */
            public Dimension getWindowSize() {
                return new Dimension( emulation.width, emulation.height );
            }

            /** notify about local echo */
            public void setLocalEcho( boolean echo ) {
                emulation.localecho = echo;
            }

            /** notify about EOR end of record */
            public void notifyEndOfRecord() {
                // only used when EOR needed, like for line mode
            }

            /** write data to our back end */
            public void write( byte[] b ) throws IOException {
                /*for ( int i = 0; i < b.length; i++ ) {
                    System.out.println( "SEND " + b[i] + "=" + (char) b[i] );
                }*/
                TelnetSession.this.sendData( b, 0, b.length );
            }
        };
        
		super.connect( spec, this );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see telnet.Session#defaultPort()
	 */
	protected int defaultPort() {
		return 23;
	}

	private TelnetProtocolHandler telnet;

	/*
	 * (non-Javadoc)
	 * 
	 * @see terminal.TerminalIOListener#receiveData(byte[])
	 */
	public void handleReceiveData( byte[] data, int offset, int length ) throws IOException {
		telnet.inputfeed( data, offset, length );
		int n;
		do {
			n = telnet.negotiate( data, offset, length );
			if ( n > 0 ) {
				/*for ( int i = offset; i < offset + n; i++ ) {
					System.out.println( "RECV " + data[i] + "=" + (char) data[i] );
				}*/
				TelnetSession.this.receiveData( data, offset, n );
			}
		}
		while ( n != -1 );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see terminal.TerminalIOListener#sendData(byte[])
	 */
	public void handleSendData( byte[] data, int offset, int length ) throws IOException {
		if ( length > 0 ) {
			telnet.transpose( data, offset, length );
		}
		else {
			telnet.sendTelnetNOP();
		}
	}
}