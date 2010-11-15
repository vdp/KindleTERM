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
package gui.settings;

import gui.EditableForm;
import gui.MainMenu;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

import ssh.v2.DHKeyExchange;
import ssh.v2.PublicKeyAuthentication;
import app.Settings;

/**
 * NOTE on ChoiceGroup workarounds. Because of a bug in Java support on Blackberry Pearl we change the
 * ChoiceGroups to be Popup rather than Exclusive in Midp2. They look nicer as popup in Midp2 as well
 * I hope.
 * @author Karl von Randow
 */
public class SettingsForm extends EditableForm {
    
    public static final int MODE_NETWORK = 1;
    
    public static final int MODE_INTERFACE = 2;
    
    //#ifndef nofonts
    public static final int MODE_FONTS = 3;
    //#endif
    
//#ifdef ssh2   
    public static final int MODE_SSH = 4;
//#endif
    
    private int mode;
    
    protected TextField tfHttpProxy = new TextField("HTTP Proxy", "", 255, TextField.ANY);
    
    protected ChoiceGroup cgHttpProxyMode = new ChoiceGroup( "HTTP Proxy Mode", ChoiceGroup.EXCLUSIVE
    		//#ifdef midp2
    		* 0 + ChoiceGroup.POPUP
    		//#endif
    		);
    
	protected TextField tfType = new TextField( "Terminal Type", "", 20, TextField.ANY );
	
	protected TextField tfCols = new TextField( "Cols", "", 3, TextField.NUMERIC );
	
	protected TextField tfRows = new TextField( "Rows", "", 3, TextField.NUMERIC );
	
//#ifdef midp2    
    protected ChoiceGroup cgFullscreen = new ChoiceGroup( "Full Screen", ChoiceGroup.POPUP);
    
    protected ChoiceGroup cgRotated = new ChoiceGroup( "Orientation", ChoiceGroup.POPUP);
    
    protected ChoiceGroup cgPredictiveText = new ChoiceGroup("Predictive Text", ChoiceGroup.POPUP);
//#endif
    
//#ifndef nofonts
    protected ChoiceGroup cgFont = new ChoiceGroup( "Font Size",  ChoiceGroup.EXCLUSIVE
    		//#ifdef midp2
    		* 0 + ChoiceGroup.POPUP
    		//#endif
    		);
    
//#ifndef nofonts
    protected ChoiceGroup cgLCDFontMode = new ChoiceGroup( "LCD Font Mode",  ChoiceGroup.EXCLUSIVE
    		//#ifdef midp2
    		* 0 + ChoiceGroup.POPUP
    		//#endif
    );
//#endif
    
	protected TextField tfFg = new TextField( "Foreground", "", 6, TextField.ANY );
	
	protected TextField tfBg = new TextField( "Background", "", 6, TextField.ANY );
//#endif
    
    protected ChoiceGroup cgPolling = new ChoiceGroup("Polling I/O", ChoiceGroup.EXCLUSIVE
    		//#ifdef midp2
    		* 0 + ChoiceGroup.POPUP
    		//#endif
    		);
	
//#ifdef ssh2
    //#ifndef nossh1
    protected ChoiceGroup cgSsh = new ChoiceGroup("Prefer", ChoiceGroup.EXCLUSIVE
    		//#ifdef midp2
    		* 0 + ChoiceGroup.POPUP
    		//#endif
    		);
    //#endif
    
    protected ChoiceGroup cgSshPublicKey = new ChoiceGroup("Public Key", ChoiceGroup.EXCLUSIVE
    		//#ifdef midp2
    		* 0 + ChoiceGroup.POPUP
    		//#endif
    		);
    
    protected ChoiceGroup cgSshKeys = new ChoiceGroup("Store Session Key", ChoiceGroup.EXCLUSIVE
    		//#ifdef midp2
    		* 0 + ChoiceGroup.POPUP
    		//#endif
    		);
    
    protected ChoiceGroup cgSshKeySize = new ChoiceGroup("Session Key Size", ChoiceGroup.EXCLUSIVE
    		//#ifdef midp2
    		* 0 + ChoiceGroup.POPUP
    		//#endif
    		);
    
    private static final int[] sshKeySizes = new int[] { 32, 64, 128, 256, 512, 1024 };
//#endif
    
    public static void booleanChoiceGroup(ChoiceGroup cg) {
    	cg.append( "On", null );
        cg.append( "Off", null );
    }
    
	public SettingsForm( String title, int mode ) {
		super( title );
        
        this.mode = mode;
        
        switch ( mode ) {
        case MODE_NETWORK:
        {
        	append(tfHttpProxy);
        	cgHttpProxyMode.append("Off", null);
        	cgHttpProxyMode.append("Persistent", null);
        	cgHttpProxyMode.append("Transient", null);
        	append(cgHttpProxyMode);
            append( tfType );
            booleanChoiceGroup(cgPolling);
            append(cgPolling);
        }
        break;
        case MODE_INTERFACE:
        {
//#ifdef midp2
            booleanChoiceGroup(cgFullscreen);
            append( cgFullscreen );
//#endif
            
//#ifdef midp2
            cgRotated.append( "Normal", null );
            cgRotated.append( "Landscape", null );
            cgRotated.append( "Landscape Flipped", null );
            append( cgRotated );
//#endif

            //#ifndef noinstructions
            append( new StringItem( "Terminal Size", "The default is to use the maximum available screen area." ) );
            //#endif
            
            append( tfCols );
            append( tfRows );
            
            //#ifdef midp2
            booleanChoiceGroup(cgPredictiveText);
            append(cgPredictiveText);
            //#endif
        }
        break;
        //#ifndef nofonts
        case MODE_FONTS:
        {
            cgFont.append( "Tiny", null );
            cgFont.append( "Device", null );
            //#ifdef midp2
            cgFont.append( "LCD 3x6", null );
            cgFont.append( "LCD 4x6", null );
            cgFont.append( "LCD 4x7", null );
            cgFont.append( "LCD 5x9", null );
            cgFont.append( "LCD 8x16", null );
            //#endif
            append( cgFont );
            
            //#ifdef midp2
            cgLCDFontMode.append("RGB", null);
            cgLCDFontMode.append("BGR", null);
            append(cgLCDFontMode);
            //#endif

            append( tfFg );
            append( tfBg );
        }
        break;
        //#endif
//#ifdef ssh2
        case MODE_SSH:
        {
        	//#ifndef nossh1
            cgSsh.append( "SSH1", null);
            cgSsh.append( "SSH2", null);
            append(cgSsh);
            //#endif
            
            booleanChoiceGroup(cgSshPublicKey);
            append(cgSshPublicKey);
            
            booleanChoiceGroup(cgSshKeys);
            append(cgSshKeys);
            
            for (int i = 0; i < sshKeySizes.length; i++) {
                cgSshKeySize.append(String.valueOf(sshKeySizes[i]), null);
            }
            append(cgSshKeySize);
        }
        break;
//#endif
        }
	}
    
    /* (non-Javadoc)
     * @see javax.microedition.lcdui.CommandListener#commandAction(javax.microedition.lcdui.Command, javax.microedition.lcdui.Displayable)
     */
    public void commandAction( Command command, Displayable displayable ) {
    	//#ifdef ssh2
    	if (displayable instanceof Alert) {
    		if (cgSshKeys.getSelectedIndex() == 0 && Settings.ssh2x == null) {
	    		byte[][] keys = DHKeyExchange.generateKeyPairBytes(Settings.ssh2KeySize);
	            Settings.ssh2x = keys[0];
	            Settings.ssh2y = keys[1];
    		}
    		if (cgSshPublicKey.getSelectedIndex() == 0 && Settings.x == null) {
    			byte[][] keys = PublicKeyAuthentication.generateKeyPair();
    			Settings.x = keys[0];
    			Settings.y = keys[1];
    		}
    		
            Settings.saveSettings();
            //#ifdef midp2
            MainMenu.getDisplay().vibrate(300);
            //#endif
        	doBack();
    	}
    	else
    	//#endif
        super.commandAction( command, displayable );
    }
    
	protected void doBack() {
		 if ( doSave() ) {
             Settings.saveSettings( );
             super.doBack();
         }
	}

	/* (non-Javadoc)
	 * @see gui.Activatable#activate()
	 */
	public void activate() {
        switch ( mode ) {
        case MODE_NETWORK:
        {
        	tfHttpProxy.setString(Settings.httpProxy);
        	cgHttpProxyMode.setSelectedIndex(Settings.httpProxyMode, true);
            tfType.setString( Settings.terminalType );
        	cgPolling.setSelectedIndex(Settings.pollingIO ? 0 : 1, true);
        }
        break;
        case MODE_INTERFACE:
        {
//#ifdef midp2
            cgFullscreen.setSelectedIndex( Settings.terminalFullscreen ? 0 : 1, true );
//#endif
            
//#ifdef midp2
            switch ( Settings.terminalRotated ) {
            case Settings.ROT_NORMAL:
                cgRotated.setSelectedIndex( 0, true );
                break;
            case Settings.ROT_270:
                cgRotated.setSelectedIndex( 1, true );
                break;
            case Settings.ROT_90:
                cgRotated.setSelectedIndex( 2, true );
                break;
            }
//#endif
            
            int cols = Settings.terminalCols;
            int rows = Settings.terminalRows;
            if ( cols > 0 ) {
                tfCols.setString( String.valueOf(cols) );
            }
            else {
                tfCols.setString( "" );
            }
            if ( rows > 0 ) {
                tfRows.setString( String.valueOf(rows) );
            }
            else {
                tfRows.setString( "" );
            }
            
            //#ifdef midp2
            cgPredictiveText.setSelectedIndex(Settings.predictiveText ? 0 : 1, true);
            //#endif
        }
        break;
        //#ifndef nofonts
        case MODE_FONTS:
        {
        	cgFont.setSelectedIndex(Settings.fontMode, true);
        	//#ifdef midp2
        	cgLCDFontMode.setSelectedIndex(Settings.lcdFontMode, true);
        	//#endif
            tfFg.setString( toHex( Settings.fgcolor ) );
            tfBg.setString( toHex( Settings.bgcolor ) );
        }
        break;
        //#endif
//#ifdef ssh2
        case MODE_SSH:
        {
        	//#ifndef nossh1
        	cgSsh.setSelectedIndex(Settings.sshVersionPreferred == 2 ? 1 : 0, true);
            //#endif
            cgSshPublicKey.setSelectedIndex(Settings.x != null ? 0 : 1, true);
            cgSshKeys.setSelectedIndex(Settings.ssh2StoreKey ? 0 : 1, true);
            for (int i = 0; i < sshKeySizes.length; i++) {
                if (Settings.ssh2KeySize == sshKeySizes[i]) {
                    cgSshKeySize.setSelectedIndex(i, true);
                    break;
                }
            }
        }
        break;
//#endif
        }
		
		super.activate();
	}
	
	protected boolean doSave() {
        switch ( mode ) {
        case MODE_NETWORK:
        {
        	Settings.httpProxy = tfHttpProxy.getString();
        	Settings.httpProxyMode = cgHttpProxyMode.getSelectedIndex();
            Settings.terminalType = tfType.getString();
        	Settings.pollingIO = cgPolling.getSelectedIndex() == 0;
        }
        break;
        case MODE_INTERFACE:
        {
//#ifdef midp2
            Settings.terminalFullscreen = cgFullscreen.getSelectedIndex() == 0;
//#endif
                
//#ifdef midp2
            switch ( cgRotated.getSelectedIndex() ) {
            case 0:
                Settings.terminalRotated = Settings.ROT_NORMAL;
                break;
            case 1:
                Settings.terminalRotated = Settings.ROT_270;
                break;
            case 2:
                Settings.terminalRotated = Settings.ROT_90;
                break;
            }
//#endif
                
            try {
                Settings.terminalCols = Integer.parseInt( tfCols.getString() );
            }
            catch ( NumberFormatException e ) {
                Settings.terminalCols = 0;
            }
            try {
                Settings.terminalRows = Integer.parseInt( tfRows.getString() );
            }
            catch ( NumberFormatException e ) {
                Settings.terminalRows = 0;
            }
            
            //#ifdef midp2
            Settings.predictiveText = cgPredictiveText.getSelectedIndex() == 0;
            //#endif
        }
        break;
        //#ifndef nofonts
        case MODE_FONTS:
        {
        	Settings.fontMode = cgFont.getSelectedIndex();
        	//#ifdef midp2
        	Settings.lcdFontMode = (byte) cgLCDFontMode.getSelectedIndex();
        	//#endif
            try {
                int col = Integer.parseInt( tfFg.getString(), 16 );
                Settings.fgcolor = col;
            }
            catch ( NumberFormatException e ) {
                Settings.fgcolor = Settings.DEFAULT_FGCOLOR;
            }
            
            try {
                int col = Integer.parseInt( tfBg.getString(), 16 );
                Settings.bgcolor = col;
            }
            catch ( NumberFormatException e ) {
                Settings.bgcolor = Settings.DEFAULT_BGCOLOR;
            }
        }
        break;
        //#endif
//#ifdef ssh2
        case MODE_SSH:
        {
        	//#ifndef nossh1
            Settings.sshVersionPreferred = cgSsh.getSelectedIndex() == 1 ? 2 : 1;
            //#endif
            boolean ssh2StoreKey = cgSshKeys.getSelectedIndex() == 0;
            Settings.ssh2StoreKey = ssh2StoreKey;
            int newKeySize = sshKeySizes[cgSshKeySize.getSelectedIndex()];
            if (newKeySize != Settings.ssh2KeySize || !ssh2StoreKey) {
                Settings.ssh2KeySize = newKeySize;
                Settings.ssh2x = null;
                Settings.ssh2y = null;
            }
            
            boolean ssh2PublicKey = cgSshPublicKey.getSelectedIndex() == 0;
            if (!ssh2PublicKey) {
            	Settings.x = null;
            	Settings.y = null;
            }
            
            if ((ssh2StoreKey && Settings.ssh2x == null) || (ssh2PublicKey && Settings.x == null)) {
                /* Pregenerate ssh2 key */
                Alert alert = new Alert("MidpSSH");
                alert.setString("Generating keys, please wait...");
                alert.setTimeout(1);
                alert.setCommandListener(this);
                MainMenu.setDisplay(alert);
                return false;
            }
        }
        break;
//#endif
        }
		return true;
	}
	
	private static String toHex(int i) {
		String str = Integer.toHexString(i);
		while (str.length() < 6) {
			str = "0" + str;
		}
		return str;
	}
	
//	private static int fromHex( String hex ) throws NumberFormatException {
//		hex = hex.toLowerCase();
//		int total = 0;
//		for ( int i = 0; i < hex.length(); i++ ) {
//			total <<= 4;
//			char c = hex.charAt( i );
//			if ( c >= '0' && c <= '9' ) {
//				total += ( c - '0' );
//			}
//			else if ( c >= 'a' && c <= 'f' ) {
//				total += ( c - 'a' ) + 10;
//			}
//			else {
//				throw new NumberFormatException( hex );
//			}
//		}
//		return total;
//	}
//	
//	private static String toHex( int i ) {
//		char[] buf = new char[32];
//		int charPos = 32;
//		do {
//		    buf[--charPos] = digits.charAt(i & 15);
//		    i >>>= 4;
//		} while ( charPos > 26 || i != 0 );
//
//		return new String(buf, charPos, (32 - charPos));
//	}
//	
//	private static final String digits = "0123456789abcdef";
}
