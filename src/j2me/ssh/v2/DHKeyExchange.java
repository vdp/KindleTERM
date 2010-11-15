/*
 * This file is part of "MidpSSH".
 * 
 * This file was greatly adapted from Java Secure Channel (www.jcraft.com/jsch/) for
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

import java.util.Random;

public class DHKeyExchange {

	public static final String SSH_DSS = "ssh-dss";
	
	public static final BigInteger g = new BigInteger("2", 16);
//	public static final BigInteger g = new BigInteger(new byte[] { 2 });

	public static final BigInteger p = new BigInteger("ffffffffffffffffc90fdaa22168c234c4c6628b80dc1cd129024e088a67cc74020bbea63b139b22514a08798e3404ddef9519b3cd3a431b302b0a6df25f14374fe1356d6d51c245e485b576625e7ec6f44c42e9a637ed6b0bff5cb6f406b7edee386bfb5a899fa5ae9f24117c4b1fe649286651ece65381ffffffffffffffff", 16);
//	public static final BigInteger p = new BigInteger(new byte[] { (byte) 0x00, (byte) 0xFF, (byte) 0xFF,
//			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
//			(byte) 0xFF, (byte) 0xC9, (byte) 0x0F, (byte) 0xDA, (byte) 0xA2,
//			(byte) 0x21, (byte) 0x68, (byte) 0xC2, (byte) 0x34, (byte) 0xC4,
//			(byte) 0xC6, (byte) 0x62, (byte) 0x8B, (byte) 0x80, (byte) 0xDC,
//			(byte) 0x1C, (byte) 0xD1, (byte) 0x29, (byte) 0x02, (byte) 0x4E,
//			(byte) 0x08, (byte) 0x8A, (byte) 0x67, (byte) 0xCC, (byte) 0x74,
//			(byte) 0x02, (byte) 0x0B, (byte) 0xBE, (byte) 0xA6, (byte) 0x3B,
//			(byte) 0x13, (byte) 0x9B, (byte) 0x22, (byte) 0x51, (byte) 0x4A,
//			(byte) 0x08, (byte) 0x79, (byte) 0x8E, (byte) 0x34, (byte) 0x04,
//			(byte) 0xDD, (byte) 0xEF, (byte) 0x95, (byte) 0x19, (byte) 0xB3,
//			(byte) 0xCD, (byte) 0x3A, (byte) 0x43, (byte) 0x1B, (byte) 0x30,
//			(byte) 0x2B, (byte) 0x0A, (byte) 0x6D, (byte) 0xF2, (byte) 0x5F,
//			(byte) 0x14, (byte) 0x37, (byte) 0x4F, (byte) 0xE1, (byte) 0x35,
//			(byte) 0x6D, (byte) 0x6D, (byte) 0x51, (byte) 0xC2, (byte) 0x45,
//			(byte) 0xE4, (byte) 0x85, (byte) 0xB5, (byte) 0x76, (byte) 0x62,
//			(byte) 0x5E, (byte) 0x7E, (byte) 0xC6, (byte) 0xF4, (byte) 0x4C,
//			(byte) 0x42, (byte) 0xE9, (byte) 0xA6, (byte) 0x37, (byte) 0xED,
//			(byte) 0x6B, (byte) 0x0B, (byte) 0xFF, (byte) 0x5C, (byte) 0xB6,
//			(byte) 0xF4, (byte) 0x06, (byte) 0xB7, (byte) 0xED, (byte) 0xEE,
//			(byte) 0x38, (byte) 0x6B, (byte) 0xFB, (byte) 0x5A, (byte) 0x89,
//			(byte) 0x9F, (byte) 0xA5, (byte) 0xAE, (byte) 0x9F, (byte) 0x24,
//			(byte) 0x11, (byte) 0x7C, (byte) 0x4B, (byte) 0x1F, (byte) 0xE6,
//			(byte) 0x49, (byte) 0x28, (byte) 0x66, (byte) 0x51, (byte) 0xEC,
//			(byte) 0xE6, (byte) 0x53, (byte) 0x81, (byte) 0xFF, (byte) 0xFF,
//			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
//			(byte) 0xFF });
    
	public byte[] V_S;

	public byte[] V_C;

	public byte[] I_S;

	public byte[] I_C;

	public byte[] H;

	private byte[] e;
	
	public byte[] K;

    private BigInteger x, y;
    
    public String keyalg;

	public DHKeyExchange(int qLength) {
        BigInteger[] keys = generateKeyPair(qLength);
        x = keys[0];
        y = keys[1];
	}
    
    public DHKeyExchange(byte[] x_array, byte[] y_array) {
        x = new BigInteger(x_array);
        y = new BigInteger(y_array);
    }

	public byte[] getE() {
		if (e == null) {
			e = y.toByteArray();
		}
		return e;
	}
    
    public static BigInteger[] generateKeyPair(int qLength) {
        qLength = Math.min(qLength, p.bitLength() - 1 - 1);

        //System.out.println( "Generating private key" );
        //
        // calculate the private key
        //
        BigInteger x = new BigInteger(qLength, new Random());

        //System.out.println( "PRIVATE KEY=" + this.x );
        //System.out.println( "Generating public key" );
        //
        // calculate the public key.
        //
        BigInteger y = g.modPow(x, p);
        //System.out.println( "PUBLIC KEY=" + this.y );
        //System.out.println( "Generated both keys" );
        return new BigInteger[] { x, y };
    }
    
    public static byte[][] generateKeyPairBytes(int qLength) {
        BigInteger[] keys = generateKeyPair(qLength);
        return new byte[][] { keys[0].toByteArray(), keys[1].toByteArray() };
    }

	/**
	 * 
	 * @param K_S
	 * @param f
	 * @param r
	 *            part 1 of sig of H
	 * @param s
	 *            part 2 of sig of H
	 * @return
	 * @throws Exception
	 */
	public boolean next(byte[] K_S, byte[] f, byte[] sig_of_h) {
		// K_S is server_key_blob, which includes ....
		// string ssh-dss
		// impint p of dsa
		// impint q of dsa
		// impint g of dsa
		// impint pub_key of dsa

		//K = calculateAgreement( new BigInteger( f ) ).toByteArray();
		K = new BigInteger(f).modPow(x, p).toByteArray();
		
		//The hash H is computed as the HASH hash of the concatenation
		// of the
		//following:
		// string V_C, the client's version string (CR and NL excluded)
		// string V_S, the server's version string (CR and NL excluded)
		// string I_C, the payload of the client's SSH_MSG_KEXINIT
		// string I_S, the payload of the server's SSH_MSG_KEXINIT
		// string K_S, the host key
		// mpint e, exchange value sent by the client
		// mpint f, exchange value sent by the server
		// mpint K, the shared secret
		// This value is called the exchange hash, and it is used to
		// authenti-
		// cate the key exchange.

		SshPacket2 buf = new SshPacket2();
		buf.putString(V_C);
		buf.putString(V_S);
		buf.putString(I_C);
		buf.putString(I_S);
		buf.putString(K_S);
		buf.putMpInt(e);
		buf.putMpInt(f);
		buf.putMpInt(K);
		byte[] foo = buf.getData();

		SHA1Digest sha = new SHA1Digest();
		sha.update(foo, 0, foo.length);

		H = new byte[sha.getDigestSize()];
		sha.doFinal(H, 0);

		SshPacket2 pp = new SshPacket2( null );
		pp.putBytes( K_S );
		keyalg = pp.getString();
		boolean result;
		
		if ( keyalg.equals(SSH_DSS) ) {
			byte [] p = pp.getByteString();
			byte [] q = pp.getByteString();
			byte [] g = pp.getByteString();
			byte [] y = pp.getByteString();

			result = verifyDSASignature(H, sig_of_h, new BigInteger(y), new BigInteger(p),
					new BigInteger(q), new BigInteger(g));
		}
		else {
			//System.out.println("unknow alg");
			result = false;
		}
		
		return result;
	}
	
	public static boolean verifyDSASignature(byte[] message, byte[] sig, BigInteger y,
			BigInteger p, BigInteger q, BigInteger g) {
		SshPacket2 buf = new SshPacket2();
		buf.putBytes(sig);
		buf.getByteString(); // algorithm
		byte[] blob = buf.getByteString();

		int rslen = blob.length / 2;
		byte[] tmp = new byte[rslen];
		tmp[0] = 0;
		System.arraycopy(blob, 0, tmp, 0, rslen);
		BigInteger r = new BigInteger(1, tmp);
		System.arraycopy(blob, rslen, tmp, 0, rslen);
		BigInteger s = new BigInteger(1, tmp);

		//boolean verifySignature(byte[] message, BigInteger r, BigInteger s)
		SHA1Digest digest = new SHA1Digest();
		digest.update(message, 0, message.length);
		byte[] hash = new byte[digest.getDigestSize()];
		digest.doFinal(hash, 0);
		message = hash;

		BigInteger m = new BigInteger(1, message);
		BigInteger zero = BigInteger.valueOf(0);

		if (zero.compareTo(r) >= 0 || q.compareTo(r) <= 0) {
			return false;
		}

		if (zero.compareTo(s) >= 0 || q.compareTo(s) <= 0) {
			return false;
		}

		BigInteger w = s.modInverse(q);

		BigInteger u1 = m.multiply(w).mod(q);
		BigInteger u2 = r.multiply(w).mod(q);

		u1 = g.modPow(u1, p);
		u2 = y.modPow(u2, p);

		BigInteger v = u1.multiply(u2).mod(p).mod(q);

		return v.equals(r);
	}
}