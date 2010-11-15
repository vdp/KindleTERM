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

import gui.session.macros.MacroSet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import app.MyRecordStore;

/**
 * @author Karl von Randow
 *
 */
public class MacroSetManager extends MyRecordStore {
	
	private static final String RMS_NAME = "macros";
	
	private static Vector macroSets;
	
	private static MacroSetManager me = new MacroSetManager();

    protected int compare(Object a, Object b) {
        MacroSet aa = (MacroSet) a;
        MacroSet bb = (MacroSet) b;
        return aa.name.compareTo(bb.name);
    }
	
	public static Vector getMacroSets() {
		if ( macroSets == null ) {
			MacroSetManager.macroSets = me.load( RMS_NAME, true );
            if ( macroSets.isEmpty() ) {
                MacroSet macroSet = new MacroSet();
                macroSet.name = "Untitled Set";
                macroSets.addElement( macroSet );
                saveMacroSets();
            }
		}
		return macroSets;
	}

	public static void saveMacroSets() {
		me.save( RMS_NAME, macroSets );
	}

	/**
	 * @param i
	 * @return
	 */
	public static MacroSet getMacroSet( int i ) {
		Vector macroSets = getMacroSets();
		return ( macroSets == null || i >= macroSets.size() ? null : (MacroSet) macroSets.elementAt( i ));
	}

	/**
	 * @param conn
	 */
	public static void addMacroSet( MacroSet macroSet ) {
		Vector macroSets = getMacroSets();
		macroSets.addElement( macroSet );
		saveMacroSets();
	}

	/**
	 * @param i
	 */
	public static void deleteMacroSet( int i ) {
		Vector macroSets = getMacroSets();
		if (i < macroSets.size() ) {
			macroSets.removeElementAt( i );
			saveMacroSets();
		}
	}

	/**
	 * @param connectionIndex
	 * @param conn
	 */
	public static void replaceMacroSet( int i, MacroSet macroSet ) {
		Vector macroSets = getMacroSets();
		if ( i >= macroSets.size() ) {
			macroSets.addElement( macroSet );
		}
		else {
			macroSets.setElementAt( macroSet, i );
		}
		saveMacroSets();
	}
    /* (non-Javadoc)
     * @see app.MyRecordStore#read(java.io.DataInputStream)
     */
    protected Object read(DataInputStream in) throws IOException {
        MacroSet macroSet = new MacroSet();
        macroSet.read( in );
        return macroSet;
    }
    /* (non-Javadoc)
     * @see app.MyRecordStore#write(java.io.DataOutputStream, java.lang.Object)
     */
    protected void write(DataOutputStream out, Object ob) throws IOException {
        MacroSet macroSet = (MacroSet) ob;
        macroSet.write( out );
    }
}
