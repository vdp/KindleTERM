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

import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import app.SessionManager;
import app.SessionSpec;
import app.session.SshSession;
import app.session.TelnetSession;

/**
 * @author Karl von Randow
 * 
 */
public class SessionsMenu extends EditableMenu {

	private static Command connectCommand = new Command( "Connect", Command.ITEM, 1 );
	
	private static SessionForm newConnectionForm = new SessionForm( false );

	private static SessionForm editConnectionForm = new SessionForm( true );
    
//#ifndef nosessionimport
    private static Command importCommand = new Command("Import", Command.SCREEN, 20);
    
    private ImportSessionsForm importSessionsForm;
//#endif
    
    private Form authenticationDialog;
    
    private TextField usernameField, passwordField;
    
    private SessionSpec conn;
    
	public SessionsMenu() {
		super( "Sessions" );
		replaceSelectCommand( connectCommand );
        
//#ifndef nosessionimport
        addCommand(importCommand);
//#endif
	}
    
    public void commandAction(Command command, Displayable displayable) {
    	if (displayable == authenticationDialog) {
    		if (command == MainMenu.okCommand) {
	    		SshSession session = new SshSession();
	    		session.connect(conn, usernameField.getString(), passwordField.getString());
	            MainMenu.openSession( session );
    		}
    		else {
    			activate();
    		}
    	}
    	else {
    		//#ifndef nosessionimport
	        if (command == importCommand) {
	            /* Import */
	            if (importSessionsForm == null) {
	                importSessionsForm = new ImportSessionsForm();
	            }
	            importSessionsForm.activate(this);
	        }
	        else
	        	//#endif
	        {
	            super.commandAction(command, displayable);
	        }
    	}
    }

    
	protected void addItems() {
		deleteAll();

		Vector connections = SessionManager.getSessions();
		if ( connections != null ) {
			for ( int i = 0; i < connections.size(); i++ ) {
				SessionSpec conn = (SessionSpec) connections.elementAt( i );
				append( conn.alias, null );
			}
		}
	}

	protected void doSelect( int i ) {
		if ( i != -1 ) {
			conn = SessionManager.getSession( i );
			if ( conn != null ) {
//#ifndef nossh
				if ( conn.type.equals( SessionSpec.TYPE_SSH ) ) {
					String username = conn.username;
                    String password = conn.password;
                    
                    if (username.length() == 0 || (password.length() == 0 && !conn.usepublickey)) {
                    	authenticationDialog = new Form("Authentication");
                    	usernameField = new TextField("Username:", username, 255, TextField.ANY);
                    	authenticationDialog.append(usernameField);
                    	if (!conn.usepublickey) {
                    		passwordField = new TextField("Password:", password, 255, TextField.PASSWORD);
                        	authenticationDialog.append(passwordField);
                    	}
                    	authenticationDialog.addCommand(MainMenu.okCommand);
                    	authenticationDialog.addCommand(MainMenu.backCommand);
                    	authenticationDialog.setCommandListener(this);
                    	MainMenu.setDisplay(authenticationDialog);
                    }
                    else {
                    	SshSession session = new SshSession();
    					session.connect( conn, null, null );
    					MainMenu.openSession( session );
                    }
				}
//#endif
//#ifndef notelnet
				if ( conn.type.equals( SessionSpec.TYPE_TELNET ) ) {
					TelnetSession session = new TelnetSession();
					session.connect( conn );
					MainMenu.openSession( session );
				}
//#endif
			}
		}
	}

	protected void doEdit( int i ) {
		if ( i != -1 ) {
			editConnectionForm.setConnectionIndex( i );
			editConnectionForm.activate( this );
		}
	}

	protected void doDelete( int i ) {
		if ( i != -1 ) {
			SessionManager.deleteSession( i );
			delete( i );
		}
	}

	protected void doNew() {
		newConnectionForm.activate( this );
	}
}