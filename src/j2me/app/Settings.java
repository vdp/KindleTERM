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
package app;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * @author Karl von Randow
 */
public class Settings extends MyRecordStore {

	public static final int DEFAULT_BGCOLOR = 0x000000,
			DEFAULT_FGCOLOR = 0xffffff;

	public static final int ROT_NORMAL = 0;

	public static final int ROT_270 = 1;

	public static final int ROT_90 = 2;

	public static final int FONT_NORMAL = 0;

	public static final int FONT_DEVICE = 1;
	
	private static final String RMS_NAME = "settings";

	public static int bgcolor, fgcolor;

	public static int terminalCols, terminalRows;

	public static String terminalType;

	//#ifdef midp2
	public static int terminalRotated;
	//#endif
	
	public static int fontMode;
	
	//#ifndef nofonts
	//#ifdef midp2
	public static byte lcdFontMode;
	//#endif
	//#endif

	private static Settings me = new Settings();

	//#ifdef midp2
	public static boolean terminalFullscreen;
	//#endif
	
	public static String sessionsImportUrl;

	//#ifdef ssh2
	public static int sshVersionPreferred;

	public static boolean ssh2StoreKey;

	public static byte[] ssh2x, ssh2y;
	
	public static byte[] x, y;

	public static int ssh2KeySize;
	//#endif

	public static boolean pollingIO;

	//#ifdef midp2
	public static boolean predictiveText;
	//#endif
	
	public static String httpProxy;

	public static int httpProxyMode;

	public static void init() {
		defaults();
		me.load(RMS_NAME, false);
	}

	/**
	 * @param settings2
	 */
	public static void saveSettings() {
		Vector v = new Vector();
		v.addElement(null); // doesn't matter what we pass through, it just
							// calls write()
		me.save(RMS_NAME, v);
	}

	public static void defaults() {
		bgcolor = DEFAULT_BGCOLOR;
		fgcolor = DEFAULT_FGCOLOR;
		terminalCols = 0;
		terminalRows = 0;
		terminalType = "";
		//#ifdef midp2
		terminalRotated = ROT_NORMAL;
		//#endif
		fontMode = FONT_NORMAL;
		//#ifdef midp2
		terminalFullscreen = false;
		//#endif
		//#ifdef ssh2
		sshVersionPreferred = 1;
		//#endif
		sessionsImportUrl = "http://";
		//#ifdef ssh2
		ssh2StoreKey = true;
		ssh2x = null;
		ssh2y = null;
		ssh2KeySize = 512;
		//#endif
		pollingIO = false;
		//#ifdef midp2
		predictiveText = true;
		//#endif
		//#ifdef ssh2
		x = null;
		y = null;
		//#endif
		//#ifndef nofonts
		//#ifdef midp2
		lcdFontMode = 0;
		//#endif
		//#endif
		httpProxy = "";
		httpProxyMode = 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see app.MyRecordStore#read(java.io.DataInputStream)
	 */
	protected Object read(DataInputStream in) throws IOException {
		fgcolor = in.readInt();
		bgcolor = in.readInt();
		terminalCols = in.readInt();
		terminalRows = in.readInt();
		terminalType = in.readUTF();
		//#ifdef midp2
		terminalRotated = in.readInt();
		//#endif
		fontMode = in.readInt();
		//#ifdef midp2
		terminalFullscreen = in.readBoolean();
		//#endif
		//#ifdef ssh2
		sshVersionPreferred = in.readInt();
		//#endif
		sessionsImportUrl = in.readUTF();
		//#ifdef ssh2
		ssh2StoreKey = in.readBoolean();
		ssh2x = readByteArray(in);
		ssh2y = readByteArray(in);
		ssh2KeySize = in.readInt();
		//#endif
		pollingIO = in.readBoolean();
		//#ifdef midp2
		predictiveText = in.readBoolean();
		//#endif
		//#ifdef ssh2
		x = readByteArray(in);
		y = readByteArray(in);
		//#endif
		//#ifndef nofonts
		//#ifdef midp2
		lcdFontMode = in.readByte();
		//#endif
		//#endif
		httpProxy = in.readUTF();
		httpProxyMode = in.readByte();
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see app.MyRecordStore#write(java.io.DataOutputStream, java.lang.Object)
	 */
	protected void write(DataOutputStream out, Object ob) throws IOException {
		out.writeInt(fgcolor);
		out.writeInt(bgcolor);
		out.writeInt(terminalCols);
		out.writeInt(terminalRows);
		out.writeUTF(terminalType);
		//#ifdef midp2
		out.writeInt(terminalRotated);
		//#endif
		out.writeInt(fontMode);
		//#ifdef midp2
		out.writeBoolean(terminalFullscreen);
		//#endif
		//#ifdef ssh2
		out.writeInt(sshVersionPreferred);
		//#endif
		out.writeUTF(sessionsImportUrl);
		//#ifdef ssh2
		out.writeBoolean(ssh2StoreKey);
		writeByteArray(out, ssh2x);
		writeByteArray(out, ssh2y);
		out.writeInt(ssh2KeySize);
		//#endif
		out.writeBoolean(pollingIO);
		//#ifdef midp2
		out.writeBoolean(predictiveText);
		//#endif
		//#ifdef ssh2
		writeByteArray(out, x);
		writeByteArray(out, y);
		//#endif
		//#ifndef nofonts
		//#ifdef midp2
		out.writeByte(lcdFontMode);
		//#endif
		//#endif
		out.writeUTF(httpProxy);
		out.writeByte(httpProxyMode);
	}

	//#ifdef ssh2
	private byte[] readByteArray(DataInputStream in) throws IOException {
		int length = in.readInt();
		if (length == 0) {
			return null;
		} else {
			byte[] buf = new byte[length];
			in.readFully(buf);
			return buf;
		}
	}

	private void writeByteArray(DataOutputStream out, byte[] ray)
			throws IOException {
		if (ray != null) {
			out.writeInt(ray.length);
			out.write(ray);
		} else {
			out.writeInt(0);
		}
	}
	//#endif
}
