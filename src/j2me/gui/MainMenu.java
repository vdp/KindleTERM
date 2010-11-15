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

import gui.session.macros.MacrosMenu;
import gui.settings.SettingsMenu;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;

import app.Main;
import app.session.Session;

/**
 * @author Karl von Randow
 */
public class MainMenu extends List implements CommandListener, Activatable {

	public static final Command okCommand = new Command( "OK", Command.OK, 1 );
    
    public static final Command backCommand = new Command( "Back", Command.BACK, 99 );
    
	private static Command quitCommand = new Command( "Quit", Command.EXIT, 2 );
	
	private static MainMenu instance;
	
	public static boolean useColors;
	
	private static Session currentSession;

	private SessionsMenu sessionsMenu;
	
//#ifndef nomacros
	private static MacrosMenu macrosMenu;
//#endif	
	private static Activatable settingsMenu;
	
	private static final String ITEM_SESSIONS = "Sessions";
//#ifndef nomacros
	private static final String ITEM_MACROS = "Macros";
//#endif
//#ifndef nosettings
	private static final String ITEM_SETTINGS = "Settings";
//#endif
//#ifndef nodocs
	private static final String ITEM_ABOUT = "About MidpSSH";
	private static final String ITEM_HELP = "Help";
//#endif
	private static final String ITEM_QUIT = "Quit";

	/**
	 * @param arg0
	 * @param arg1
	 */
	public MainMenu() {
		super( "MidpSSH", List.IMPLICIT );
		
		MainMenu.instance = this;
		
		useColors = getDisplay().isColor();

		append( ITEM_SESSIONS, null );
//#ifndef nomacros
		append( ITEM_MACROS, null );
//#endif
//#ifndef nosettings
		append( ITEM_SETTINGS, null );
//#endif
//#ifndef nodocs
		append( ITEM_ABOUT, null );
		append( ITEM_HELP, null );
//#endif
		append( ITEM_QUIT, null );

		//setSelectCommand( selectCommand );
		addCommand( quitCommand );

		setCommandListener( this );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.lcdui.CommandListener#commandAction(javax.microedition.lcdui.Command,
	 *      javax.microedition.lcdui.Displayable)
	 */
	public void commandAction( Command com, Displayable displayable ) {
		if ( com == List.SELECT_COMMAND /*|| com == selectCommand*/ ) {
			String command = getString( getSelectedIndex() );
			if ( command.equals( ITEM_SESSIONS ) ) {
				doSessions();
			}
//#ifndef nomacros
			else if ( command.equals( ITEM_MACROS ) ) {
				doMacros( this );
			}
//#endif
//#ifndef nosettings			
			else if ( command.equals( ITEM_SETTINGS ) ) {    
				doSettings();
			}
//#endif
//#ifndef nodocs
			else if ( command.equals( ITEM_ABOUT ) ) {
					doAbout();
			}
			else if ( command.equals( ITEM_HELP ) ) {
			    doHelp();
			}
//#endif
			else if ( command.equals( ITEM_QUIT ) ) {
				Main.quitApp();
			}
		}
		else if (com == okCommand) {
			goMainMenu();
		}
		else if ( com == quitCommand ) {
			Main.quitApp();
		}
	}

	private void doSessions() {
		if ( sessionsMenu == null ) {
			sessionsMenu = new SessionsMenu();
		}
		sessionsMenu.activate( this );
	}
	
//#ifndef nomacros
	public static void doMacros( Activatable back ) {
		if ( macrosMenu == null ) {
			macrosMenu = new MacrosMenu();
		}
		macrosMenu.activate( back );
	}
//#endif
	
//#ifndef nosettings
	private void doSettings() {
		if ( settingsMenu == null ) {
			settingsMenu = new SettingsMenu();
		}
		settingsMenu.activate( this );
	}
//#endif
	
	public static Form showMessage(String title, String msg, CommandListener commandListener) {
		Form form = new Form(title);
		form.append( new StringItem( null, msg ) );
		form.addCommand(okCommand);
		form.setCommandListener(commandListener);
		setDisplay(form);
		return form;
	}
	
	private void doAbout() {
		showMessage( "About MidpSSH",
				"Version @VERSION@\n\n" +
		        "MidpSSH is a Telnet and SSH application for Java compatible phones and other mobile devices.\n\n" +
				"Please visit the project website for more information:\n" +
				"http://www.xk72.com/midpssh/\n\n" +
				"MidpSSH is developed by Karl von Randow. MidpSSH is based upon " +
				"FloydSSH and Telnet Floyd by Radek Polak.\n\n" +
				"MidpSSH is distributed under the GPL licence. For more information please " +
				"visit the website.",
				this);
	}
	
	private void doHelp() {
		showMessage( "MidpSSH Help",
				"Connecting\n\n" +
				"To connect to a remote server choose the Sessions option " +
				"from the main menu - create a new session, entering in the host and other details. " +
				"You can then connect to that server by choosing the new session.\n\n" +
				
				"More Information\n\n" +
				"For more information please visit the project website http://www.xk72.com/midpssh/",
				this);
	}
	
	/* (non-Javadoc)
	 * @see app.Activatable#activate()
	 */
	public void activate() {
		setDisplay( this );
	}
	/* (non-Javadoc)
	 * @see gui.Activatable#activate(gui.Activatable)
	 */
	public void activate( Activatable back ) {
		activate();
	}

	public static void setDisplay( Displayable display ) {
		getDisplay().setCurrent( display );
	}

	public static void alert( Alert alert, Displayable back ) {
		getDisplay().setCurrent( alert, back );
	}

	public static Display getDisplay() {
		return Display.getDisplay(Main.instance);
	}

	public static void goMainMenu() {
		setDisplay(instance);
	}

	public static void alertBackToMain( Alert alert ) {
		getDisplay().setCurrent(alert, instance);
	}
	
	public static void openSession( Session session ) {
		currentSession = session;
		session.activate();
	}
	
	public static Session currentSession() {
		return currentSession;
	}
	
	public static void showErrorMessage( String errorMessage ) {
		Alert alert = new Alert( "Error" );
		alert.setString( errorMessage );
		alert.setType( AlertType.ERROR );
		MainMenu.setDisplay( alert );
	}
}