/*
 * ISshSession.java
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

package app.session;

import java.io.IOException;

/**
 *
 * @author VDP <vdp DOT kindle AT gmail.com>
 */
public interface ISshSession {
    public void sendData( byte [] data ) throws IOException;
    public void prompt(String name, String instruction,
            String[] prompts, boolean[] echos) throws IOException;
    public String getTerminalID();
    public int getTerminalHeight();
    public int getTerminalWidth();
}
