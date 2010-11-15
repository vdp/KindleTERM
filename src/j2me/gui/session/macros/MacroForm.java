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

import gui.EditableForm;
import gui.MainMenu;
import gui.SessionForm;

import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.TextField;

import app.Settings;
import app.session.MacroSetManager;


/**
 * @author Karl von Randow
 * 
 */
public class MacroForm extends EditableForm {

    public static final int MODE_MACRO_SET = 1;
    public static final int MODE_MACRO = 2;
    
	private int macroSetIndex, macroIndex;

	private boolean edit;
	
	private TextField tfName, tfValue;
	
	private ChoiceGroup cgType;
    
    private boolean isMacroSet;

	/**
	 * @param arg0
	 */
	public MacroForm( boolean edit, boolean isMacroSet ) {
	    super( ( !isMacroSet ? ( edit ? "Edit Macro" : "New Macro" ) :
            ( edit ? "Edit Macro Set" : "New Macro Set" ) ) );

        this.isMacroSet = isMacroSet;
        
        if ( isMacroSet ) {
            tfName = new TextField( "Macro Set Name:", null, 255, TextField.ANY );
            append( tfName );
        }
        else {
    		tfValue = new TextField( "Value:", null, 255, TextField.ANY );
//#ifdef midp2
    		if (!Settings.predictiveText) {
    			tfValue.setConstraints(TextField.ANY | TextField.NON_PREDICTIVE);
    		}
//#endif
            
    		tfName = new TextField( "Name (Optional):", null, 255, TextField.ANY );
    		cgType = new ChoiceGroup( "Mode", ChoiceGroup.EXCLUSIVE
    	    		//#ifdef midp2
    	    		* 0 + ChoiceGroup.POPUP
    	    		//#endif
    				);
    		cgType.append( "Enter", null );
    		cgType.append( "Type", null );
    		
    		append( tfName );
    		append( tfValue );
    		append( cgType );
        }

        if (!edit) {
		    addOKCommand();
		}
		this.edit = edit;
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
	        tfName.setString( "" );
            if ( tfValue != null ) {
                tfValue.setString( "" );
            }
	    }
		super.activate();
	}
	
	/**
	 * @param macroSetIndex The macroSetIndex to set.
	 */
	public void setMacroSetIndex( int macroSetIndex ) {
		this.macroSetIndex = macroSetIndex;
        
        if ( isMacroSet ) {
            MacroSet macroSet = MacroSetManager.getMacroSet( macroSetIndex );
            if ( macroSet != null ) {
                tfName.setString( macroSet.name );
            }
        }
	}

	public void setMacroIndices( int macroSetIndex, int macroIndex ) {
		this.macroSetIndex = macroSetIndex;
		this.macroIndex = macroIndex;

		MacroSet macroSet = MacroSetManager.getMacroSet( macroSetIndex );
		MacroSet macro = macroSet.getMacro( macroIndex );
		tfName.setString( macro.name );
		
		String value = macro.value;
		if ( value.endsWith( "\n" ) ) {
			cgType.setSelectedIndex( 0, true );
			value = value.substring( 0, value.length() - 1 );
		}
		else {
			cgType.setSelectedIndex( 1, true );
		}
		tfValue.setString( value );
	}

	private boolean doSave(boolean create) {
		if (create || macroSetIndex != -1 ) {
			if ( validateForm() ) {
                if ( isMacroSet ) {
                    MacroSet macroSet = create ? new MacroSet() : MacroSetManager.getMacroSet( macroSetIndex );
                    macroSet.name = tfName.getString();
                    if (create) {
                    	MacroSetManager.addMacroSet( macroSet );
                    }
                    else {
                    	MacroSetManager.replaceMacroSet( macroSetIndex, macroSet );
                    }
                }
                else {
    				MacroSet macroSet = MacroSetManager.getMacroSet( macroSetIndex );
    				String value = tfValue.getString();
    				if ( cgType.getSelectedIndex() == 0 ) {
    					value += "\n";
    				}
    				MacroSet macro = new MacroSet( tfName.getString(), value );
    				if (create) {
    					macroSet.addMacro( macro );
    				}
    				else {
    					macroSet.replaceMacro( macroIndex, macro );
    				}
                }
                return true;
			}
		}
		return false;
	}

	protected boolean validateForm() {
		if ((isMacroSet && tfName.getString().length() == 0)
				|| (!isMacroSet && tfValue.getString().length() == 0)) {
			MainMenu.showErrorMessage(SessionForm.WARNING_REQUIRED);
			return false;
		}
		else {
			return true;
		}
	}
}