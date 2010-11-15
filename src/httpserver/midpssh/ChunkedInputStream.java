/*
 * @(#)ChunkedInputStream.java				0.3-3 06/05/2001
 *
 *  This file is part of the HTTPClient package
 *  Copyright (C) 1996-2001 Ronald Tschal�r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free
 *  Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 *  MA 02111-1307, USA
 *
 *  For questions, suggestions, bug-reports, enhancement-requests etc.
 *  I may be contacted at:
 *
 *  ronald@innovation.ch
 *
 *  The HTTPClient's home page is located at:
 *
 *  http://www.innovation.ch/java/HTTPClient/
 *
 */

package midpssh;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class de-chunks an input stream.
 * 
 * @version 0.3-3 06/05/2001
 * @author Ronald Tschal�r
 */
public class ChunkedInputStream extends FilterInputStream {
	/**
	 * @param is
	 *            the input stream to dechunk
	 */
	public ChunkedInputStream(InputStream is) {
		super(is);
	}

	byte[] one = new byte[1];

	public synchronized int read() throws IOException {
		int b = read(one, 0, 1);
		if (b == 1)
			return (one[0] & 0xff);
		else
			return -1;
	}

	private long chunk_len = -1;

	private boolean eof = false;

	public synchronized int read(byte[] buf, int off, int len) throws IOException {
		if (eof)
			return -1;

		if (chunk_len == -1) // it's a new chunk
		{
			try {
				chunk_len = getChunkLength(in);
			} catch (ParseException pe) {
				throw new IOException(pe.toString());
			}
		}

		if (chunk_len > 0) // it's data
		{
			if (len > chunk_len)
				len = (int) chunk_len;
			int rcvd = in.read(buf, off, len);
			if (rcvd == -1)
				throw new EOFException("Premature EOF encountered");

			chunk_len -= rcvd;
			if (chunk_len == 0) // got the whole chunk
			{
				in.read(); // CR
				in.read(); // LF
				chunk_len = -1;
			}

			return rcvd;
		} else // the footers (trailers)
		{
			// discard
			/*
			 * try { readLines(); } catch ( IOException e ) {}
			 */

			eof = true;
			return -1;
		}
	}

	/**
	 * Gets the length of the chunk.
	 * 
	 * @param input
	 *            the stream from which to read the next chunk.
	 * @return the length of chunk to follow (w/o trailing CR LF).
	 * @exception ParseException
	 *                If any exception during parsing occured.
	 * @exception IOException
	 *                If any exception during reading occured.
	 */
	final static long getChunkLength(InputStream input) throws ParseException, IOException {
		byte[] hex_len = new byte[16]; // if they send more than 8EB chunks...
		int off = 0, ch;

		// read chunk length

		while ((ch = input.read()) > 0 && (ch == ' ' || ch == '\t'))
			;
		if (ch < 0)
			throw new EOFException("Premature EOF while reading chunk length");
		hex_len[off++] = (byte) ch;
		while ((ch = input.read()) > 0 && ch != '\r' && ch != '\n' && ch != ' ' && ch != '\t' && ch != ';' && off < hex_len.length)
			hex_len[off++] = (byte) ch;

		while ((ch == ' ' || ch == '\t') && (ch = input.read()) > 0)
			;
		if (ch == ';') // chunk-ext (ignore it)
			while ((ch = input.read()) > 0 && ch != '\r' && ch != '\n')
				;

		if (ch < 0)
			throw new EOFException("Premature EOF while reading chunk length");
		if (ch != '\n' && (ch != '\r' || input.read() != '\n'))
			throw new ParseException("Didn't find valid chunk length: " + new String(hex_len, 0, off, "8859_1"));

		// parse chunk length

		try {
			return Long.parseLong(new String(hex_len, 0, off, "8859_1").trim(), 16);
		} catch (NumberFormatException nfe) {
			throw new ParseException("Didn't find valid chunk length: " + new String(hex_len, 0, off, "8859_1"));
		}
	}

	// private void readLines() throws IOException {
	// /* This loop is a merge of readLine() from DataInputStream and
	// * the necessary header logic to merge continued lines and terminate
	// * after an empty line. The reason this is explicit is because of
	// * the need to handle InterruptedIOExceptions.
	// */
	// loop: while ( true ) {
	// boolean got_cr = false, bol = false;
	// int b = super.read();
	// switch ( b ) {
	// case -1:
	// throw new EOFException( "Encountered premature EOF while reading
	// trailers" );
	// case '\r':
	// got_cr = true;
	// break;
	// case '\n':
	// if ( bol ) break loop; // all headers read
	// bol = true;
	// got_cr = false;
	// break;
	// case ' ':
	// case '\t':
	// if ( bol ) // a continued line
	// {
	// // replace previous \n with SP
	// bol = false;
	// break;
	// }
	// default:
	// if ( got_cr ) {
	// got_cr = false;
	// }
	// bol = false;
	// break;
	// }
	// }
	// }

	public synchronized long skip(long num) throws IOException {
		byte[] tmp = new byte[(int) num];
		int got = read(tmp, 0, (int) num);

		if (got > 0)
			return (long) got;
		else
			return 0L;
	}

	public synchronized int available() throws IOException {
		if (eof)
			return 0;

		if (chunk_len != -1)
			return (int) chunk_len + in.available();
		else
			return in.available();
	}
}
