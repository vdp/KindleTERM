package ssh.v2;

import java.util.Random;

import app.Settings;

public class PublicKeyAuthentication {

	public static final BigInteger p = new BigInteger(
			"d1c9009c7f181e9b27ce020e014d72c499f604c8d978a4dd9a8614362b09a74be4004cdd1b6ccf2bb2d2a4d4710be0817a375c85e8b0ce05e92a1f7c0b4886418dc6de84457dfd8dc19efdc0efb5c15bbab7a860b3e95c169d6e8aceef445deddc85ab44a11d5870847b99239011ff7d36a0f52cd11c3a0a33c415cdd58d85a1",
			16),
			q = new BigInteger("e691d26b30a8b43081b981d96a8189fe78d04f8f", 16),
			g = new BigInteger(
					"6ded9dc637ecc98f7ccf5a50839e13354972985cb901ca164fb8174318c84fec50ce84bfef5f4ee4981c239ed7c2bcf0718fcc0f30382df782221f64bfd09c9dc8e098ad10f296eadd9f5650f17414a77d3ff5ca8d103235e2de14a392c9a1156b2a5652d135111858af96d531688d80e962c52c75738f3d48aa09d59ca064a6",
					16);

	public BigInteger x, y;

	//#ifdef ssh2
	public PublicKeyAuthentication() {
		this.x = new BigInteger(Settings.x);
		this.y = new BigInteger(Settings.y);
	}
	//#endif

	public byte[] sign(byte[] session_id, byte[] message) {
		SshPacket2 buf = new SshPacket2();
		buf.putString(session_id);
		buf.putByte((byte) 50); // SSH2_MSG_USERAUTH_REQUEST
		buf.putBytes(message);

		message = buf.getData();

		SHA1Digest digest = new SHA1Digest();
		digest.update(message, 0, message.length);
		byte[] hash = new byte[digest.getDigestSize()];
		digest.doFinal(hash, 0);

		BigInteger[] rs = generateSignature(hash);

		buf = new SshPacket2();
		buf.putString(DHKeyExchange.SSH_DSS);
		byte[] r = rs[0].toByteArray();
		byte[] s = rs[1].toByteArray();
		byte[] sig = new byte[40];
		System.arraycopy(r, r.length > 20 ? 1 : 0, sig, r.length > 20 ? 0
				: 20 - r.length, r.length > 20 ? 20 : r.length);
		System.arraycopy(s, s.length > 20 ? 1 : 0, sig, s.length > 20 ? 20
				: 40 - s.length, s.length > 20 ? 20 : s.length);
		buf.putString(sig);

		return buf.getData();
	}

	/**
	 * generate a signature for the given message using the key we were
	 * initialised with. For conventional DSA the message should be a SHA-1 hash
	 * of the message of interest.
	 * 
	 * @param message
	 *            the message that will be verified later.
	 */
	public BigInteger[] generateSignature(byte[] message) {
		BigInteger m = new BigInteger(1, message);
		BigInteger k;
		int qBitLength = q.bitLength();

		do {
			k = new BigInteger(qBitLength, new Random());
		} while (k.compareTo(q) >= 0);

		BigInteger r = g.modPow(k, p).mod(q);

		k = k.modInverse(q).multiply(m.add(x.multiply(r)));

		BigInteger s = k.mod(q);

		BigInteger[] res = new BigInteger[2];

		res[0] = r;
		res[1] = s;

		return res;
	}

	public String getPublicKeyText() {
		byte[] pubblob = getPublicKeyBlob();
		byte[] pub = toBase64(pubblob, 0, pubblob.length);
		return DHKeyExchange.SSH_DSS + " " + new String(pub);
	}

	public byte[] getPublicKeyBlob() {
		SshPacket2 buf = new SshPacket2();
		buf.putString(DHKeyExchange.SSH_DSS);
		buf.putMpInt(p.toByteArray());
		buf.putMpInt(q.toByteArray());
		buf.putMpInt(g.toByteArray());
		buf.putMpInt(y.toByteArray());
		return buf.getData();
	}

	public static byte[][] generateKeyPair() {
		BigInteger x, y;
		
		do {
			x = new BigInteger(160, new Random());
		} while (x.equals(BigInteger.ZERO) || x.compareTo(q) >= 0);

		//
		// calculate the public key.
		//
		y = g.modPow(x, p);
		
		byte[][] res = new byte[2][];
		res[0] = x.toByteArray();
		res[1] = y.toByteArray();
		return res;
	}

	private static final byte[] b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
			.getBytes();

	private static byte[] toBase64(byte[] buf, int start, int length) {

		byte[] tmp = new byte[length * 2];
		int i, j, k;

		int foo = (length / 3) * 3 + start;
		i = 0;
		for (j = start; j < foo; j += 3) {
			k = (buf[j] >>> 2) & 0x3f;
			tmp[i++] = b64[k];
			k = (buf[j] & 0x03) << 4 | (buf[j + 1] >>> 4) & 0x0f;
			tmp[i++] = b64[k];
			k = (buf[j + 1] & 0x0f) << 2 | (buf[j + 2] >>> 6) & 0x03;
			tmp[i++] = b64[k];
			k = buf[j + 2] & 0x3f;
			tmp[i++] = b64[k];
		}

		foo = (start + length) - foo;
		if (foo == 1) {
			k = (buf[j] >>> 2) & 0x3f;
			tmp[i++] = b64[k];
			k = ((buf[j] & 0x03) << 4) & 0x3f;
			tmp[i++] = b64[k];
			tmp[i++] = (byte) '=';
			tmp[i++] = (byte) '=';
		} else if (foo == 2) {
			k = (buf[j] >>> 2) & 0x3f;
			tmp[i++] = b64[k];
			k = (buf[j] & 0x03) << 4 | (buf[j + 1] >>> 4) & 0x0f;
			tmp[i++] = b64[k];
			k = ((buf[j + 1] & 0x0f) << 2) & 0x3f;
			tmp[i++] = b64[k];
			tmp[i++] = (byte) '=';
		}
		byte[] bar = new byte[i];
		System.arraycopy(tmp, 0, bar, 0, i);
		return bar;
	}
}
