/* This file is part of "MidpSSH".
 * Copyright (c) 2005 Karl von Randow.
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
package gui.session;

import gui.Activatable;
import gui.MainMenu;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import ssh.v2.PublicKeyAuthentication;
import terminal.VT320;
import app.Settings;
import app.session.Session;

/**
 * @author Karl
 *
 */
public class SpecialMenu extends List implements CommandListener, Activatable {
    
    private Activatable back, done;
    
    private int index;
    
    /**
     * @param title
     * @param mode
     */
    public SpecialMenu() {
        this( "Special", 0 );
    }
    
    public SpecialMenu( String title, int index ) {
        super(title, List.IMPLICIT);

		//setSelectCommand( selectCommand );
		addCommand( MainMenu.backCommand );
		setCommandListener( this );

        String options;
        this.index = index;
        switch (index) {
        case 0:
        	options = "Keys|Funcs|Symbols|Output|";
        	break;
        case 1:
        	options = "Bksp|Home|End|PgU|PgD|Del|Ins|";
        	break;
        case 2:
        	options = "F1|F2|F3|F4|F5|F6|F7|F8|F9|F10|F11|F12|";
        	break;
        case 3:
        	options = "||\\|~|:|;|'|\"|,|<|.|>|/|?|`|!|@|#|$|%|^|&|*|(|)|-|_|+|=|[|{|]|}|";
        	break;
        default:
        	//#ifdef ssh2
        	options = "Public Key|";
        	//#else
        	options = "";
        	//#endif
        }
        
        int start = 0;
        int i = options.indexOf('|', 1);
        while (i != -1) {
        	append(options.substring(start, i), null);
        	start = i + 1;
        	i = options.indexOf('|', start + 1); // +1 to that we see tokens that are single |
        }
    }
    
    public void commandAction( Command command, Displayable displayed ) {
		if ( command == List.SELECT_COMMAND ) {
	        Session session = MainMenu.currentSession();
			if ( session != null ) {
				int selectedIndex = getSelectedIndex();
				String option = null;
				int keyCode = 0;
				if (index == 0) {
					new SpecialMenu(getString(selectedIndex), selectedIndex + 1).activate(this, done);
				}
				else if (index == 1) {
					switch (selectedIndex) {
		            case 0:
		                keyCode = VT320.VK_BACK_SPACE;
		                break;
                    case 1:
                        keyCode = VT320.VK_HOME;
                        break;
                    case 2:
                        keyCode = VT320.VK_END;
                        break;
                    case 3:
                        keyCode = VT320.VK_PAGE_UP;
                        break;
                    case 4:
                        keyCode = VT320.VK_PAGE_DOWN;
                        break;
                    case 5:
                        keyCode = VT320.VK_DELETE;
                        break;
                    case 6:
                        keyCode = VT320.VK_INSERT;
                        break;
					}
				}
				else if (index == 2) {
					keyCode = VT320.VK_F1 + selectedIndex;
				}
				else if (index == 3) {
					option = getString(selectedIndex);
				}
				else if (index == 4) {
		        	//#ifdef ssh2
		        	if (Settings.x != null) {
		        		PublicKeyAuthentication pk = new PublicKeyAuthentication();
		        		option = pk.getPublicKeyText();
		        	}
		        	else {
		        		option = "";
		        	}
		        	//#endif
				}
				
				if (keyCode != 0) {
					session.typeKey(keyCode, 0);
					done.activate();
				}
				else if (option != null) {
					session.typeString(option);
					done.activate();
				}
		    }
		}
		else if ( command == MainMenu.backCommand ) {
		    if ( back != null ) {
		        back.activate();
		    }
		}
	}
    
    public void activate() {
        MainMenu.setDisplay( this );
    }
    public void activate(Activatable back) {
        activate( back, back );
    }
    public void activate(Activatable back, Activatable done) {
        this.back = back;
        this.done = done;
        activate();
    }
}
