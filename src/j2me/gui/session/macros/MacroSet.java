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
package gui.session.macros;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import app.session.MacroSetManager;

/**
 * @author Karl von Randow
 *
 */
public class MacroSet {
	
	private static final byte VERSION = 1;
	
	public String name, value;
	
	public Vector macros;
	
	public MacroSet() {
		macros = new Vector();
	}
	
	public MacroSet(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	/**
	 * @param in
	 * @throws IOException
	 */
	public void read( DataInputStream in ) throws IOException {
		in.readByte(); // a version byte, for future use
		name = in.readUTF();
		
		int n = in.readByte();
		for ( int i = 0; i < n; i++ ) {
			String name = in.readUTF();
			String value = in.readUTF();
			
			MacroSet macro = new MacroSet( name, value );
			macros.addElement( macro );
		}
	}
	/**
	 * @param dout
	 * @throws IOException
	 */
	public void write( DataOutputStream out ) throws IOException {
		out.writeByte( VERSION );
		out.writeUTF( name );
		
		out.writeByte( macros.size() );
		for ( int i = 0; i < macros.size(); i++ ) {
			MacroSet macro = (MacroSet) macros.elementAt( i );
			out.writeUTF( macro.name );
			out.writeUTF( macro.value );
		}
	}
	
	public MacroSet getMacro( int i ) {
		return ( i >= macros.size() ? null : (MacroSet) macros.elementAt( i ));
	}
	
	public void addMacro( MacroSet macro ) {
		macros.addElement( macro );
		MacroSetManager.saveMacroSets();
	}
	
	public void deleteMacro( int i ) {
		if ( i < macros.size() ) {
			macros.removeElementAt( i );
			MacroSetManager.saveMacroSets();
		}
	}
	
	public void replaceMacro( int i, MacroSet macro ) {
		if ( i >= macros.size() ) {
			macros.addElement( macro );
		}
		else {
			macros.setElementAt( macro, i );
		}
		MacroSetManager.saveMacroSets();
	}
}
