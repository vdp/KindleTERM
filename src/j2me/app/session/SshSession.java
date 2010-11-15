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
package app.session;

import gui.MainMenu;

import java.io.IOException;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

import ssh.SshIO;
import app.SessionSpec;
import app.Settings;

/**
 * @author Karl von Randow
 * 
 */
public class SshSession extends Session implements SessionIOHandler
	//#ifdef keybrdinteractive
	, CommandListener
	//#endif
	{
	

    private SshIO sshIO;
    
	public void connect(SessionSpec spec, String username, String password) {
        sshIO = new SshIO( this );
        sshIO.login = username != null ? username : spec.username;
        sshIO.password = password != null ? password : spec.password;
        sshIO.usepublickey = spec.usepublickey;
		super.connect( spec, this );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see telnet.Session#defaultPort()
	 */
	protected int defaultPort() {
		return 22;
	}
    /*
     * (non-Javadoc)
     * 
     * @see app.session.SessionIOListener#receiveData(byte[], int, int)
     */
    public void handleReceiveData( byte[] data, int offset, int length ) throws IOException {
        byte[] result;
        result = sshIO.handleSSH( data, offset, length );
        super.receiveData( result, 0, result.length );
    }

    /*
     * (non-Javadoc)
     * 
     * @see app.session.SessionIOListener#sendData(byte[], int, int)
     */
    public void handleSendData( byte[] data, int offset, int length ) throws IOException {
        if ( length > 0 ) {
            sshIO.sendData( data, offset, length );
        }
        else {
            sshIO.Send_SSH_NOOP();
        }
    }
    
    /*
     * Receive data send back by SshIO and send it out onto the network
     */
    public void sendData( byte [] data ) throws IOException {
        super.sendData( data, 0, data.length );
    }

	public String getTerminalID() {
		if ( Settings.terminalType.length() > 0 ) {
			return Settings.terminalType;
		}
		else {
			return emulation.getTerminalID();
		}
	}

	public int getTerminalWidth() {
		return emulation.width;
	}

	public int getTerminalHeight() {
		return emulation.height;
	}
	
	//#ifdef keybrdinteractive
	public void prompt(String name, String instruction, String[] prompts, boolean[] echos) throws IOException {
		if (prompts.length > 0) {
			/* Show prompt */
			if (name.length() == 0) {
				name = "Authenticate";
			}
			Form form = new Form(name);

			if (instruction.length() > 0) {
				form.append(new StringItem("Instructions", instruction));
			}
			promptFields = new TextField[prompts.length];
			for (int i = 0; i < prompts.length; i++) {
				promptFields[i] = new TextField(prompts[i], "", 255, TextField.ANY | (echos[i] ? 0 : TextField.PASSWORD));
				if (prompts[i].toLowerCase().startsWith("password:")) {
					promptFields[i].setString(sshIO.password);
				}
				form.append(promptFields[i]);
			}
			form.addCommand(MainMenu.okCommand);
			form.addCommand(MainMenu.backCommand);
			form.setCommandListener(this);
			MainMenu.setDisplay(form);
		}
		else {
			sshIO.sendUserauthInfoResponse(new String[0]);
		}
	}
	
	private TextField[] promptFields;

	public void commandAction(Command cmd, Displayable displayable) {
		try {
			if (cmd == MainMenu.okCommand) {
				String[] responses = new String[promptFields.length];
				for (int i = 0; i < responses.length; i++) {
					responses[i] = promptFields[i].getString();
				}
				sshIO.sendUserauthInfoResponse(responses);
			}
			else {
				sshIO.sendUserauthInfoResponse(new String[0]);
			}
		}
		catch (IOException e) {
			
		}
	
		promptFields = null;
		activate();
	}
	//#endif
}