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

import gui.MainMenu;

import javax.microedition.midlet.MIDlet;

public class Main extends MIDlet {

	public static Main instance;
	
	private static MainMenu mainMenu;
    
    private static boolean paused;
	
	/** Constructor */
	public Main() {
		Main.instance = this;
		mainMenu = new MainMenu();
		Settings.init();
	}

	/** Main method */
	protected void startApp() {
        if ( !paused ) {
            mainMenu.activate();
        }
        paused = false;
	}

	/** Handle pausing the MIDlet */
	protected void pauseApp() {
        paused = true;
	}

	/** Handle destroying the MIDlet */
	protected void destroyApp( boolean unconditional ) {
        paused = false;
	}

	/** Quit the MIDlet */
	public static void quitApp() {
		instance.destroyApp( true );
		instance.notifyDestroyed();
		instance = null;
	}
    
    /*
    public static void debug(String msg) {
        Alert alert = new Alert("DEBUG");
        alert.setString(msg);
        alert.setTimeout(Alert.FOREVER);
        alert(alert, getDisplay().getCurrent());
    }
    */
}