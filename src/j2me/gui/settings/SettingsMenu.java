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

import gui.Activatable;
import gui.MainMenu;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import app.Settings;

/**
 * @author Karl von Randow
 */
public class SettingsMenu extends List implements Activatable,
		CommandListener {

	private Activatable back;

	protected SettingsMenu(String title) {
		super(title, List.IMPLICIT);

		append("Network", null);
		append("Interface", null);
		//#ifndef nofonts
		append("Fonts", null);
		//#endif
		//#ifdef ssh2
		append("SSH", null);
		//#endif
		append("Restore Defaults", null);

		// setSelectCommand( selectCommand );
		addCommand(MainMenu.backCommand);

		setCommandListener(this);
	}

	public SettingsMenu() {
		this("Settings");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.microedition.lcdui.CommandListener#commandAction(javax.microedition.lcdui.Command,
	 *      javax.microedition.lcdui.Displayable)
	 */
	public void commandAction(Command command, Displayable displayable) {
		if (command == List.SELECT_COMMAND) {
			doSelect(getSelectedIndex());
		} else if (command == MainMenu.backCommand) {
			doBack();
		}
	}

	protected void doSelect(int i) {
		int j = 0;
		if (j++ == i) {
			showSettingsForm(i, SettingsForm.MODE_NETWORK);
		} else if (j++ == i) {
			showSettingsForm(i, SettingsForm.MODE_INTERFACE);
		//#ifndef nofonts
		} else if (j++ == i) {
			showSettingsForm(i, SettingsForm.MODE_FONTS);
		//#endif
		}
		//#ifdef ssh2
		else if (j++ == i) {
			showSettingsForm(i, SettingsForm.MODE_SSH);
		}
		//#endif
		else if (j++ == i) {
			Settings.defaults();
			Settings.saveSettings();
//			Main.alertBackToMain(new Alert("Settings",
//					"Default settings have been restored.", null,
//					AlertType.INFO));
			MainMenu.goMainMenu();
		}
	}

	private void showSettingsForm(int i, int mode) {
		SettingsForm f = new SettingsForm(getString(i), mode);
		f.activate(this);
	}

	private void doBack() {
		back.activate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gui.Activatable#activate()
	 */
	public void activate() {
		MainMenu.setDisplay(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gui.Activatable#activate(gui.Activatable)
	 */
	public void activate(Activatable back) {
		this.back = back;
		activate();
	}
}
