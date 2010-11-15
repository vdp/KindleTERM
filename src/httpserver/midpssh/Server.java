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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class Server implements Runnable {

	private static final String HTTP_HEADERS = "HTTP/1.1 200 OK\r\nConnection: close\r\nContent-type: application/octet-stream\r\n";

	private static final String CRLF = "\r\n";

	private static Map current;

	private Socket clientSocket;

	private HttpInputStream in;

	private HttpOutputStream out;

	private Session session;

	private String key;

	private String host;

	private int port;

	/**
	 * The application's main method. Starts listening on a socket and creates a new instance
	 * of the Server class for each socket connection accepted.
	 */
	public static void main(String[] args) throws Exception {
		current = new HashMap();

		int port = 8088;

		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}

		System.out.println("Starting MidpSSH HTTP Proxy on port " + port);

		ServerSocket ss = new ServerSocket(port);

		while (true) {
			Socket s = ss.accept();
			new Server(s);
		}
	}

	public Server(Socket socket) {
		this.clientSocket = socket;
		new Thread(this).start();
	}

	/**
	 * The main server loop. A new thread is created for each socket handled by the server. This
	 * method handles that socket and exists until the socket is closed.
	 */
	public void run() {
		debug("Connection from " + clientSocket);
		try {
			in = new HttpInputStream(clientSocket.getInputStream());
			out = new HttpOutputStream(clientSocket.getOutputStream());

//			boolean keptAlive = false;
			while (true) {
//				if (keptAlive) {
//					System.out.println("trying a keep alive read");
//				}
				HttpFields fields = new HttpFields();
				fields.read(in);
//				if (keptAlive) {
//					System.out.println("kept alive!");
//				}

				String firstLine = fields.getFirstLine();
				if (firstLine != null) {
					StringTokenizer tok = new StringTokenizer(firstLine, " ");
					String method = tok.nextToken();
					String path = tok.nextToken();

					/* Determine the key that uniquely identifies the connection */
					key = path;
					int i = key.indexOf('/', 1);
					host = key.substring(i + 1);

					i = host.indexOf(':');
					port = Integer.parseInt(host.substring(i + 1));
					host = host.substring(0, i);

					/* Look for that connection in our current list and use that if possible. Otherwise this
					 * is a new connection.
					 */
					synchronized (current) {
						if (current.containsKey(key)) {
							session = (Session) current.get(key);
							session.setLastAccessed(System.currentTimeMillis());
						} else {
							session = new Session();
							current.put(key, session);
						}
					}
					
					synchronized (session) {
						if (!session.isConnected()) {
							session.connect(host, port);
						}
					}

					try {
						if (method.equalsIgnoreCase("POST")) {
							doClientOutbound(fields);
						} else {
							doClientInbound(fields);
						}
					}
					catch (IOException e) {
						/* Remove this connection from the current list */
						current.remove(key);
						session.close();
						throw e;
					}

					// keptAlive = true;
					break;
				} else {
					break;
				}
			}

			in.close();
			out.close();
		} catch (IOException e) {
			System.out.println(e);
		}

		try {
			clientSocket.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	/**
	 * Handle the outbound stream from MidpSSH. Can support chunked POST data, in which case MidpSSH may
	 * be able to send all outbound data over a single chunked POST. Otherwise multiple and separate POSTs
	 * are sent.
	 * 
	 * We read all the data from the post and write it out to the destination server.
	 * @param fields
	 * @throws IOException
	 */
	public void doClientOutbound(HttpFields fields) throws IOException {
		OutputStream destout = session.getSocket().getOutputStream();

		InputStream in = this.in;
		boolean chunking = fields.getField("Transfer-Encoding") != null && fields.getField("Transfer-Encoding").equalsIgnoreCase("chunked");
		if (chunking) {
			in = new ChunkedInputStream(in);
		} else {
			int contentLength = fields.getIntField("Content-length");
			in = new FiniteInputStream(in, contentLength);
		}

		byte[] buf = new byte[8192];
		int read = in.read(buf);
		while (read != -1) {
			destout.write(buf, 0, read);
			destout.flush();

			read = in.read(buf);
		}

		out.write(HTTP_HEADERS + CRLF);
		out.flush();
	}

	/**
	 * Handle the inbound stream to MidpSSH. Can support persistent connection to MidpSSH if the phone + network
	 * can support it: the entire inbound stream is sent during one connection. Otherwise we send back what we have
	 * and then close the connection.
	 * @param fields
	 * @throws IOException
	 */
	public void doClientInbound(HttpFields fields) throws IOException {
		InputStream destin = session.getSocket().getInputStream();

		boolean persistent = fields.get("X-MidpSSH-Persistent") != null;
		boolean headers = false;

		byte[] buf = new byte[8192];
		int read = destin.read(buf);
		while (read != -1) {
			if (!headers) {
				if (persistent) {
					out.write(HTTP_HEADERS + CRLF);
				} else {
					out.write(HTTP_HEADERS + "Content-length: " + read + CRLF + CRLF);
				}
				out.flush();
				headers = true;
			}
			out.write(buf, 0, read);
			out.flush();
			if (persistent) {
				read = destin.read(buf);
			} else {
				break;
			}
		}
		
		if (read == -1) {
			throw new EOFException("Remote server has closed connection");
		}
	}

//	private String toHex(byte[] ray, int offset, int length) {
//		StringBuffer buf = new StringBuffer();
//		for (int i = offset; i < offset + length; i++) {
//			buf.append("0x");
//			buf.append(Integer.toHexString(ray[i]));
//			buf.append(' ');
//		}
//		buf.append('\n');
//		buf.append(new String(ray, offset, length));
//		return buf.toString();
//	}
	
	private void debug(String message) {
		System.out.println(message);
	}

}
