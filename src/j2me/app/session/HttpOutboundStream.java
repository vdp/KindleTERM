package app.session;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

public class HttpOutboundStream extends ByteArrayOutputStream {

	private String url;
	
	public HttpOutboundStream(String url) {
		this.url = url;
	}
	
	public synchronized void close() throws IOException {
		flush();
		super.close();
	}

	public void flush() throws IOException {
		super.flush();
		
		HttpConnection outbound = (HttpConnection) Connector.open(url, Connector.READ_WRITE, false);
		outbound.setRequestMethod(HttpConnection.POST);
		OutputStream out = outbound.openOutputStream();
		out.write(toByteArray());
		out.close();
		outbound.openInputStream().close();
		
		reset();
	}
	
}
