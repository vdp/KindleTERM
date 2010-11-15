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

import java.io.EOFException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * 
 * @author karl
 * @version
 */
public class HttpFields extends Object implements Serializable, Cloneable {

	private ArrayList fieldNames = new ArrayList();

	private ArrayList fieldValues = new ArrayList();

	/** Holds value of property firstLine. */
	private String firstLine;

	/** Creates new HttpFields */
	public HttpFields() {
	}

	public boolean read(HttpInputStream in) throws IOException {
		firstLine = in.readLineInclusive();
		if (firstLine == null)
			throw new EOFException("EOF reading HTTP headers");
		String trimmedFirstLine = HttpInputStream.trimNewline(firstLine);
		if (trimmedFirstLine.length() == 0) {
			firstLine = "";
			return false;
		}

		// Sanity check, some servers don't return headers they just return body
		// straight away (eg. images on Tucows.com)
		if (firstLine.indexOf("HTTP/") == -1) {
			in.unreadLine(firstLine);
			firstLine = null;
			return true;
		}

		// Trim the newline characters
		firstLine = trimmedFirstLine;

		String line = in.readLine();
		String key = null;
		StringBuffer value = null;

		// System.out.println();
		// System.out.println( firstLine );
		while (line != null && line.length() != 0) {
			// System.out.println( line );
			if (line.startsWith(" ")) {
				if (value == null) {
					throw new IOException("Found header field continuation before a header field!: \"" + line + "\"");
				}
				// value.append( " " );
				value.append(line); // .trim()
			} else {
				int colon = line.indexOf(":");
				if (colon == -1) {
					if (value != null) {
						value.append(' ');
						value.append(line);
					}
				} else {
					if (key != null) {
						addField(key, value.toString().trim());
					}
					key = line.substring(0, colon);
					value = new StringBuffer();
					if (colon + 2 < line.length()) {
						value.append(line.substring(colon + 2)); // .trim()
					}
				}
			}

			line = in.readLine();
		}

		if (key != null) {
			addField(key, value.toString().trim());
		}

		if (line == null)
			throw new EOFException("EOF reading HTTP headers");
		return true;
	}

	public String get(String fieldName) {
		return getField(fieldName);
	}

	public String getField(String fieldName) {
		int n = fieldNames.size();

		for (int i = 0; i < n; i++) {
			String key = (String) fieldNames.get(i);
			if (key.equalsIgnoreCase(fieldName)) {
				return (String) fieldValues.get(i);
			}
		}

		return null;
	}

	public int getIntField(String fieldName) throws NumberFormatException {
		return getIntField(fieldName, -1);
	}

	public int getIntField(String fieldName, int defaultValue) throws NumberFormatException {
		String value = getField(fieldName);
		if (value != null) {
			value = value.trim();
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				// Try to get some numbers from the front as some spastic web
				// servers return something after the number,
				/*
				 * eg. from
				 * http://ad.nz.doubleclick.net/viewad/622507/asb_matrix_home_tile_260x80.gif
				 * HTTP/1.0 200 OK Server: DCLK-HttpSvr Content-Type: image/gif
				 * Content-Length: 7996Wed, 13 Aug 2003 04:51:35 GMT
				 */
				int iValue = -1;
				for (int i = 1; i <= value.length(); i++) {
					try {
						iValue = Integer.parseInt(value.substring(0, i));
					} catch (NumberFormatException f) {
						// If possible return the last number that was valid
						if (i > 1)
							return iValue;
						// Otherwise throw the original exception
						break;
					}
				}
				throw e;
			}
		} else {
			return defaultValue;
		}
	}

	/**
	 */
	public void addField(String key, String value) {
		fieldNames.add(key);
		fieldValues.add(value);
	}

	/**
	 * Getter for property firstLine.
	 * 
	 * @return Value of property firstLine.
	 */
	public String getFirstLine() {
		return firstLine;
	}

}
