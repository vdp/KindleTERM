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

import gui.settings.SettingsForm;

import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

import app.SessionManager;
import app.SessionSpec;
import app.Settings;

/**
 * @author Karl von Randow
 * 
 */
public class SessionForm extends EditableForm {

	public static final String WARNING_REQUIRED = "Please fill in required fields.";

	private int connectionIndex = 1;

	private boolean edit;
	
	private TextField tfAlias, tfHost, tfUsername, tfPassword;
    
//#ifdef blackberryconntypes
    private ChoiceGroup cgBlackberryConnType;
//#endif
    
    //#ifdef ssh2
    private ChoiceGroup cgUsePublicKey;
    //#endif

    //#ifndef notelnet
	private ChoiceGroup cgType;
	
	private static String[] typeNames = new String[] {
			"SSH", "Telnet"
	};

	private static String[] typeCodes = new String[] {
			SessionSpec.TYPE_SSH, SessionSpec.TYPE_TELNET
	};
	//#endif

	/**
	 * @param arg0
	 */
	public SessionForm( boolean edit ) {
	    super( edit ? "Edit Session" : "New Session" );

		this.edit = edit;

		tfAlias = new TextField( "Alias:", null, 255, TextField.ANY );
		tfHost = new TextField( "Host:", null, 255, TextField.ANY );
//#ifdef midp2
        tfHost.setConstraints(TextField.ANY | TextField.URL);
//#endif
		tfUsername = new TextField( "Username:", null, 255, TextField.ANY );
//#ifdef midp2
		if (!Settings.predictiveText) {
			tfUsername.setConstraints(TextField.ANY | TextField.NON_PREDICTIVE);
		}
//#endif
		tfPassword = new TextField( "Password:", null, 255, TextField.PASSWORD );
		//#ifndef notelnet
		cgType = new ChoiceGroup( "Type", ChoiceGroup.EXCLUSIVE
    		//#ifdef midp2
    		* 0 + ChoiceGroup.POPUP
    		//#endif
			);
		for ( int i = 0; i < typeNames.length; i++ ) {
			cgType.append( typeNames[i], null );
		}
		//#endif

		append( tfAlias );
		append( tfHost );
//#ifndef notelnet
		append( cgType );
		//#ifndef noinstructions
		append( new StringItem( "Authentication:\n", "For SSH connections only. Optional." ) );
		//#endif
//#endif
		append( tfUsername );
		append( tfPassword );
		
		//#ifdef ssh2
		if (Settings.x != null) {
			cgUsePublicKey = new ChoiceGroup("Use Public Key", ChoiceGroup.EXCLUSIVE
		    		//#ifdef midp2
		    		* 0 + ChoiceGroup.POPUP
		    		//#endif
					);
			SettingsForm.booleanChoiceGroup(cgUsePublicKey);
			append(cgUsePublicKey);
		}
		//#endif

//#ifdef blackberryconntypes
        cgBlackberryConnType = new ChoiceGroup( "Connection Type", ChoiceGroup.EXCLUSIVE
        		//#ifdef midp2
        		* 0 + ChoiceGroup.POPUP
        		//#endif
        		);
        cgBlackberryConnType.append( "Default", null );
        cgBlackberryConnType.append( "TCP/IP", null );
        cgBlackberryConnType.append( "BES", null );
        cgBlackberryConnType.append( "WiFi", null );
        append(cgBlackberryConnType);
//#endif
        
        if (!edit) {
		    addOKCommand();
		}
	}

	protected void doBack() {
		if (edit) {
			if (doSave(false)) {
				super.doBack();
			}
		} else {
			super.doBack();
		}
	}

	protected void doOK() {
		if (doSave(true)) {
			super.doOK();
		}
	}

	/* (non-Javadoc)
	 * @see gui.Activatable#activate()
	 */
	public void activate() {
	    if ( !edit ) {
			tfAlias.setString( "" );
			tfHost.setString( "" );
			tfUsername.setString( "" );
			tfPassword.setString( "" );
	    }
		super.activate();
	}

	public void setConnectionIndex( int connectionIndex ) {
		this.connectionIndex = connectionIndex;

		SessionSpec conn = SessionManager.getSession( connectionIndex );
		if ( conn != null ) {
			tfAlias.setString( conn.alias );
			tfHost.setString( conn.host );
			//#ifndef notelnet
			if ( conn.type != null ) {
				for ( int i = 0; i < typeCodes.length; i++ ) {
					if ( typeCodes[i].equals( conn.type ) ) {
						cgType.setSelectedIndex( i, true );
					}
				}
			}
			//#endif
			tfUsername.setString( conn.username );
			tfPassword.setString( conn.password );
			
			//#ifdef ssh2
			if (cgUsePublicKey != null) {
				cgUsePublicKey.setSelectedIndex(conn.usepublickey ? 0 : 1, true);
			}
            //#endif
			
//#ifdef blackberryconntypes
            switch ( conn.blackberryConnType ) {
            case SessionSpec.BLACKBERRY_CONN_TYPE_DEFAULT:
                cgBlackberryConnType.setSelectedIndex( 0, true );
                break;
            case SessionSpec.BLACKBERRY_CONN_TYPE_DEVICESIDE:
                cgBlackberryConnType.setSelectedIndex( 1, true );
                break;
            case SessionSpec.BLACKBERRY_CONN_TYPE_PROXY:
                cgBlackberryConnType.setSelectedIndex( 2, true );
                break;
            case SessionSpec.BLACKBERRY_CONN_TYPE_WIFI:
            	cgBlackberryConnType.setSelectedIndex( 3, true );
            	break;
            }
//#endif
		}
	}

	private boolean doSave(boolean create) {
		if ( !create || connectionIndex != -1 ) {
			if ( validateForm() ) {
				SessionSpec conn = new SessionSpec();
				conn.alias = tfAlias.getString();
				//#ifndef notelnet
				conn.type = typeCodes[cgType.getSelectedIndex()];
				//#else
				conn.type = SessionSpec.TYPE_SSH;
				//#endif
				conn.host = tfHost.getString();
				conn.username = tfUsername.getString();
				conn.password = tfPassword.getString();
				//#ifdef ssh2
				if (cgUsePublicKey != null) {
					conn.usepublickey = cgUsePublicKey.getSelectedIndex() == 0;
				}
				//#endif
//#ifdef blackberryconntypes
                conn.blackberryConnType = selectedBlackberryConnType();
//#endif
                
                if (create) {
                	SessionManager.addSession( conn );
                }
                else {
                	SessionManager.replaceSession( connectionIndex, conn );
                }
                
				return true;
			}
		}
		return false;
	}
    
	protected boolean validateForm() {
		String alias = tfAlias.getString();
		String host = tfHost.getString();

		if (alias.length() == 0 || host.length() == 0) {
			MainMenu.showErrorMessage(WARNING_REQUIRED);
			return false;
		}
		else {
			return true;
		}
	}

//#ifdef blackberryconntypes
    protected int selectedBlackberryConnType() {
        switch ( cgBlackberryConnType.getSelectedIndex() ) {
        case 0:
            return SessionSpec.BLACKBERRY_CONN_TYPE_DEFAULT;
        case 1:
            return SessionSpec.BLACKBERRY_CONN_TYPE_DEVICESIDE;
        case 2:
            return SessionSpec.BLACKBERRY_CONN_TYPE_PROXY;
        case 3:
        	return SessionSpec.BLACKBERRY_CONN_TYPE_WIFI;
        }
        return SessionSpec.BLACKBERRY_CONN_TYPE_DEFAULT;
    }
//#endif
}