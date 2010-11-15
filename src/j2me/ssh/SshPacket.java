/*
 * This file is part of "The Java Telnet Application".
 *
 * (c) Matthias L. Jugel, Marcus Mei�ner 1996-2002. All Rights Reserved.
 * The file was changed by Radek Polak to work as midlet in MIDP 1.0
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

abstract public class SshPacket {
	public SshPacket() { /* nothing */
	}

	// Data management
	protected byte[] byteArray = new byte[0];

	protected int offset;

	private boolean finished;

	public byte[] getData() {
		return byteArray;
	}

	public void putData(byte[] data) {
		byteArray = data;
		offset = 0;
		finished = true;
	}

	public boolean isFinished() {
		return finished;
	}

	abstract public int addPayload(byte[] buff, int boffset, int length);

	// Type
	protected byte packet_type;

	public byte getType() {
		return packet_type;
	}

	public abstract void putMpInt(byte[] bi);

	public int getInt32() {
		short d0 = byteArray[offset++];
		short d1 = byteArray[offset++];
		short d2 = byteArray[offset++];
		short d3 = byteArray[offset++];

		if (d0 < 0)
			d0 = (short) (256 + d0);
		if (d1 < 0)
			d1 = (short) (256 + d1);
		if (d2 < 0)
			d2 = (short) (256 + d2);
		if (d3 < 0)
			d3 = (short) (256 + d3);

		return (d0 << 24) + (d1 << 16) + (d2 << 8) + d3;
	}

	public int getInt16() {
		short d0 = byteArray[offset++];
		short d1 = byteArray[offset++];

		if (d0 < 0)
			d0 = (short) (256 + d0);
		if (d1 < 0)
			d1 = (short) (256 + d1);

		return (d0 << 8) + d1;
	}

	public String getString() {
		int length = getInt32();

		StringBuffer str = new StringBuffer();
		for (int i = 0; i < length; i++) {
			/*
			 * if ( byteArray[offset] >= 0 ) str.append( (char) (
			 * byteArray[offset++] ) ); else str.append( (char) ( 256 +
			 * byteArray[offset++] ) );
			 */
			str.append((char) byteArray[offset++]);
		}
		return str.toString();
	}

	public byte[] getByteString() {
		int length = getInt32();
		return getBytes(length);
	}

	public byte getByte() {
		return byteArray[offset++];
	}

	public byte[] getBytes(int cnt) {
		byte[] bytes = new byte[cnt];

		System.arraycopy(byteArray, offset, bytes, 0, cnt);
		offset += cnt;
		return bytes;
	}

	private void grow(int howmuch) {
		byte[] value = new byte[byteArray.length + howmuch];
		System.arraycopy(byteArray, 0, value, 0, byteArray.length);
		byteArray = value;
	}

	public void putInt16(int xint) {
		int boffset = byteArray.length;
		grow(2);
		byteArray[boffset + 1] = (byte) ((xint) & 0xff);
		byteArray[boffset] = (byte) ((xint >> 8) & 0xff);
	}

	public void putInt32(int xint) {
		int boffset = byteArray.length;
		grow(4);
		byteArray[boffset + 3] = (byte) ((xint) & 0xff);
		byteArray[boffset + 2] = (byte) ((xint >> 8) & 0xff);
		byteArray[boffset + 1] = (byte) ((xint >> 16) & 0xff);
		byteArray[boffset + 0] = (byte) ((xint >> 24) & 0xff);
	}

	public void putByte(byte xbyte) {
		grow(1);
		byteArray[byteArray.length - 1] = xbyte;
	}

	public void putBytes(byte[] bytes) {
		int oldlen = byteArray.length;
		grow(bytes.length);
		System.arraycopy(bytes, 0, byteArray, oldlen, bytes.length);
	}

	public void putBytes(byte[] bytes, int offset, int length) {
		int oldlen = byteArray.length;
		grow(length);
		System.arraycopy(bytes, offset, byteArray, oldlen, length);
	}

	/**
	 * Add a SSH String to a packet. The incore representation is a INT32 length
	 * BYTE[length] data
	 * 
	 * @param str:
	 *            The string to be added.
	 */
	public void putString(String str) {
		putInt32(str.length());
		putBytes(str.getBytes());
	}

	public void putString(byte[] foo) {
		putString(foo, 0, foo.length);
	}

	public void putString(byte[] foo, int begin, int length) {
		putInt32(length);
		putBytes(foo, begin, length);
	}
}