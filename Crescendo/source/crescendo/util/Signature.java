package crescendo.util;

import horizon.system.AbstractObject;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class Signature extends AbstractObject {
	private static final String CHARSET = "UTF8";
	private static final KeyPair key;
	static {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(512);
			key = keyGen.generateKeyPair();
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	public static final byte[] get(String data) {
		if (isEmpty(data)) return null;
		try {
			java.security.Signature sig = signature();
			sig.initSign(key.getPrivate());
			sig.update(data.getBytes(CHARSET));

			return sig.sign();
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	public static final String string(String data) {
		try {
			return new String(get(data), CHARSET);
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	public static final boolean verified(String data, byte[] signature) {
		if (isEmpty(data)) return false;
		try {
			java.security.Signature sig = signature();
			sig.initVerify(key.getPublic());
			sig.update(data.getBytes(CHARSET));

			return sig.verify(signature);
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	private static java.security.Signature signature() throws Exception {
		return java.security.Signature.getInstance("MD5WithRSA");
	}

	public static void main(String[] args) {
		String msg = "gksakswls",
			   sig = string(msg);
		byte[] bytes = get(msg);
		System.out.println("[" + sig + "]");
		System.out.println(verified(msg, bytes));
	}
}