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

import gui.EditableMenu;
import gui.MainMenu;

import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

import terminal.VT320;
import app.session.MacroSetManager;
import app.session.Session;

/**
 * @author Karl von Randow
 *
 */
public class MacrosMenu extends EditableMenu {
    
    protected static final Command useCommand = new Command( "Use", Command.ITEM, 1 );
	
	private MacroSet macroSet;
	
	private int macroSetIndex;
    
    private boolean isMacroSets;
	
    public MacrosMenu() {
        super( "Macro Sets" );
        isMacroSets = true;
    }
    
	public MacrosMenu( MacroSet macroSet, int macroSetIndex ) {
		super( macroSet.name );
        isMacroSets = false;
		this.macroSet = macroSet;
		this.macroSetIndex = macroSetIndex;
        
        if ( MainMenu.currentSession() != null ) {
            addCommand( useCommand );
        }
	}
	
    /* (non-Javadoc)
     * @see javax.microedition.lcdui.CommandListener#commandAction(javax.microedition.lcdui.Command, javax.microedition.lcdui.Displayable)
     */
    public void commandAction(Command command, Displayable displayable) {
        if ( command == useCommand ) {
            int i = getSelectedIndex();
            if ( i >= 0 && i < size() ) {
                Session session = MainMenu.currentSession();
                if ( session != null ) {
                    MacroSet macro = macroSet.getMacro( i );
                    if ( macro != null ) {
                    	session.getTerminal().doTextInput(macro.value.trim());
                    }
                }
                else {
                    doEdit( i );
                }
            }
        }
        else {
            super.commandAction(command, displayable);
        }
    }
	/* (non-Javadoc)
	 * @see gui.EditableMenu#addItems()
	 */
	protected void addItems() {
		deleteAll();

        if ( isMacroSets ) {
            Vector macroSets = MacroSetManager.getMacroSets();
            for ( int i = 0; i < macroSets.size(); i++ ) {
                MacroSet macroSet = (MacroSet) macroSets.elementAt( i );
                append( macroSet.name, null );
            }
        }
        else {
    		Vector macros = macroSet.macros;
			for ( int i = 0; i < macros.size(); i++ ) {
				MacroSet macro = (MacroSet) macros.elementAt( i );
				String name = macro.name;
				if (name.length() == 0) {
					name = macro.value.trim(); // trim off whitespace as it may end with a newline
				}
				append( name, null );
			}
        }
	}
	/* (non-Javadoc)
	 * @see gui.EditableMenu#doDelete(int)
	 */
	protected void doDelete( int i ) {
		if ( i != -1 ) {
            if ( isMacroSets ) {
                MacroSetManager.deleteMacroSet( i );
            }
            else {
                macroSet.deleteMacro( i );
            }
			delete( i );
		}
	}
	/* (non-Javadoc)
	 * @see gui.EditableMenu#doSelect(int)
	 */
	protected void doSelect( int i ) {
		if ( i != -1 ) {
            if ( isMacroSets ) {
                MacroSet macroSet = MacroSetManager.getMacroSet( i );
                MacrosMenu macrosMenu = new MacrosMenu( macroSet, i );
                macrosMenu.activate( this );
            }
            else {
    			Session session = MainMenu.currentSession();
    			if ( session != null ) {
    				MacroSet macro = macroSet.getMacro( i );
    				if ( macro != null ) {
    					doMacro(session, macro);
    					session.activate();
    				}
    			}
    			else {
    				doEdit( i );
    			}
            }
		}
	}
	
	private static final char MACRO_CTRL = '^';
	
	private static final char MACRO_BACKSLASH = '\\';
	
	private int indexOfCommand(String value, int start) {
		int n = value.length();
		for (int i = start; i < n; i++) {
			char c = value.charAt(i);
			if (c == MACRO_CTRL || c == MACRO_BACKSLASH) {
				return i;
			}
		}
		return -1;
	}
	
	private void doMacro(Session session, MacroSet macro) {
		String value = macro.value;
		
		int start = 0;
		int i = indexOfCommand(value, 0);
		while (i != -1) {
			if (i + 1 < value.length()) {
				if (i > start) {
					/* Output text before the control char */
					session.typeString(value.substring(start, i));
				}
				char com = value.charAt(i);
				char arg = value.charAt(i+1);
				if (arg == com) {
					/* An escaped command char */
					session.typeString(value.substring(i, i + 2));
				}
				else if (com == MACRO_CTRL) {
					/* Type control char */
					session.typeChar(arg, VT320.KEY_CONTROL);
				}
				else if (com == MACRO_BACKSLASH) {
					if (arg == 'n') {
						session.typeChar('\n', 0);
					}
					else if (arg == 'r') {
						session.typeChar('\r', 0);
					}
					else if (arg == 't') {
						session.typeChar('\t', 0);
					}
					else if (arg == 'e') {
						session.typeChar((char)27, 0);
					}
					else {
						session.typeString(value.substring(i, i + 2));
					}
				}
				start = i + 2;
			}
			else {
				start = i + 1;
			}
			i = indexOfCommand(value, start);
		}
		
		if (start < value.length()) {
			session.typeString(value.substring(start));
		}
	}
	
	protected void doEdit( int i ) {
		if ( i != -1 ) {
            MacroForm editMacroForm = new MacroForm( true, isMacroSets );
            if ( isMacroSets ) {
                editMacroForm.setMacroSetIndex( i );
            }
            else {
    			editMacroForm.setMacroIndices( macroSetIndex, i );
            }
            editMacroForm.activate( this );
		}
	}

	protected void doNew() {
        MacroForm newMacroForm = new MacroForm( false, isMacroSets );
        if ( !isMacroSets ) {
            newMacroForm.setMacroSetIndex( macroSetIndex );
        }
		newMacroForm.activate( this );
	}
}
