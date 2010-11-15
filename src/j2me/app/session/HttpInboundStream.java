package app.session;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

public class HttpInboundStream extends InputStream {

	private String url;
	
	private byte[] buf;
	
	private int offset, length;
	
	public HttpInboundStream(String url) {
		this.url = url;
		this.buf = new byte[10240];
		this.offset = 0;
		this.length = 0;
	}

	public int read() throws IOException {
		if (length - offset <= 0) {
			fillBuffer();
		}
		
		if (length - offset <= 0) {
			return -1;
		}
		
		return buf[offset++];
	}
	
	public int read(byte[] destbuf, int destoffset, int destlength) throws IOException {
		if (length - offset <= 0) {
			fillBuffer();
		}
		
		if (length - offset <= 0) {
			throw new EOFException();
		}
		
		int readlength = Math.min(length - offset, destlength - destoffset);
		System.arraycopy(buf, offset, destbuf, destoffset, readlength);
		offset += readlength;
		return readlength;
	}

	public int read(byte[] buf) throws IOException {
		return read(buf, 0, buf.length);
	}

	private void fillBuffer() throws IOException {
		/* Note that we assume that the server will never send responses larger than our buffer size. If the server
		 * does we'll probably go into an infinite loop.
		 */
		HttpConnection conn = (HttpConnection) Connector.open(url, Connector.READ_WRITE, false);
		InputStream in = conn.openInputStream();
		int offset = 0;
		int read = in.read(buf, offset, buf.length - offset);
		while (read != -1) {
			offset += read;
			read = in.read(buf, offset, buf.length - offset);
		}
		
		length = offset;
		this.offset = 0;
		in.close();
	}
	
}
