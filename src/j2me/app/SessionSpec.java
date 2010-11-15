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
import java.io.IOException;

/**
 * @author Karl von Randow
 * 
 */
public class SessionSpec {
	public static final String TYPE_SSH = "ssh";

	public static final String TYPE_TELNET = "telnet";
	
	public String alias, type, host, username, password;
	
	public boolean usepublickey;
    
//#ifdef blackberryconntypes
    public static final int BLACKBERRY_CONN_TYPE_DEFAULT = 0;
    public static final int BLACKBERRY_CONN_TYPE_DEVICESIDE = 1;
    public static final int BLACKBERRY_CONN_TYPE_PROXY = 2;
    public static final int BLACKBERRY_CONN_TYPE_WIFI = 3;
    
    public int blackberryConnType;
//#endif
	
	public void read( DataInputStream in ) throws IOException {
		alias = in.readUTF();
		type = in.readUTF();
		host = in.readUTF();
		username = in.readUTF();
		password = in.readUTF();
//#ifdef blackberryconntypes
        blackberryConnType = in.readInt();
//#endif
    	usepublickey = in.readBoolean();
	}

	public void write( DataOutputStream out ) throws IOException {
		out.writeUTF( alias );
		out.writeUTF( type );
		out.writeUTF( host );
		out.writeUTF( username );
		out.writeUTF( password );
//#ifdef blackberryconntypes
        out.writeInt(blackberryConnType);
//#endif
        out.writeBoolean(usepublickey);
	}
}