/*
 * AwtSshSession.java
 * 
 * Copyright (c) 2010 VDP <vdp DOT kindle AT gmail.com>.
 * 
 * This file is part of MidpSSH.
 * 
 * MidpSSH is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MidpSSH is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MidpSSH.  If not, see <http ://www.gnu.org/licenses/>.
 */

package awt;

import app.session.ISshSession;
import app.session.SessionIOHandler;
import java.io.IOException;
import ssh.SshIO;

/**
 *
 * @author VDP <vdp DOT kindle AT gmail.com>
 */
public class AwtSshSession extends AwtSession 
        implements SessionIOHandler, ISshSession {

        private SshIO sshIO;

	public void connect(String host, int port,
                String username, String password) {
            sshIO = new SshIO(this);
            sshIO.login = username;
            sshIO.password = password;
            //sshIO.usepublickey = spec.usepublickey;
            super.connect(host, port, username, password, this);
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
        //System.out.println("Handle received data: " + new String(data) + "\n");
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
            return emulation.getTerminalID();
	}

	public int getTerminalWidth() {
		return emulation.width;
	}

	public int getTerminalHeight() {
		return emulation.height;
	}

	public void prompt(String name, String instruction, String[] prompts, boolean[] echos) throws IOException {
            System.out.println("PROMPT");
            throw new UnsupportedOperationException("AWT SSH Session prompt()");
	}
}
