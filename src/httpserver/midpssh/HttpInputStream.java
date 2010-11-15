/* This file is part of "MidpSSH".
 * Copyright (c) 2006 Karl von Randow.
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

package midpssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * 
 * @author karl
 * @version
 */
public class HttpInputStream extends PushbackInputStream {

	public static final String CHARSET = "ISO-8859-1";

	/** Creates new HttpInputStream */
	public HttpInputStream(InputStream in) {
		super(in, 8192);
	}

	public void waitForData() throws IOException {
		int c = read();
		unread(c);
	}

	/**
	 * The String contains a line of text read from the stream, not including
	 * the EOL character(s). The string is converted from bytes using ISO-8859-1
	 * encoding - in the hope that ISO-8859-1 is 8-bit safe. That is, the bytes
	 * can be converted to a String and back to bytes without changing the
	 * bytes.
	 * 
	 * @return
	 * @throws IOException
	 */
	public String readLine() throws IOException {
		if (buf == null) {
			buf = new byte[128];
		}

		int c = read();
		if (c == -1)
			return null;
		int i = 0;
		while (c != -1) {
			if (c == '\r') {
				int d = read();
				if (d == '\n') {
					break;
				}
				unread(d);
				break;
			} else if (c == '\n') {
				break;
			}

			addByteToBuffer(i++, c);
			c = read();
		}
		return new String(buf, 0, i, CHARSET);
	}

	public String readLineInclusive() throws IOException {
		if (buf == null) {
			buf = new byte[128];
		}

		int c = read();
		if (c == -1)
			return null;
		int i = 0;
		while (c != -1) {
			// First put the byte in the buffer
			addByteToBuffer(i++, c);

			if (c == '\r') {
				int d = read();
				if (d == '\n') {
					addByteToBuffer(i++, d);
					break;
				}
				unread(d);
				break;
			} else if (c == '\n') {
				break;
			}

			c = read();
		}
		return new String(buf, 0, i, CHARSET);
	}

	public static String trimNewline(String line) {
		if (line.endsWith("\r\n")) {
			return line.substring(0, line.length() - 2);
		} else if (line.endsWith("\r") || line.endsWith("\n")) {
			return line.substring(0, line.length() - 1);
		} else {
			return line;
		}
	}

	private void addByteToBuffer(int i, int c) {
		if (i >= buf.length) {
			// Extend the buffer
			byte[] buf = new byte[this.buf.length + 128];
			System.arraycopy(this.buf, 0, buf, 0, this.buf.length);
			this.buf = buf;
		}

		buf[i] = (byte) (c & 0xff);
	}

	public void unreadLine(String line) throws IOException {
		byte[] buf = line.getBytes(CHARSET);
		unread(buf);
	}

	private byte[] buf;
}
