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
import javax.microedition.lcdui.List;

/**
 * @author Karl von Randow
 *
 */
public abstract class EditableMenu extends List implements CommandListener, Activatable {

	protected static Command newCommand = new Command( "New", Command.SCREEN, 8 );

	protected static Command editCommand = new Command( "Edit", Command.ITEM, 9 );

	protected static Command deleteCommand = new Command( "Delete", Command.ITEM, 10 );

	protected Command selectCommand = List.SELECT_COMMAND;
	
	private Activatable back;

	public EditableMenu( String title ) {
		super( title, List.IMPLICIT );
		
		//addCommand( selectCommand );
		addCommand( newCommand );
		addCommand( editCommand );
		addCommand( deleteCommand );
		addCommand( MainMenu.backCommand );

		setCommandListener( this );
	}
	
	protected void replaceSelectCommand( Command selectCommand ) {
		//removeCommand( this.selectCommand );
		this.selectCommand = selectCommand;
		
		//#ifdef midp2
	    super.setSelectCommand( selectCommand );
	    //#else
	    //#ifndef blackberry
	    // On the blackberry we don't require the command to be added as the implicit command is also listed in
	    // the menu.
	    addCommand( selectCommand );
	    //#endif
	    //#endif
	}
	
	//#ifndef midp2
	public void deleteAll() {
		while ( size() > 0 ) {
			delete(0);
		}
	}
	//#endif
	
	protected abstract void addItems();
	
	/* (non-Javadoc)
	 * @see javax.microedition.lcdui.CommandListener#commandAction(javax.microedition.lcdui.Command, javax.microedition.lcdui.Displayable)
	 */
	public void commandAction( Command command, Displayable displayable ) {
		if ( command == List.SELECT_COMMAND || command == selectCommand ) {
			int i = getSelectedIndex();
			if ( i >= 0 && i < size() ) {
				doSelect( getSelectedIndex() );
			}
		}
		else if ( command == newCommand ) {
			doNew();
		}
		else if ( command == editCommand ) {
			int i = getSelectedIndex();
			if ( i >= 0 && i < size() ) {
				doEdit( i );
			}
		}
		else if ( command == deleteCommand ) {
			int i = getSelectedIndex();
			if ( i >= 0 && i < size() ) {
				doDelete( i );
			}
		}
		else if ( command == MainMenu.backCommand ) {
			doBack();
		}
	}
	
	/* (non-Javadoc)
	 * @see app.Activatable#activate()
	 */
	public void activate() {
		addItems();
		MainMenu.setDisplay( this );
	}
	
	public void activate( Activatable back ) {
		this.back = back;
		activate();
	}

	protected abstract void doSelect( int i );

	protected abstract void doEdit( int i );

	protected abstract void doDelete( int i );

	protected abstract void doNew();

	protected void doBack() {
		back.activate();
	}
}
