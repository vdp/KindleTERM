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
package gui;

import java.io.IOException;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import app.LineInputStream;
import app.SessionManager;
import app.SessionSpec;
import app.Settings;

/**
 * Import sessions using an HTTP connection. Parses the returned page looking for lines of the form:
 * ssh username@hostname[:port] alias
 * telnet hostname[:port] alias
 * @author Karl
 *
 */
public class ImportSessionsForm extends Form implements Activatable, Runnable, CommandListener {
    private TextField tfUrl;
    
    private Activatable back;
    
//#ifdef blackberryconntypes
    private ChoiceGroup cgBlackberryConnType;
//#endif
    
    /**
     * @param title
     * @param text
     * @param maxSize
     * @param constraints
     */
    public ImportSessionsForm() {
        super("Import Sessions");
        
        tfUrl = new TextField( "URL:", null, 255, TextField.ANY );
//#ifdef midp2
        tfUrl.setConstraints(TextField.ANY | TextField.URL);
//#endif
        append(tfUrl);
        
//#ifdef blackberryconntypes
        cgBlackberryConnType = new ChoiceGroup( "Connection Type", ChoiceGroup.EXCLUSIVE
    		//#ifdef midp2
    		* 0 + ChoiceGroup.POPUP
    		//#endif	
        	);
        cgBlackberryConnType.append( "Default", null );
        cgBlackberryConnType.append( "TCP/IP", null );
        cgBlackberryConnType.append( "BES", null );
        append(cgBlackberryConnType);
//#endif
              
        addCommand(MainMenu.okCommand);
        addCommand(MainMenu.backCommand);
        setCommandListener(this);
    }
    
    public void commandAction(Command command, Displayable arg1) {
        if (command == MainMenu.okCommand) {
            new Thread(this).start();
        }
        else if (command == MainMenu.backCommand) {
            if (back != null) {
                back.activate();
            }
        }
    }
    
    public void activate() {
        MainMenu.setDisplay(this);
    }
    public void activate(Activatable back) {
        this.back = back;
        activate();
    }
    
    public void run() {
        HttpConnection c = null;
        LineInputStream in = null;
        
        try {
            int imported = 0;
            
            String url = tfUrl.getString();
//#ifdef blackberryconntypes
            if (cgBlackberryConnType.getSelectedIndex() == 1) {
                url += ";deviceside=true";
            }
            else if (cgBlackberryConnType.getSelectedIndex() == 2) {
                url += ";deviceside=false";
            }
//#endif
//#ifdef blackberryenterprise
            url += ";deviceside=false";
//#endif
            
            c = (HttpConnection) Connector.open(url);
            int rc = c.getResponseCode();
            if (rc != HttpConnection.HTTP_OK) {
                throw new IOException("HTTP Error: " + rc);
            }
            
            in = new LineInputStream(c.openInputStream());
            String line = in.readLine();
            while (line != null) {
                String username = "", host = null, alias = "";
                SessionSpec spec = null;
                
                if (line.startsWith("ssh ")) {
                    int soh = 4;
                    int eoh = line.indexOf(' ', soh);
                    if (eoh != -1) {
                        int at = line.indexOf('@', soh);
                        if (at != -1 && at < eoh) {
                            /* Contains username */
                            username = line.substring(soh, at);
                            soh = at + 1;
                        }
                        
                        host = line.substring(soh, eoh);
                        alias = line.substring(eoh + 1).trim();
                        
                        spec = new SessionSpec();
                        spec.type = SessionSpec.TYPE_SSH;
                    }
                }
                else if (line.startsWith("telnet ")) {
                    int soh = 7;
                    int eoh = line.indexOf(' ', soh);
                    if (eoh != -1) {
                        host = line.substring(soh, eoh);
                        alias = line.substring(eoh + 1).trim();
                        
                        /* Insert or replace in Sessions list */
                        spec = new SessionSpec();
                        spec.type = SessionSpec.TYPE_TELNET;
                    }
                }
                
                if (spec != null) {
                    /* Insert or replace in Sessions list */
                    spec.alias = alias;
                    spec.host = host;
                    spec.username = username;
                    spec.password = "";
                    appendOrReplaceSession(spec);
                    
                    imported++;
                }
                
                line = in.readLine();
            }
            
            back.activate();
            Settings.sessionsImportUrl = url;
            Settings.saveSettings();
            
            Alert alert = new Alert( "Import Complete" );
            alert.setType( AlertType.INFO );
            alert.setString( "Imported " + imported + " sessions" );
            MainMenu.alert(alert, (Displayable)back);
        }
        catch (Exception e) {
            Alert alert = new Alert( "Import Failed" );
            alert.setType( AlertType.ERROR );
        
            alert.setString( e.getMessage() );
            alert.setTimeout( Alert.FOREVER );
            MainMenu.alert(alert, this);
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                }
            }
            if (c != null) {
                try {
                    c.close();
                } catch (IOException e1) {
                }
            }
        }
    }

    private void appendOrReplaceSession(SessionSpec newSpec) {
        SessionSpec spec = null;
        int replaceAt = -1;
        
        Vector sessions = SessionManager.getSessions();
        for (int i = 0; i < sessions.size(); i++) {
            spec = (SessionSpec) sessions.elementAt(i);
            if (spec.type.equals(newSpec.type)) {
                if (newSpec.alias.equals(spec.alias)) {
                    /* Replace this one */
                    replaceAt = i;
                    break;
                }
            }
        }
        
        if (replaceAt == -1) {
            SessionManager.addSession(newSpec);
        }
        else {
            spec.alias = newSpec.alias;
            spec.username = newSpec.username;
            spec.host = newSpec.host;
            SessionManager.replaceSession(replaceAt, spec);
        }
    }
}
