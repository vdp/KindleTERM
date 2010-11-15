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
package gui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;

/**
 * @author Karl von Randow
 */
public abstract class EditableForm extends Form implements CommandListener, Activatable {

	private Activatable back;

	public EditableForm( String title ) {
		super( title );

		addCommand( MainMenu.backCommand );
		setCommandListener( this );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.lcdui.CommandListener#commandAction(javax.microedition.lcdui.Command,
	 *      javax.microedition.lcdui.Displayable)
	 */
	public void commandAction( Command command, Displayable displayed ) {
		if ( command == MainMenu.backCommand ) {
			doBack();
		} else if (command == MainMenu.okCommand) {
			doOK();
		}
	}
	
	protected void addOKCommand() {
		addCommand(MainMenu.okCommand);
	}

	protected void doBack() {
		back.activate();
	}
	
	protected void doOK() {
		back.activate();
	}
	
	/* (non-Javadoc)
	 * @see gui.Activatable#activate()
	 */
	public void activate() {
		MainMenu.setDisplay( this );
	}
	/* (non-Javadoc)
	 * @see gui.Activatable#activate(gui.Activatable)
	 */
	public void activate( Activatable back ) {
		this.back = back;
		activate();
	}
}
