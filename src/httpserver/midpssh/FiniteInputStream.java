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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * FiniteInputStream is a FilterInputStream implementation that takes a limit on
 * the number of bytes that can be read from the stream.
 */
public class FiniteInputStream extends FilterInputStream {
	private long size;

	private long counter;

	public FiniteInputStream(InputStream in, long size) {
		super(in);
		this.size = size;
		counter = 0;
	}

	public int read() throws IOException {
		if (counter < size) {
			int c = in.read();
			if (c != -1) {
				counter++;
				return c;
			} else {
				return -1;
			}
		} else {
			return -1;
		}
	}

	public int read(byte b[]) throws IOException {
		return read(b, 0, b.length);
	}

	public int read(byte b[], int off, int len) throws IOException {
		if (counter + len >= size) {
			len = (int) (size - counter);
			if (len <= 0)
				return -1;
		}
		int n = in.read(b, off, len);
		if (n > 0) {
			counter += n;
		}
		return n;
	}

	public long skip(long n) throws IOException {
		if (counter + n >= size) {
			n = size - counter;
		}
		long skipped = in.skip(n);
		counter += skipped;
		return skipped;
	}

	public int available() throws IOException {
		int n = in.available();
		if (counter + n >= size) {
			n = (int) (size - counter);
		}
		return n;
	}

	public long getCounter() {
		return counter;
	}
}
