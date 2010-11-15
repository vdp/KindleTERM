/*
 * This file is part of "MidpSSH".
 * 
 * This file was adapted from Java Secure Channel (www.jcraft.com/jsch/) for
 * MidpSSH by Karl von Randow
 * 
 * Copyright (c) 2002,2003,2004 ymnk, JCraft,Inc. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. The names of the authors may not be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JCRAFT,
 * INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ssh.v2;

public class HMACSHA1 {
	private String name = "hmac-sha1";

	private int bsize = 20;

	private byte[] tmp = new byte[4];
	
	private final static int BLOCK_LENGTH = 64;

	private final static byte IPAD = (byte) 0x36;

	private final static byte OPAD = (byte) 0x5C;

	private SHA1Digest digest;

	private int digestSize;

	private byte[] inputPad = new byte[BLOCK_LENGTH];

	private byte[] outputPad = new byte[BLOCK_LENGTH];

	public int getBlockSize() {
		return bsize;
	};

	public void init(byte[] key) {
		if (key.length > bsize) {
			byte[] tmp = new byte[bsize];
			System.arraycopy(key, 0, tmp, 0, bsize);
			key = tmp;
		}
		
		digest = new SHA1Digest();
		digestSize = digest.getDigestSize();
		
		digest.reset();

		if (key.length > BLOCK_LENGTH) {
			digest.update(key, 0, key.length);
			digest.doFinal(inputPad, 0);
			for (int i = digestSize; i < inputPad.length; i++) {
				inputPad[i] = 0;
			}
		} else {
			System.arraycopy(key, 0, inputPad, 0, key.length);
			for (int i = key.length; i < inputPad.length; i++) {
				inputPad[i] = 0;
			}
		}

		outputPad = new byte[inputPad.length];
		System.arraycopy(inputPad, 0, outputPad, 0, inputPad.length);

		for (int i = 0; i < inputPad.length; i++) {
			inputPad[i] ^= IPAD;
		}

		for (int i = 0; i < outputPad.length; i++) {
			outputPad[i] ^= OPAD;
		}

		digest.update(inputPad, 0, inputPad.length);
	}

	public void update(int i) {
		tmp[0] = (byte) (i >>> 24);
		tmp[1] = (byte) (i >>> 16);
		tmp[2] = (byte) (i >>> 8);
		tmp[3] = (byte) i;
		update(tmp, 0, 4);
	}

	public void update(byte foo[], int s, int l) {
		digest.update(foo, s, l);
	}

	public byte[] doFinal() {
		byte[] out = new byte[digestSize];
		
		byte[] tmp = new byte[digestSize];
		digest.doFinal(tmp, 0);

		digest.update(outputPad, 0, outputPad.length);
		digest.update(tmp, 0, tmp.length);

		digest.doFinal(out, 0);

		reset();

		return out;
	}

	/**
	 * Reset the mac generator.
	 */
	public void reset() {
		/*
		 * reset the underlying digest.
		 */
		digest.reset();

		/*
		 * reinitialize the digest.
		 */
		digest.update(inputPad, 0, inputPad.length);
	}

	public String getName() {
		return name;
	}
}