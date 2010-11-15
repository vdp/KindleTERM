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
package app;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Vector;

/**
 * @author Karl von Randow
 * 
 */
public class SessionManager extends MyRecordStore {
	
	private static final String RMS_NAME = "sessions";
	
	private static Vector sessions;
	
	private static SessionManager me = new SessionManager();

    protected int compare(Object a, Object b) {
        return ((SessionSpec) a).alias.compareTo(((SessionSpec) b).alias);
    }
    
	public static Vector getSessions() {
		if ( sessions == null ) {
			SessionManager.sessions = me.load( RMS_NAME, true );
		}
		return sessions;
	}

	private static void saveSessions() {
		me.save( RMS_NAME, sessions );
	}

	/**
	 * @param conn
	 */
	public static void addSession( SessionSpec conn ) {
		Vector connections = getSessions();
		connections.addElement( conn );
		saveSessions();
	}

	/**
	 * @param i
	 * @return
	 */
	public static SessionSpec getSession( int i ) {
		Vector connections = getSessions();
		return (i >= connections.size() ? null : (SessionSpec) connections.elementAt( i ));
	}

	/**
	 * @param i
	 */
	public static void deleteSession( int i ) {
		Vector connections = getSessions();
		if (i < connections.size()) {
			connections.removeElementAt( i );
			saveSessions();
		}
	}

	/**
	 * @param connectionIndex
	 * @param conn
	 */
	public static void replaceSession( int i, SessionSpec conn ) {
		Vector connections = getSessions();
		if ( i >= connections.size() ) {
			connections.addElement( conn );
		}
		else {
			connections.setElementAt( conn, i );
		}
		saveSessions();
	}
    /* (non-Javadoc)
     * @see app.MyRecordStore#read(java.io.DataInputStream)
     */
    protected Object read(DataInputStream in) throws IOException {
        SessionSpec spec = new SessionSpec();
        try {
        	spec.read( in );
        }
        catch (EOFException e) {
        	/* ignore EOFExceptions as they result from us adding more fields to the end of the spec */
        }
        return spec;
    }
    /* (non-Javadoc)
     * @see app.MyRecordStore#write(java.io.DataOutputStream, java.lang.Object)
     */
    protected void write(DataOutputStream out, Object ob) throws IOException {
        SessionSpec spec = (SessionSpec) ob;
        spec.write( out );
    }
    
}