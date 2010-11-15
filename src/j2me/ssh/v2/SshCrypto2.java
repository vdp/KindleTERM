/*
 * This file is part of "MidpSSH". Copyright (c) 2004 Karl von Randow.
 * 
 * MidpSSH is based upon Telnet Floyd and FloydSSH by Radek Polak.
 * 
 * --LICENSE NOTICE-- This program is free software; you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 675 Mass
 * Ave, Cambridge, MA 02139, USA. --LICENSE NOTICE--
 *  
 */
package ssh.v2;


public class SshCrypto2 {
	private BufferedDESedeCBC sndCipher, rcvCipher;

	private HMACSHA1 sndHmac, rcvHmac;

	public SshCrypto2(byte[] IVc2s, byte[] IVs2c, byte[] Ec2s, byte[] Es2c,
			byte[] MACc2s, byte[] MACs2c) {
		sndCipher = new BufferedDESedeCBC();
		sndCipher.init(true, IVc2s, Ec2s);
		rcvCipher = new BufferedDESedeCBC();
		rcvCipher.init(false, IVs2c, Es2c);

		sndHmac = new HMACSHA1();
		sndHmac.init(MACc2s);

		rcvHmac = new HMACSHA1();
		rcvHmac.init(MACs2c);
	}

	public byte[] encrypt(byte[] src) {
		byte[] dest = new byte[src.length];
		sndCipher.processBytes(src, 0, src.length, dest, 0);
		return dest;
	}

	public byte[] decrypt(byte[] src) {
		byte[] dest = new byte[src.length];
		rcvCipher.processBytes(src, 0, src.length, dest, 0);
		return dest;
	}

	/**
	 * @return Returns the rcvHmac.
	 */
	public HMACSHA1 getRcvHmac() {
		return rcvHmac;
	}

	/**
	 * @return Returns the sndHmac.
	 */
	public HMACSHA1 getSndHmac() {
		return sndHmac;
	}
}