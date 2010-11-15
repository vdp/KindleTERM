/*
 * This file is part of "The Java Telnet Application".
 * 
 * (c) Matthias L. Jugel, Marcus Mei�ner 1996-2002. All Rights Reserved. The
 * file was changed by Radek Polak to work as midlet in MIDP 1.0
 * 
 * This file has been modified by Karl von Randow for MidpSSH.
 * 
 * Please visit http://javatelnet.org/ for updates and contact.
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
 */
package ssh.v2;

import java.io.IOException;

import ssh.SshIO;
import ssh.SshPacket;

public class SshPacket2 extends SshPacket {

	private static final int PHASE_packet_length = 0;

	private static final int PHASE_block = 1;

	//SSH_RECEIVE_PACKET
	private byte[] packet_length_array = new byte[8]; // 8 bytes

	private int packet_length; // 32-bit sign int

	private int padlen; // packet length 1 byte unsigned

	private int position;

	private int phase_packet = PHASE_packet_length;

	private SshCrypto2 crypto;

	public SshPacket2() {
		
	}
	
	public SshPacket2(SshCrypto2 _crypto) {
		/* receiving packet */
		crypto = _crypto;
	}

	public SshPacket2(byte newType) {
		packet_type = newType;
	}

	/**
	 * Return the mp-int at the position offset in the data First 4 bytes are
	 * the number of bytes in the integer, msb first (for example, the value
	 * 0x00012345 would have 17 bits). The value zero has zero bits. It is
	 * permissible that the number of bits be larger than the real number of
	 * bits. The number of bits is followed by (bits + 7) / 8 bytes of binary
	 * data, msb first, giving the value of the integer.
	 */

	public byte[] getMpInt() {
		return getBytes(getInt32());
	}

	public void putMpInt(byte[] foo) {
		int i = foo.length;
		if ((foo[0] & 0x80) != 0) {
			i++;
			putInt32(i);
			putByte((byte) 0);
		} else {
			putInt32(i);
		}
		putBytes(foo);
	}

	public byte[] getPayLoad(SshCrypto2 xcrypt, long seqnr) throws IOException {
		byte[] data = getData();

		int blocksize = 8;

		// crypted data is:
		// packet length [ payloadlen + padlen + type + data ]
		packet_length = 4 + 1 + 1;
		if (data != null)
			packet_length += data.length;

		// pad it up to full blocksize.
		// If not crypto, zeroes, otherwise random.
		// (zeros because we do not want to tell the attacker the state of our
		//  random generator)
		int padlen = blocksize - (packet_length % blocksize);
		if (padlen < 4)
			padlen += blocksize;

		byte[] padding = new byte[padlen];
		//System.out.println( "packet length is " + packet_length + ", padlen
		// is " + padlen );
		if (xcrypt == null)
			for (int i = 0; i < padlen; i++)
				padding[i] = 0;
		else
			for (int i = 0; i < padlen; i++)
				padding[i] = SshIO.getNotZeroRandomByte();

		// [ packetlength, padlength, padding, packet type, data ]
		byte[] block = new byte[packet_length + padlen];

		int xlen = padlen + packet_length - 4;
		block[3] = (byte) (xlen & 0xff);
		block[2] = (byte) ((xlen >> 8) & 0xff);
		block[1] = (byte) ((xlen >> 16) & 0xff);
		block[0] = (byte) ((xlen >> 24) & 0xff);

		block[4] = (byte) padlen;
		block[5] = getType();
		System.arraycopy(data, 0, block, 6, data.length);
		System.arraycopy(padding, 0, block, 6 + data.length, padlen);

		/*
		 * byte[] md5sum; if ( xcrypt != null ) { MD5 md5 = new MD5(); byte[]
		 * seqint = new byte[4];
		 * 
		 * seqint[0] = (byte) ( ( seqnr >> 24 ) & 0xff ); seqint[1] = (byte) ( (
		 * seqnr >> 16 ) & 0xff ); seqint[2] = (byte) ( ( seqnr >> 8 ) & 0xff );
		 * seqint[3] = (byte) ( ( seqnr ) & 0xff ); md5.update( seqint, 0, 4 );
		 * md5.update( block, 0, block.length ); md5sum = md5.digest(); } else {
		 * md5sum = new byte[0]; }
		 */

		byte[] mac = null;
		if (xcrypt != null) {
			HMACSHA1 c2smac = xcrypt.getSndHmac();
			if (c2smac != null) {
				c2smac.update((int) seqnr);
				c2smac.update(block, 0, block.length);
				mac = c2smac.doFinal();
			}
		}

		if (xcrypt != null)
			block = xcrypt.encrypt(block);

		byte[] sendblock = new byte[block.length
				+ (mac != null ? mac.length : 0)];
		System.arraycopy(block, 0, sendblock, 0, block.length);
		if (mac != null) {
			System.arraycopy(mac, 0, sendblock, block.length, mac.length);
		}
		return sendblock;
	};

	private byte block[];

	public int addPayload(byte buff[], int boffset, int length) {
		int hmaclen = 0;
		int boffsetend = boffset + length;

		if (crypto != null)
			hmaclen = crypto.getRcvHmac().getBlockSize();

		//System.out.println( "addPayload2 " + length + " mac length " +
		// hmaclen );

		/*
		 * Note: The whole packet is encrypted, except for the MAC.
		 * 
		 * (So I have to rewrite it again).
		 */

		while (boffset < boffsetend) {
			switch (phase_packet) {
			// 4 bytes
			// Packet length: 32 bit unsigned integer
			// gives the length of the packet, not including the length
			// field
			// and padding. maximum is 262144 bytes.

			case PHASE_packet_length:
				packet_length_array[position++] = buff[boffset++];
				if (position == 8) {
					/*
					 * HERE I AM Well, the crypto doesn't appear to be working.
					 * We've tested the crypto algs in Jsch so maybe we're not
					 * getting the right keys? or setting up the ciphers
					 * incorrectly?
					 *  
					 */
					packet_length = (packet_length_array[3] & 0xff)
							+ ((packet_length_array[2] & 0xff) << 8)
							+ ((packet_length_array[1] & 0xff) << 16)
							+ ((packet_length_array[0] & 0xff) << 24);

					// NOW HAVE READ FIRST 8 BYTES OF PACKET, CAN DECRYPT
					byte[] packet_length_array;

					if (crypto != null) {
						packet_length_array = crypto
								.decrypt(this.packet_length_array);
					} else {
						packet_length_array = this.packet_length_array;
					}

					packet_length = (packet_length_array[3] & 0xff)
							+ ((packet_length_array[2] & 0xff) << 8)
							+ ((packet_length_array[1] & 0xff) << 16)
							+ ((packet_length_array[0] & 0xff) << 24);
					padlen = packet_length_array[4];
					position = 3;
					//System.out.println( "SSH2: packet length " +
					// packet_length );
					//System.out.println( "SSH2: padlen " + padlen );
					packet_length += hmaclen; /* len(md5) */
					block = new byte[packet_length - 1]; /*
														  * padlen already done
														  */
					// copy in already decrypted first 3 bytes of payload
					System.arraycopy(packet_length_array, 5, block, 0, 3);
					phase_packet++;
				}
				break; //switch (phase_packet)

			//8*(packet_length/8 +1) bytes

			case PHASE_block:
				if (block.length > position) {
					if (boffset < boffsetend) {
						int amount = boffsetend - boffset;
						if (amount > block.length - position)
							amount = block.length - position;
						System
								.arraycopy(buff, boffset, block, position,
										amount);
						boffset += amount;
						position += amount;
					}
				}

				if (position == block.length) { //the block is complete
					packet_length -= hmaclen; // packet_length is now payload
											  // (incl type) + padding
					byte[] decryptedBlock = new byte[block.length - hmaclen - 3]; // 3
																				  // bytes
																				  // already
																				  // decrypted
					byte[] data;

					// first 3 bytes already decrypted
					System.arraycopy(block, 3, decryptedBlock, 0,
							decryptedBlock.length);

					if (crypto != null)
						decryptedBlock = crypto.decrypt(decryptedBlock);

					// Add back in first 3 bytes
					byte[] dd = new byte[decryptedBlock.length + 3];
					System.arraycopy(block, 0, dd, 0, 3);
					System.arraycopy(decryptedBlock, 0, dd, 3,
							decryptedBlock.length);
					decryptedBlock = dd;

					packet_type = decryptedBlock[0];
					//System.err.println( "IN Packet type: " + getType() );
					//System.err.println( "Packet len: " + packet_length );

					//data
					if (packet_length > padlen + 1 + 1) { // 1 for padding
														  // length, 1 for type
						data = new byte[packet_length - 2 - padlen];
						System.arraycopy(decryptedBlock, 1, data, 0,
								data.length);
						putData(data);
					} else {
						putData(null);
					}
					/* MAC! */
					return boffset;
				}
				break;
			}
		}
		return boffset;
	}
	/*
	 * 
	 * private boolean checkCrc(){ byte[] crc_arrayCheck = new byte[4]; long
	 * crcCheck;
	 * 
	 * crcCheck = SshMisc.crc32(decryptedBlock, decryptedBlock.length-4);
	 * crc_arrayCheck[3] = (byte) (crcCheck & 0xff); crc_arrayCheck[2] = (byte)
	 * ((crcCheck>>8) & 0xff); crc_arrayCheck[1] = (byte) ((crcCheck>>16) &
	 * 0xff); crc_arrayCheck[0] = (byte) ((crcCheck>>24) & 0xff);
	 * 
	 * if(debug) { System.err.println(crc_arrayCheck[3]+" == "+crc_array[3]);
	 * System.err.println(crc_arrayCheck[2]+" == "+crc_array[2]);
	 * System.err.println(crc_arrayCheck[1]+" == "+crc_array[1]);
	 * System.err.println(crc_arrayCheck[0]+" == "+crc_array[0]); } if
	 * (crc_arrayCheck[3] != crc_array[3]) return false; if (crc_arrayCheck[2] !=
	 * crc_array[2]) return false; if (crc_arrayCheck[1] != crc_array[1]) return
	 * false; if (crc_arrayCheck[0] != crc_array[0]) return false; return true; }
	 */
}