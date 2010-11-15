package midpssh;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Session {

	private Socket socket;
	
	private long lastAccessed;
	
	public Session() {
		lastAccessed = System.currentTimeMillis();
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public long getLastAccessed() {
		return lastAccessed;
	}

	public void setLastAccessed(long lastAccessed) {
		this.lastAccessed = lastAccessed;
	}

	public void close() throws IOException {
		socket.close();
	}
	
	public void connect(String host, int port) throws UnknownHostException, IOException {
		socket = new Socket(host, port);
	}

	public boolean isConnected() {
		return socket != null;
	}

}
