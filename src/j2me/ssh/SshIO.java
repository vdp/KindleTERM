/*
 * This file is part of "The Java Telnet Application".
 *
 * (c) Matthias L. Jugel, Marcus Mei�ner 1996-2002. All Rights Reserved.
 * The file was changed by Radek Polak to work as midlet in MIDP 1.0
 * 
 * This file has been modified by Karl von Randow for MidpSSH.
 *
 * Please visit http://javatelnet.org/ for updates and contact.
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
 */

package ssh;

//import app.Main;
import java.io.IOException;
import java.util.Random;

import ssh.v1.BigInteger;
import ssh.v1.Cipher;
import ssh.v1.MD5;
import ssh.v1.SshCrypto;
import ssh.v1.SshPacket1;
import ssh.v2.DHKeyExchange;
import ssh.v2.PublicKeyAuthentication;
import ssh.v2.SHA1Digest;
import ssh.v2.SshCrypto2;
import ssh.v2.SshPacket2;
//#ifndef noj2me
import app.Settings;
//#endif
import app.session.ISshSession;
//import app.session.SshSession;

/**
 * Secure Shell IO
 * 
 * @author Marcus Meissner
 * @version $Id: SshIO.java 517 2008-03-14 04:17:55Z karlvr $
 */
public class SshIO {

	private static MD5 md5 = new MD5();
	
	private ISshSession sshSession;

	/**
	 * variables for the connection
	 */
	private String idstr = ""; // ("SSH-<protocolmajor>.<protocolminor>-<version>\n")

	private String idstr_sent = "SSH/MidpSSH\n";

	/**
	 * Debug level. This results in additional diagnostic messages on the java
	 * console.
	 */
	// private static int debug = 0;
	/**
	 * State variable for Ssh negotiation reader
	 */
	//#ifndef nossh1
	private SshCrypto crypto;
	//#endif
	
	//#ifdef ssh2
	private SshCrypto2 crypto2;
	
	private int remoteId;
	//#endif
	
	String cipher_type;// = "IDEA";

	private static java.util.Random rnd = new java.util.Random();

	private int remotemajor, remoteminor;

	private int mymajor, myminor;

	private int useprotocol;

	public String login, password;

	// nobody is to access those fields : better to use pivate, nobody knows :-)

	private String dataToSend = null;

	private byte lastPacketSentType;

	// phase : handleBytes
	private byte state;
	
	private static final byte STATE_INIT = 0;
	
	private static final byte STATE_KEYS = 1;
	
	private static final byte STATE_SECURE = 2;
	
	private static final byte STATE_CONNECTED = 3;
	
	private int authmode;
	
	public boolean usepublickey = false;
	
	private static final byte MODE_PUBLICKEY = 1;
	
	private static final byte MODE_PASSWORD = 2;
	
	//#ifdef keybrdinteractive
	private static final byte MODE_KEYBOARD_INTERACTIVE = 3;
	//#endif
	
	// handlePacket
	// messages
	// The supported packet types and the corresponding message numbers are
	// given in the following table. Messages with _MSG_ in their name may
	// be sent by either side. Messages with _CMSG_ are only sent by the
	// client, and messages with _SMSG_ only by the server.
	//
//	private static final byte SSH_MSG_NONE = 0;

	private static final byte SSH_MSG_DISCONNECT = 1;

	private static final byte SSH_SMSG_PUBLIC_KEY = 2;

	private static final byte SSH_CMSG_SESSION_KEY = 3;

	private static final byte SSH_CMSG_USER = 4;

	private static final byte SSH_CMSG_AUTH_PASSWORD = 9;

	private static final byte SSH_CMSG_REQUEST_PTY = 10;

	private static final byte SSH_CMSG_EXEC_SHELL = 12;

	private static final byte SSH_SMSG_SUCCESS = 14;

	private static final byte SSH_SMSG_FAILURE = 15;

	private static final byte SSH_CMSG_STDIN_DATA = 16;

	private static final byte SSH_SMSG_STDOUT_DATA = 17;

	private static final byte SSH_SMSG_STDERR_DATA = 18;

	private static final byte SSH_SMSG_EXITSTATUS = 20;

	private static final byte SSH_MSG_IGNORE = 32;

	private static final byte SSH_CMSG_EXIT_CONFIRMATION = 33;

//	private static final byte SSH_MSG_DEBUG = 36;

	/* SSH v2 stuff */

	private static final byte SSH2_MSG_DISCONNECT = 1;

	private static final byte SSH2_MSG_IGNORE = 2;

//	private static final byte SSH2_MSG_UNIMPLEMENTED = 3;

	private static final byte SSH2_MSG_SERVICE_REQUEST = 5;

	private static final byte SSH2_MSG_SERVICE_ACCEPT = 6;

	private static final byte SSH2_MSG_KEXINIT = 20;

	private static final byte SSH2_MSG_NEWKEYS = 21;

	private static final byte SSH2_MSG_KEXDH_INIT = 30;

	private static final byte SSH2_MSG_KEXDH_REPLY = 31;

	private static final byte SSH2_MSG_USERAUTH_REQUEST = 50;

	private static final byte SSH2_MSG_USERAUTH_FAILURE = 51;

	private static final byte SSH2_MSG_USERAUTH_SUCCESS = 52;

//	private static final byte SSH2_MSG_USERAUTH_BANNER = 53;

//	private static final byte SSH2_MSG_USERAUTH_PK_OK = 60;
	
	private static final byte SSH2_MSG_USERAUTH_INFO_REQUEST = 60;
	
	private static final byte SSH2_MSG_USERAUTH_INFO_RESPONSE = 61;

	private static final byte SSH2_MSG_CHANNEL_OPEN = 90;

	private static final byte SSH2_MSG_CHANNEL_OPEN_CONFIRMATION = 91;

	// private static final byte SSH2_MSG_CHANNEL_OPEN_FAILURE = 92;

//	private static final byte SSH2_MSG_CHANNEL_WINDOW_ADJUST = 93;

	private static final byte SSH2_MSG_CHANNEL_DATA = 94;

	// private static final byte SSH2_MSG_CHANNEL_EXTENDED_DATA = 95;

//	private static final byte SSH2_MSG_CHANNEL_EOF = 96;

	private static final byte SSH2_MSG_CHANNEL_CLOSE = 97;

	private static final byte SSH2_MSG_CHANNEL_REQUEST = 98;

	// private static final byte SSH2_MSG_CHANNEL_SUCCESS = 99;

	// private static final byte SSH2_MSG_CHANNEL_FAILURE = 100;
	
	private int outgoingseq = 0;

	//
	// encryption types
	//
	// private static final int SSH_CIPHER_NONE = 0; // No encryption

	private static final int SSH_CIPHER_IDEA = 1; // IDEA in CFB mode (patented)

	private static final int SSH_CIPHER_DES = 2; // DES in CBC mode

	private static final int SSH_CIPHER_3DES = 3; // Triple-DES in CBC mode

	// private static final int SSH_CIPHER_TSS = 4; // An experimental stream cipher

	// private static final int SSH_CIPHER_RC4 = 5; // RC4 (patented)

	private static final int SSH_CIPHER_BLOWFISH = 6; // Bruce Scheiers blowfish (public

	// d)

	//
	// authentication methods
	//
	// private final int SSH_AUTH_RHOSTS = 1; // .rhosts or /etc/hosts.equiv

	// private final int SSH_AUTH_RSA = 2; // pure RSA authentication

	// private final int SSH_AUTH_PASSWORD = 3; // password authentication,

	// implemented !

	// private final int SSH_AUTH_RHOSTS_RSA = 4; // .rhosts with RSA host

	// authentication

	/**
	 * Initialise SshIO
	 */
	public SshIO(ISshSession sshSession) {
		this.sshSession = sshSession;
	}

	SshPacket currentpacket;

	/** write data to our back end */
	public void write(byte[] b) throws IOException {
		sshSession.sendData(b);
	}

	protected void sendDisconnect(int reason, String reasonStr)
			throws IOException {
		//#ifdef ssh2
		if (useprotocol == 2) {
			SshPacket2 pn = new SshPacket2(SSH2_MSG_DISCONNECT);
			pn.putInt32(reason);
			pn.putString(reasonStr);
			pn.putString("en");
			sendPacket2(pn);
		}
		//#endif
		//#ifndef nossh1
		if (useprotocol == 1) {
			SshPacket1 pn = new SshPacket1(SSH_MSG_DISCONNECT);
			pn.putInt32(reason);
			pn.putString(reasonStr);
			pn.putString("en");
			sendPacket1(pn);
		}
		//#endif
	}

	public void sendData(byte[] data, int offset, int length)
			throws IOException {
		String str = new String(data, offset, length);
		// if (debug > 1) System.out.println("SshIO.send(" + str + ")");
		if (dataToSend == null)
			dataToSend = str;
		else
			dataToSend += str;
		if (state == STATE_CONNECTED) {
			//#ifdef ssh2
			if (useprotocol == 2) {
				SshPacket2 pn = new SshPacket2(SSH2_MSG_CHANNEL_DATA);
				pn.putInt32(remoteId);
				pn.putString(dataToSend);
				sendPacket2(pn);
			}
			//#endif
			//#ifndef nossh1
			if (useprotocol == 1) {
				Send_SSH_CMSG_STDIN_DATA(dataToSend);
			}
			//#endif
			dataToSend = null;
		}
	}

	/**
	 * Read data from the remote host. Blocks until data is available.
	 * 
	 * Returns an array of bytes that will be displayed.
	 * 
	 */
	public byte[] handleSSH(byte buff[], int boffset, int length)
			throws IOException {
		String result;
		int boffsetend = boffset + length;

		// if (debug > 1)
		// Telnet.console.append("SshIO.getPacket(" + buff + "," + length +
		// ")");

		PHASE_INIT: if (state == STATE_INIT) {
			byte b; // of course, byte is a signed entity (-128 -> 127)

			while (boffset < boffsetend) {
				b = buff[boffset++];
				// both sides MUST send an identification string of the form
				// "SSH-protoversion-softwareversion comments",
				// followed by newline character(ascii 10 = '\n' or '\r')
				idstr += (char) b;
				if (b == '\n') {
					if (idstr.startsWith("SSH-")) {
						remotemajor = Integer.parseInt(idstr.substring(4, 5));
						String minorverstr = idstr.substring(6, 8);
						if (!Character.isDigit(minorverstr.charAt(1)))
							minorverstr = minorverstr.substring(0, 1);
						remoteminor = Integer.parseInt(minorverstr);
	
						//#ifdef ssh2
						if (remotemajor == 2) {
							mymajor = 2;
							myminor = 0;
							useprotocol = 2;
						} else {
							//#ifndef nossh1
                                                        //#ifndef noj2me
							/*
							 * Check if we have discretion over whether to use ssh1
							 * or ssh2
							 */
							if (remoteminor == 99
									&& Settings.sshVersionPreferred == 2) {
								mymajor = 2;
								myminor = 0;
								useprotocol = 2;
							} else {
								mymajor = 1;
								myminor = 5;
								useprotocol = 1;
							}
							//#else
							if (remoteminor == 99) {
								mymajor = 2;
								myminor = 0;
								useprotocol = 2;
							}
							else {
								return "Server requires SSH1.\r\n".getBytes();
							}
                                                        //#endif
							//#endif
						}
						//#else
						if (remotemajor == 2) {
							// TODO disconnect
							return "Server requires SSH2.\r\n".getBytes();
						} else {
							mymajor = 1;
							myminor = 5;
							useprotocol = 1;
						}
						//#endif
						// this is how we tell the remote server what protocol
						// we
						// use.
						idstr_sent = "SSH-" + mymajor + "." + myminor + "-"
								+ idstr_sent;
						write(idstr_sent.getBytes());

						state = STATE_KEYS;
						
						//#ifdef ssh2
						if (useprotocol == 2) {
							currentpacket = new SshPacket2();
						}
						//#ifndef nossh1
						else {
							currentpacket = new SshPacket1();
						}
						//#endif
						//#else
						currentpacket = new SshPacket1();
						//#endif

                                                //Main.printStack("Phase init end", 0);
						
						break PHASE_INIT;
					}
					else {
						/* Lines sent during init that do not start SSH- should be ignored
						 * http://www.ietf.org/internet-drafts/draft-ietf-secsh-transport-24.txt
						 * section 4.2
						 */
						idstr = "";
					}
				}
			}
			if (boffset == boffsetend)
				return "".getBytes();
			return "Init Error\n".getBytes();
		}

		result = "";
		while (boffset < boffsetend) {
			boffset = currentpacket.addPayload(buff, boffset,
					(boffsetend - boffset));
			if (currentpacket.isFinished()) {
				//#ifdef ssh2
				if (useprotocol == 2) {
					result = result + handlePacket2((SshPacket2) currentpacket);
					currentpacket = new SshPacket2(crypto2);
				}
				//#endif
				//#ifndef nossh1
				if (useprotocol == 1) {
					result = result + handlePacket1((SshPacket1) currentpacket);
					currentpacket = new SshPacket1(crypto);
				}
				//#endif
			}
		}
		return result.getBytes();
	}

	/**
	 * Given a host key return the finger print string for that key.
	 * 
	 * @param host_key
	 * @return
	 */
	private String fingerprint(byte[] host_key) {
		byte[] fprint = md5.digest(host_key);
		StringBuffer buf = new StringBuffer();
		int n = fprint.length;
		for (int i = 0; i < n; i++) {
			int j = fprint[i] & 0xff;
			String hex = Integer.toHexString(j);
			if (hex.length() == 1) {
				buf.append('0');
			}
			buf.append(hex);
			if (i + 1 < n)
				buf.append(':');
		}
		return buf.toString();
	}

	//#ifdef ssh2
	/**
	 * Handle SSH protocol Version 2
	 * 
	 * @param p
	 *            the packet we will process here.
	 * @return a array of bytes
	 */
	private String handlePacket2(SshPacket2 p) throws IOException {
		switch (p.getType()) {
		
		case SSH2_MSG_KEXINIT: {
			/*
			 * Described
			 * http://www.ietf.org/rfc/rfc4253.txt
			 * Section 7.1
			 */
//			p.getBytes(16); // cookie
//			p.getString(); // kex_algorithms
//			p.getString(); // server_host_key_algorithms
//			p.getString(); // encryption_algorithms_client_to_server
//			p.getString(); // encryption_algorithms_server_to_client
//			p.getString(); // mac_algorithms_client_to_server
//			p.getString(); // mac_algorithms_server_to_client
//			p.getString(); // compression_algorithms_client_to_server
//			p.getString(); // compression_algorithms_server_to_client
//			p.getString(); // languages_client_to_server
//			p.getString(); // languages_server_to_client
//			p.getBytes(1); // first_kex_packet_follows

			SshPacket2 pn = new SshPacket2(SSH2_MSG_KEXINIT);
			byte[] kexsend = new byte[16];
			Random random = new Random();
			for (int i = 0; i < kexsend.length; i++) {
				kexsend[i] = (byte) random.nextInt();
			}
			String ciphername;
			pn.putBytes(kexsend);
			pn.putString("diffie-hellman-group1-sha1");
			pn.putString(DHKeyExchange.SSH_DSS);

			cipher_type = "DES3";
			ciphername = "3des-cbc";

			pn.putString(ciphername);
			pn.putString(ciphername);
			pn.putString("hmac-sha1");
			pn.putString("hmac-sha1");
			pn.putString("none");
			pn.putString("none");
			pn.putString("");
			pn.putString("");
			pn.putByte((byte) 0);
			pn.putInt32(0);

			byte[] I_C = pn.getData();
			sendPacket2(pn);

                        //#ifndef noj2me
			if (Settings.ssh2StoreKey) {
				if (Settings.ssh2x == null || Settings.ssh2y == null) {
					byte[][] keys = DHKeyExchange
							.generateKeyPairBytes(Settings.ssh2KeySize);
					Settings.ssh2x = keys[0];
					Settings.ssh2y = keys[1];
					Settings.saveSettings();
				}

				dhkex = new DHKeyExchange(Settings.ssh2x, Settings.ssh2y);
			} else {
				dhkex = new DHKeyExchange(Settings.ssh2KeySize);
			}
                        //#else
                        dhkex = new DHKeyExchange(512);
                        //#endif

			dhkex.V_S = idstr.trim().getBytes();
			dhkex.V_C = idstr_sent.trim().getBytes();
			dhkex.I_S = add20(p.getData());
			dhkex.I_C = add20(I_C);
			
			pn = new SshPacket2(SSH2_MSG_KEXDH_INIT);
			pn.putMpInt(dhkex.getE());
			sendPacket2(pn);

                        //Main.printStack("DH end", state);

			return "Negotiating...";
		}

		case SSH2_MSG_KEXDH_REPLY: {
			byte[] K_S = p.getByteString();
			// System.out.println( "K_S=" + K_S );
			byte[] dhserverpub = p.getMpInt();
			// result += "DH Server Pub: " + dhserverpub + "\n\r";

			byte[] sig_of_h = p.getByteString();

			boolean ok = dhkex.next(K_S, dhserverpub, sig_of_h);
			if (ok) {
				// TODO handle fingerprint better
				return "OK\r\n" + dhkex.keyalg + " " + fingerprint(K_S)
						+ "\r\n";
			} else {
				sendDisconnect(3, "Key exchange failed");
				return "FAILED\r\n";
			}
		}

		case SSH2_MSG_NEWKEYS: {
			// Send response
			sendPacket2(new SshPacket2(SSH2_MSG_NEWKEYS));

			// byte[] session_key = new byte[24];
			// crypto = new SshCrypto( cipher_type, session_key );
			updateKeys(dhkex);
			
			state = STATE_SECURE;

			SshPacket2 pn = new SshPacket2(SSH2_MSG_SERVICE_REQUEST);
			pn.putString("ssh-userauth");

			sendPacket2(pn);

			//#ifndef noinstructions
			//#ifdef removeme
			if (1 == 1)
				//#endif
				return "Requesting authentication\r\n";
			//#else
			break;
			//#endif
		}
		
		case SSH2_MSG_SERVICE_ACCEPT:
			if (state < STATE_SECURE)
				break;
			return authenticate2();

		case SSH2_MSG_USERAUTH_FAILURE:
			if (state < STATE_SECURE)
				break;
//			p.getString(); // methods
//			p.getByte(); // partialSuccess

			String message = authenticate2();
			if (message != null) {
				return message;
			} else {
				return "Authentication failed.\r\nAvailable methods are: "
				+ p.getString() + "\r\n";
			}
			
		//#ifdef keybrdinteractive
		case SSH2_MSG_USERAUTH_INFO_REQUEST:
			if (state < STATE_SECURE)
				break;
			String name = p.getString();
			String instruction = p.getString();
			p.getString(); // language tag
			int numPrompts = p.getInt32();
			String[] prompts = new String[numPrompts];
			boolean[] echos = new boolean[numPrompts];
			
			for (int i = 0; i < numPrompts; i++) {
				prompts[i] = p.getString();
				echos[i] = p.getByte() != 0;
			}
			
			sshSession.prompt(name, instruction, prompts, echos);
			break;
		//#endif

		case SSH2_MSG_USERAUTH_SUCCESS: {
			// Open channel
			SshPacket2 pn = new SshPacket2(SSH2_MSG_CHANNEL_OPEN);
			pn.putString("session");
			pn.putInt32(0);
			pn.putInt32(0x100000);
			pn.putInt32(0x4000);
			sendPacket2(pn);

			//#ifndef noinstructions
			//#ifdef removeme
			if (1 == 1)
				//#endif
				return "Authentication accepted\r\n";
			//#else
			break;
			//#endif
		}
		
		case SSH2_MSG_CHANNEL_OPEN_CONFIRMATION: {
			p.getInt32(); // localId
			remoteId = p.getInt32();
//			p.getInt32(); // remoteWindowSize
//			p.getInt32(); // remotePacketSize

			// Open PTY
			SshPacket2 pn = new SshPacket2(SSH2_MSG_CHANNEL_REQUEST);
			pn.putInt32(remoteId);
			pn.putString("pty-req");
			pn.putByte((byte) 0); // want reply
			pn.putString(getTerminalID());
			pn.putInt32(getTerminalWidth());
			pn.putInt32(getTerminalHeight());
			pn.putInt32(0);
			pn.putInt32(0);
			pn.putString("");
			sendPacket2(pn);
				
			// Open Shell
			pn = new SshPacket2(SSH2_MSG_CHANNEL_REQUEST);
			pn.putInt32(remoteId);
			pn.putString("shell");
			pn.putByte((byte) 0); // want reply
			sendPacket2(pn);

			state = STATE_CONNECTED;
			
			if (dataToSend != null) {
			pn = new SshPacket2(SSH2_MSG_CHANNEL_DATA);
				pn.putInt32(0);
				pn.putString(dataToSend);
				sendPacket2(pn);
				dataToSend = null;
			}

			//#ifndef noinstructions
			//#ifdef removeme
			if (1 == 1)
				//#endif
				return "Shell opened\r\n";
			//#else
			break;
			//#endif
		}

		case SSH2_MSG_CHANNEL_DATA: {
			p.getInt32(); // localId
			String data = p.getString();
			return data;
		}

		case SSH2_MSG_CHANNEL_CLOSE: {
			sendDisconnect(11, "Finished");
			break;
		}
		
		case SSH2_MSG_DISCONNECT:
			p.getInt32(); // disconnect reason
			String discreason1 = p.getString();
			return "\r\nDisconnected: " + discreason1 + "\r\n";
			
		}
		return "";
	}
	
	//#ifdef keybrdinteractive
	public void sendUserauthInfoResponse(String[] responses) throws IOException {
		SshPacket2 buf = new SshPacket2(SSH2_MSG_USERAUTH_INFO_RESPONSE);
		buf.putInt32(responses.length);
		for (int i = 0; i < responses.length; i++) {
			buf.putString(responses[i]);
		}
		sendPacket2(buf);
	}
	//#endif
	
	private String authenticate2() throws IOException {
		SshPacket2 buf = new SshPacket2(SSH2_MSG_USERAUTH_REQUEST);
		buf.putString(login);
		buf.putString("ssh-connection");
                //#ifndef noj2me
		if (usepublickey && Settings.x != null && authmode < MODE_PUBLICKEY) {
			/* Try publickey */
			authmode = MODE_PUBLICKEY;
			
			PublicKeyAuthentication kg = new PublicKeyAuthentication();
			
			buf.putString("publickey");
			buf.putByte((byte) 1);
			buf.putString(DHKeyExchange.SSH_DSS);
			buf.putString(kg.getPublicKeyBlob());
			byte[] sig = kg.sign(session_id, buf.getData());
			buf.putString(sig);
			sendPacket2(buf);

			//#ifndef noinstructions
			//#ifdef removeme
			if (1 == 1)
				//#endif
				return "Sent publickey\r\n";
			//#else
			return "";
			//#endif
		}
		else if (authmode < MODE_PASSWORD) {
                //#endif
			/* Do password auth */
			authmode = MODE_PASSWORD;
			
			buf.putString("password");
			buf.putByte((byte) 0);
			buf.putString(password);
			sendPacket2(buf);

                        //Main.printStack("Auth mode is PASSWORD", 0);
			
			//#ifndef noinstructions
			//#ifdef removeme
			if (1 == 1)
				//#endif
				return "Sent password\r\n";
			//#else
			return "";
			//#endif

                //#ifndef noj2me
		}
		//#ifdef keybrdinteractive
		else if (authmode < MODE_KEYBOARD_INTERACTIVE) {
			/* Attempt keyboard-interactive auth */
			authmode = MODE_KEYBOARD_INTERACTIVE;
			
			buf.putString("keyboard-interactive");
			buf.putString("");
			buf.putString("");
			sendPacket2(buf);

			//#ifndef noinstructions
			//#ifdef removeme
			if (1 == 1)
				//#endif
				return "Start keyboard-interactive\r\n";
			//#else
			return "";
			//#endif
		}
		//#endif
		else {
			return null;
		}
                //#endif
	}

	private void sendPacket2(SshPacket2 packet) throws IOException {
		write(packet.getPayLoad(crypto2, outgoingseq));
		outgoingseq++;
		lastPacketSentType = packet.getType();
	}

	private DHKeyExchange dhkex;

	private byte[] session_id;

	private void updateKeys(DHKeyExchange kex) {
		byte[] K = kex.K;
		byte[] H = kex.H;
		SHA1Digest hash = new SHA1Digest();

		if (session_id == null) {
			session_id = new byte[H.length];
			System.arraycopy(H, 0, session_id, 0, H.length);
		}

		/*
		 * Initial IV client to server: HASH (K || H || "A" || session_id)
		 * Initial IV server to client: HASH (K || H || "B" || session_id)
		 * Encryption key client to server: HASH (K || H || "C" || session_id)
		 * Encryption key server to client: HASH (K || H || "D" || session_id)
		 * Integrity key client to server: HASH (K || H || "E" || session_id)
		 * Integrity key server to client: HASH (K || H || "F" || session_id)
		 */

		SshPacket2 buf = new SshPacket2();
		buf.putMpInt(K);
		buf.putBytes(H);
		buf.putByte((byte) 0x41);
		buf.putBytes(session_id);
		byte[] b = buf.getData();

		hash.update(b, 0, b.length);
		byte[] IVc2s = new byte[hash.getDigestSize()];
		hash.doFinal(IVc2s, 0);

		int j = b.length - session_id.length - 1;

		b[j]++;
		hash.update(b, 0, b.length);
		byte[] IVs2c = new byte[hash.getDigestSize()];
		hash.doFinal(IVs2c, 0);

		b[j]++;
		hash.update(b, 0, b.length);
		byte[] Ec2s = new byte[hash.getDigestSize()];
		hash.doFinal(Ec2s, 0);

		b[j]++;
		hash.update(b, 0, b.length);
		byte[] Es2c = new byte[hash.getDigestSize()];
		hash.doFinal(Es2c, 0);

		b[j]++;
		hash.update(b, 0, b.length);
		byte[] MACc2s = new byte[hash.getDigestSize()];
		hash.doFinal(MACc2s, 0);

		b[j]++;
		hash.update(b, 0, b.length);
		byte[] MACs2c = new byte[hash.getDigestSize()];
		hash.doFinal(MACs2c, 0);

		int keySize = 24;

		while (keySize > Es2c.length) {
			buf = new SshPacket2();
			buf.putMpInt(K);
			buf.putBytes(H);
			buf.putBytes(Es2c);
			b = buf.getData();

			hash.update(b, 0, b.length);
			byte[] foo = new byte[hash.getDigestSize()];
			hash.doFinal(foo, 0);
			byte[] bar = new byte[Es2c.length + foo.length];
			System.arraycopy(Es2c, 0, bar, 0, Es2c.length);
			System.arraycopy(foo, 0, bar, Es2c.length, foo.length);
			Es2c = bar;
		}
		while (keySize > Ec2s.length) {
			buf = new SshPacket2();
			buf.putMpInt(K);
			buf.putBytes(H);
			buf.putBytes(Ec2s);
			b = buf.getData();

			hash.update(b, 0, b.length);
			byte[] foo = new byte[hash.getDigestSize()];
			hash.doFinal(foo, 0);
			byte[] bar = new byte[Ec2s.length + foo.length];
			System.arraycopy(Ec2s, 0, bar, 0, Ec2s.length);
			System.arraycopy(foo, 0, bar, Ec2s.length, foo.length);
			Ec2s = bar;
		}

		crypto2 = new SshCrypto2(IVc2s, IVs2c, Ec2s, Es2c, MACc2s, MACs2c);
	}

	private byte[] add20(byte[] in) {
		byte[] out = new byte[in.length + 1];
		out[0] = 20;
		System.arraycopy(in, 0, out, 1, in.length);
		return out;
	}

	//#endif

	//#ifndef nossh1
	


	private String handlePacket1(SshPacket1 p) throws IOException { // the
		// message
		// to handle
		// is data
		// and its
		// length is

		// we have to deal with data....

		// if (debug > 0)
		// System.out.println("1 packet to handle, type " + p.getType());

		switch (p.getType()) {

		case SSH_MSG_DISCONNECT:
			return p.getString();

		case SSH_SMSG_PUBLIC_KEY:
			byte[] anti_spoofing_cookie; // 8 bytes
			byte[] server_key_public_exponent; // mp-int
			byte[] server_key_public_modulus; // mp-int
			byte[] host_key_public_exponent; // mp-int
			byte[] host_key_public_modulus; // mp-int
			byte[] supported_ciphers_mask; // 32-bit int

			anti_spoofing_cookie = p.getBytes(8);
			p.getBytes(4); // server_key_bits
			server_key_public_exponent = p.getMpInt();
			server_key_public_modulus = p.getMpInt();
			p.getBytes(4); // host_key_bits
			host_key_public_exponent = p.getMpInt();
			host_key_public_modulus = p.getMpInt();
			p.getBytes(4); // protocol_flags
			supported_ciphers_mask = p.getBytes(4);
			p.getBytes(4); // supported_authentications_mask

			// We have completely received the PUBLIC_KEY
			// We prepare the answer ...

			String ret = Send_SSH_CMSG_SESSION_KEY(anti_spoofing_cookie,
					server_key_public_modulus, host_key_public_modulus,
					supported_ciphers_mask, server_key_public_exponent,
					host_key_public_exponent);
			if (ret != null)
				return ret;

			// TODO prompt user to confirm fingerprint
			byte[] host_key_combined = new byte[host_key_public_exponent.length
					+ host_key_public_modulus.length];
			System.arraycopy(host_key_public_modulus, 0, host_key_combined, 0,
					host_key_public_modulus.length);
			System.arraycopy(server_key_public_exponent, 0, host_key_combined,
					host_key_public_modulus.length,
					server_key_public_exponent.length);
			String fingerprint = fingerprint(host_key_combined);
			return fingerprint + "\r\n";

		// break;

		case SSH_SMSG_SUCCESS:
			// if (debug > 0)
			// System.out.println("SSH_SMSG_SUCCESS (last packet was " +
			// lastPacketSentType + ")");
			if (lastPacketSentType == SSH_CMSG_SESSION_KEY) {
				// we have succefully sent the session key !! (at last :-) )
				Send_SSH_CMSG_USER();
				break;
			}

			if (lastPacketSentType == SSH_CMSG_USER) {
				// authentication is NOT needed for this user
				Send_SSH_CMSG_REQUEST_PTY(); // request a pseudo-terminal
				return "Empty password login.\r\n";
			}

			if (lastPacketSentType == SSH_CMSG_AUTH_PASSWORD) {// password
				// correct !!!
				// yahoo
				// if (debug > 0)
				// System.out.println("login succesful");

				// now we have to start the interactive session ...
				Send_SSH_CMSG_REQUEST_PTY(); // request a pseudo-terminal
				return "Login & password accepted\r\n";
			}

			if (lastPacketSentType == SSH_CMSG_REQUEST_PTY) {// pty
				// accepted
				// !!
				/*
				 * we can send data with a pty accepted ... no need for a shell.
				 */
				state = STATE_CONNECTED;
				
				if (dataToSend != null) {
					Send_SSH_CMSG_STDIN_DATA(dataToSend);
					dataToSend = null;
				}
				Send_SSH_CMSG_EXEC_SHELL(); // we start a shell
				break;
			}
			if (lastPacketSentType == SSH_CMSG_EXEC_SHELL) {// shell is
				// running
				// ...
				/* empty */
			}

			break;

		case SSH_SMSG_FAILURE:
			if (lastPacketSentType == SSH_CMSG_AUTH_PASSWORD) {// password
				// incorrect ???
				// System.out.println("failed to log in");
				return "Login & password not accepted\r\n";
			}
			if (lastPacketSentType == SSH_CMSG_USER) {
				// authentication is needed for the given user
				// (in most cases that's true)
				Send_SSH_CMSG_AUTH_PASSWORD();
				break;
			}

			if (lastPacketSentType == SSH_CMSG_REQUEST_PTY) {// pty not
				// accepted
				// !!
				break;
			}
			break;

		case SSH_SMSG_STDOUT_DATA: // receive some data from the server
			return p.getString();

		case SSH_SMSG_STDERR_DATA: // receive some error data from the
			return "Error : " + p.getString();

		case SSH_SMSG_EXITSTATUS: // sent by the server to indicate that
			// the client program has terminated.
			// 32-bit int exit status of the command
			p.getInt32();
			Send_SSH_CMSG_EXIT_CONFIRMATION();
			// System.out.println("SshIO : Exit status " + value);
			break;

		}
		return "";
	} // handlePacket

	private void sendPacket1(SshPacket1 packet) throws IOException {
		write(packet.getPayLoad(crypto));
		lastPacketSentType = packet.getType();
	}
	
	//
	// Send_SSH_CMSG_SESSION_KEY
	// Create :
	// the session_id,
	// the session_key,
	// the Xored session_key,
	// the double_encrypted session key
	// send SSH_CMSG_SESSION_KEY
	// Turn the encryption on (initialise the block cipher)
	//

	private String Send_SSH_CMSG_SESSION_KEY(byte[] anti_spoofing_cookie,
			byte[] server_key_public_modulus, byte[] host_key_public_modulus,
			byte[] supported_ciphers_mask, byte[] server_key_public_exponent,
			byte[] host_key_public_exponent) throws IOException {

		byte cipher_types; // encryption types
		byte[] session_key; // mp-int

		// create the session id
		// session_id = md5(hostkey->n || servkey->n || cookie) //protocol V
		// 1.5. (we use this one)
		// session_id = md5(servkey->n || hostkey->n || cookie) //protocol V
		// 1.1.(Why is it different ??)
		//

		byte[] session_id_byte = new byte[host_key_public_modulus.length
				+ server_key_public_modulus.length
				+ anti_spoofing_cookie.length];

		System.arraycopy(host_key_public_modulus, 0, session_id_byte, 0,
				host_key_public_modulus.length);
		System.arraycopy(server_key_public_modulus, 0, session_id_byte,
				host_key_public_modulus.length,
				server_key_public_modulus.length);
		System.arraycopy(anti_spoofing_cookie, 0, session_id_byte,
				host_key_public_modulus.length
						+ server_key_public_modulus.length,
				anti_spoofing_cookie.length);

		byte[] hash_md5 = md5.digest(session_id_byte);

		// SSH_CMSG_SESSION_KEY : Sent by the client
		// 1 byte cipher_type (must be one of the supported values)
		// 8 bytes anti_spoofing_cookie (must match data sent by the server)
		// mp-int double-encrypted session key (uses the session-id)
		// 32-bit int protocol_flags
		//
		if ((supported_ciphers_mask[3] & (byte) (1 << SSH_CIPHER_BLOWFISH)) != 0
				&& hasCipher("Blowfish")) {
			cipher_types = (byte) SSH_CIPHER_BLOWFISH;
			cipher_type = "Blowfish";
		} else {
			if ((supported_ciphers_mask[3] & (1 << SSH_CIPHER_IDEA)) != 0
					&& hasCipher("IDEA")) {
				cipher_types = (byte) SSH_CIPHER_IDEA;
				cipher_type = "IDEA";
			} else {
				if ((supported_ciphers_mask[3] & (1 << SSH_CIPHER_3DES)) != 0
						&& hasCipher("DES3")) {
					cipher_types = (byte) SSH_CIPHER_3DES;
					cipher_type = "DES3";
				} else {
					if ((supported_ciphers_mask[3] & (1 << SSH_CIPHER_DES)) != 0
							&& hasCipher("DES")) {
						cipher_types = (byte) SSH_CIPHER_DES;
						cipher_type = "DES";
					} else {
						// System.err.println("SshIO: remote server does not
						// supported IDEA, BlowFish or 3DES, support cypher mask
						// is " + supported_ciphers_mask[3] + ".\n");
						return "\rIncompatible ciphers.\r\n";
					}
				}
			}
		}
		// if (debug > 0)
		// System.out.println("SshIO: Using " + cipher_type + "
		// blockcipher.\n");

		// anti_spoofing_cookie : the same
		// double_encrypted_session_key :
		// 32 bytes of random bits
		// Xor the 16 first bytes with the session-id
		// encrypt with the server_key_public (small) then the
		// host_key_public(big) using RSA.
		//

		// 32 bytes of random bits
		byte[] random_bits1 = new byte[16], random_bits2 = new byte[16];

		// / java.util.Date date = new java.util.Date(); ////the number of
		// milliseconds since January 1, 1970, 00:00:00 GMT.
		// Math.random() a pseudorandom double between 0.0 and 1.0.
		random_bits2 = random_bits1 =
		// md5.hash("" + Math.random() * (new java.util.Date()).getDate());
		md5.digest(("" + rnd.nextLong() * (new java.util.Date()).getTime())
				.getBytes()); // RADEK
		// -
		// zase
		// RANDOM

		random_bits1 = md5.digest(addArrayOfBytes(md5
				.digest((password + login).getBytes()), random_bits1));
		random_bits2 = md5.digest(addArrayOfBytes(md5
				.digest((password + login).getBytes()), random_bits2));

		// SecureRandom random = new java.security.SecureRandom(random_bits1);
		// //no supported by netscape :-(
		// random.nextBytes(random_bits1);
		// random.nextBytes(random_bits2);

		session_key = addArrayOfBytes(random_bits1, random_bits2);

		// Xor the 16 first bytes with the session-id
		byte[] session_keyXored = XORArrayOfBytes(random_bits1,
				hash_md5);
		session_keyXored = addArrayOfBytes(session_keyXored,
				random_bits2);

		// We encrypt now!!
		byte[] encrypted_session_key;
		/*
		 * Karl: according to SSH 1.5 protocol spec we encrypt first with the
		 * key with the shortest modulus. Usually this was the server key but
		 * some servers have bigger keys than the host key! So check here and
		 * swap around if necessary.
		 */
		if (server_key_public_modulus.length <= host_key_public_modulus.length) {
			encrypted_session_key = encrypteRSAPkcs1Twice(
					session_keyXored, server_key_public_exponent,
					server_key_public_modulus, host_key_public_exponent,
					host_key_public_modulus);
		} else {
			encrypted_session_key = encrypteRSAPkcs1Twice(
					session_keyXored, host_key_public_exponent,
					host_key_public_modulus, server_key_public_exponent,
					server_key_public_modulus);
		}

		// protocol_flags :protocol extension cf. page 18
		int protocol_flags = 0; /* currently 0 */

		SshPacket1 packet = new SshPacket1(SSH_CMSG_SESSION_KEY);
		packet.putByte((byte) cipher_types);
		packet.putBytes(anti_spoofing_cookie);
		packet.putBytes(encrypted_session_key);
		packet.putInt32(protocol_flags);
		sendPacket1(packet);
		crypto = new SshCrypto(cipher_type, session_key);
		return null;
	}
	

	static public byte[] encrypteRSAPkcs1Twice( byte[] clearData, byte[] server_key_public_exponent,
			byte[] server_key_public_modulus, byte[] host_key_public_exponent, byte[] host_key_public_modulus ) {

		// At each encryption step, a multiple-precision integer is constructed
		//
		// the integer is interpreted as a sequence of bytes, msb first;
		// the number of bytes is the number of bytes needed to represent the
		// modulus.
		//
		// cf PKCS #1: RSA Encryption Standard. Available for anonymous ftp at
		// ftp.rsa.com.
		//  The sequence of byte is as follows:
		// The most significant byte is zero.
		// The next byte contains the value 2 (stands for public-key encrypted
		// data)
		// Then, there are non zero random bytes to fill any unused space
		// a zero byte,
		// and the data to be encrypted

		byte[] EncryptionBlock; //what will be encrypted

		int offset = 0;
		EncryptionBlock = new byte[server_key_public_modulus.length];
		EncryptionBlock[0] = 0;
		EncryptionBlock[1] = 2;
		offset = 2;
		for ( int i = 2; i < ( EncryptionBlock.length - clearData.length - 1 ); i++ )
			EncryptionBlock[offset++] = getNotZeroRandomByte();
		EncryptionBlock[offset++] = 0;
		for ( int i = 0; i < clearData.length; i++ )
			EncryptionBlock[offset++] = clearData[i];

		//EncryptionBlock can be encrypted now !
		BigInteger m, e, message;
		byte[] messageByte;

		m = new BigInteger( server_key_public_modulus );
		e = new BigInteger( server_key_public_exponent );
		message = new BigInteger( EncryptionBlock );
		//      byte[] messageByteOld1 = message.toByteArray();

		message = message.modPow( e, m ); //RSA Encryption !!

		byte[] messageByteTemp = message.toByteArray(); //messageByte holds the
		// encypted data.
		//there should be no zeroes a the begining but we have to fix it (JDK
		// bug !!)
		messageByte = new byte[server_key_public_modulus.length];
		int tempOffset = 0;
		while ( messageByteTemp[tempOffset] == 0 )
			tempOffset++;
		for ( int i = messageByte.length - messageByteTemp.length + tempOffset; i < messageByte.length; i++ )
			messageByte[i] = messageByteTemp[tempOffset++];

		// we can't check that the crypted message is OK : no way to decrypt :-(

		//according to the ssh source !!!!! Not well explained in the
		// protocol!!!
		clearData = messageByte;

		//SECOND ROUND !!

		offset = 0;
		EncryptionBlock = new byte[host_key_public_modulus.length];
		EncryptionBlock[0] = 0;
		EncryptionBlock[1] = 2;

		offset = 2;
		for ( int i = 2; i < ( EncryptionBlock.length - clearData.length - 1 ); i++ )
			EncryptionBlock[offset++] = getNotZeroRandomByte(); //random
		// !=0
		EncryptionBlock[offset++] = 0;
		for ( int i = 0; i < clearData.length; i++ )
			EncryptionBlock[offset++] = clearData[i];

		//EncryptionBlock can be encrypted now !

		m = new BigInteger( host_key_public_modulus );
		e = new BigInteger( host_key_public_exponent );
		message = new BigInteger( EncryptionBlock );

		message = message.modPow( e, m );

		messageByteTemp = message.toByteArray(); //messageByte holds the
		// encypted data.
		//there should be no zeroes a the begining but we have to fix it (JDK
		// bug !!)
		messageByte = new byte[host_key_public_modulus.length];
		tempOffset = 0;
		while ( messageByteTemp[tempOffset] == 0 )
			tempOffset++;
		for ( int i = messageByte.length - messageByteTemp.length + tempOffset; i < messageByte.length; i++ )
			messageByte[i] = messageByteTemp[tempOffset++];

		//Second encrypted key : encrypted_session_key //mp-int
		byte[] encrypted_session_key = new byte[host_key_public_modulus.length + 2]; //encrypted_session_key
		// is a
		// mp-int
		// !!!

		//the lengh of the mp-int.

		encrypted_session_key[1] = (byte) ( ( 8 * host_key_public_modulus.length ) & 0xff );

		encrypted_session_key[0] = (byte) ( ( ( 8 * host_key_public_modulus.length ) >> 8 ) & 0xff );
		//the mp-int
		for ( int i = 0; i < host_key_public_modulus.length; i++ )
			encrypted_session_key[i + 2] = messageByte[i];
		return encrypted_session_key;
	}

    static public byte[] addArrayOfBytes( byte[] a, byte[] b ) {
        if ( a == null )
            return b;
        if ( b == null )
            return a;
        byte[] temp = new byte[a.length + b.length];
        for ( int i = 0; i < a.length; i++ )
            temp[i] = a[i];
        for ( int i = 0; i < b.length; i++ )
            temp[i + a.length] = b[i];
        return temp;
    }

    static public byte[] XORArrayOfBytes( byte[] a, byte[] b ) {
        if ( a == null )
            return null;
        if ( b == null )
            return null;
        if ( a.length != b.length )
            return null;
        byte[] result = new byte[a.length];
        for ( int i = 0; i < result.length; i++ )
            result[i] = (byte) ( ( ( a[i] & 0xff ) ^ ( b[i] & 0xff ) ) & 0xff );// ^
        // xor
        // operator
        return result;
    }

	private boolean hasCipher(String cipherName) {
		return (Cipher.getInstance(cipherName) != null);
	}

	/**
	 * SSH_CMSG_USER string user login name on server
	 */
	private String Send_SSH_CMSG_USER() throws IOException {
		// if (debug > 0) System.err.println("Send_SSH_CMSG_USER(" + login +
		// ")");

		SshPacket1 p = new SshPacket1(SSH_CMSG_USER);
		p.putString(login);
		sendPacket1(p);

		return "";
	}

	/**
	 * Send_SSH_CMSG_AUTH_PASSWORD string user password
	 */
	private String Send_SSH_CMSG_AUTH_PASSWORD() throws IOException {
		SshPacket1 p = new SshPacket1(SSH_CMSG_AUTH_PASSWORD);
		p.putString(password);
		sendPacket1(p);
		return "";
	}

	/**
	 * Send_SSH_CMSG_EXEC_SHELL (no arguments) Starts a shell (command
	 * interpreter), and enters interactive session mode.
	 */
	private String Send_SSH_CMSG_EXEC_SHELL() throws IOException {
		SshPacket1 packet = new SshPacket1(SSH_CMSG_EXEC_SHELL);
		sendPacket1(packet);
		return "";
	}

	/**
	 * Send_SSH_CMSG_STDIN_DATA
	 * 
	 */
	private String Send_SSH_CMSG_STDIN_DATA(String str) throws IOException {
		SshPacket1 packet = new SshPacket1(SSH_CMSG_STDIN_DATA);
		packet.putString(str);
		sendPacket1(packet);
		return "";
	}

	/**
	 * Send_SSH_CMSG_REQUEST_PTY string TERM environment variable value (e.g.
	 * vt100) 32-bit int terminal height, rows (e.g., 24) 32-bit int terminal
	 * width, columns (e.g., 80) 32-bit int terminal width, pixels (0 if no
	 * graphics) (e.g., 480)
	 */
	private String Send_SSH_CMSG_REQUEST_PTY() throws IOException {
		SshPacket1 p = new SshPacket1(SSH_CMSG_REQUEST_PTY);

		p.putString(getTerminalID());
		p.putInt32(getTerminalHeight()); // Int32 rows
		p.putInt32(getTerminalWidth()); // Int32 columns
		p.putInt32(0); // Int32 x pixels
		p.putInt32(0); // Int32 y pixels
		p.putByte((byte) 0); // Int8 terminal modes
		sendPacket1(p);
		return "";
	}

	private String Send_SSH_CMSG_EXIT_CONFIRMATION() throws IOException {
		SshPacket1 packet = new SshPacket1(SSH_CMSG_EXIT_CONFIRMATION);
		sendPacket1(packet);
		return "";
	}

	//#endif
	
	/**
	 * Send_SSH_NOOP (no arguments) Sends a NOOP packet to keep the connection
	 * alive.
	 */
	public String Send_SSH_NOOP() throws IOException {
		// KARL The specification states that this packet is never sent, however
		// the OpenSSL source
		// for keep alives indicates that SSH_MSG_IGNORE (the alternative)
		// crashes some servers and
		// advocates SSH_MSG_NONE instead.
		/* OpenSSL now seems to not like SSH_MSG_NONE http://www.xk72.com/phpBB2/viewtopic.php?t=346 */
		
		//#ifndef nossh1
		if (useprotocol == 1) {
			SshPacket1 packet = new SshPacket1(SSH_MSG_IGNORE);
			sendPacket1(packet);
		}
		//#endif
		//#ifdef ssh2
		if (useprotocol == 2) {
			SshPacket2 packet = new SshPacket2(SSH2_MSG_IGNORE);
			packet.putString("");
			sendPacket2(packet);
		}
		//#endif
		return "";
	}

	protected String getTerminalID() {
		return sshSession.getTerminalID();
	}

	protected int getTerminalHeight() {
		return sshSession.getTerminalHeight();
	}

	protected int getTerminalWidth() {
		return sshSession.getTerminalWidth();
	}

    static public byte getNotZeroRandomByte() {
        java.util.Date date = new java.util.Date();
        String randomString = String.valueOf( SshIO.rnd.nextLong() * date.getTime() ); // RADEK
        // date.GetTime()
        // *
        // Math.random()
        byte[] randomBytes = md5.digest( randomString.getBytes() );
        int i = 0;
        while ( i < 20 ) {
            byte b = 0;
            if ( i < randomBytes.length )
                b = randomBytes[i];
            if ( b != 0 )
                return b;
            i++;
        }
        return getNotZeroRandomByte();
    }
}