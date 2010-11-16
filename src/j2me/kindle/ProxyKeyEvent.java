/*
 * ProxyKeyEvent.java
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

package kindle;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.StringTokenizer;

public class ProxyKeyEvent extends KeyEvent {
    private final static String HDR_EVENT_ID = "EVENT_ID";
    private final static String HDR_MODIFIERS = "MODIFIERS";
    private final static String HDR_KEY_CODE = "KEY_CODE";
    private final static String HDR_KEY_CHAR = "KEY_CHAR";

    private int id;

    public ProxyKeyEvent(Component source, int id, long when, int modifiers,
            int keyCode, char keyChar) {
            super(source, id, when, modifiers, keyCode, keyChar);
            this.id = id;
    }

    public ProxyKeyEvent(KeyEvent ke) {
        super(ke.getComponent(), ke.getID(), ke.getWhen(), ke.getModifiers(),
                ke.getKeyCode(), ke.getKeyChar());
        this.id = ke.getID();
    }

    /**
     * Event format in text (everything below is of type string)
     * EVENT_ID (KEY_PRESSED | KEY_RELEASED | KEY_TYPED)
     * MODIFIERS STRING_REPR_OF_INT
     * KEY_CODE STRING_REPR_OF_INT
     * KEY_CHAR STRING_REPR_OF_CHAR
     */
    public static ProxyKeyEvent makeEvent(String eventDescr, Component source, long when) {
        int id = 0, modifiers = 0, code = 0;
        char keyChar = 0;

        StringTokenizer st = new StringTokenizer(eventDescr);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            
            if (token.equals(HDR_EVENT_ID)) {
                id = Integer.parseInt(st.nextToken());
            } 
            else if (token.equals(HDR_MODIFIERS)) {
                modifiers = Integer.parseInt(st.nextToken());
            }
            else if (token.equals(HDR_KEY_CODE)) {
                code = Integer.parseInt(st.nextToken());
            }
            else if (token.equals(HDR_KEY_CHAR)) {
                String charString = st.nextToken();
                keyChar = charString.charAt(0);
                if (keyChar == '\\') {
                    switch (charString.charAt(1)) {
                        case 'n':
                            keyChar = '\n';
                            break;
                        case 't':
                            keyChar = '\t';
                            break;
                        case 's':
                            keyChar = ' ';
                            break;
                    }
                }
            }
            else {
                //throw new IllegalArgumentException("Unexpected event token: " + token);
            }
        }

        ProxyKeyEvent theEvent = new ProxyKeyEvent(source, id, when, modifiers, code, keyChar);
        return theEvent;
    }

    public String toString() {
        String keyStr;

        StringBuffer sb = new StringBuffer(200);
        sb.append(HDR_EVENT_ID).append(' ').append(getID()).append('\n');
        sb.append(HDR_MODIFIERS).append(' ').append(getModifiers()).append('\n');
        sb.append(HDR_KEY_CODE).append(' ').append(getKeyCode()).append('\n');
        sb.append(HDR_KEY_CHAR).append(' ');

        char kc = getKeyChar();
        if (kc == '\n') {
            sb.append("\\n");
        }
        else if (kc == '\t') {
            sb.append("\\t");
        }
        else if (kc == ' ') {
            sb.append("\\s");
        }
        else {
            sb.append(kc);
        }
        
        return sb.toString();
    }

    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }
}
