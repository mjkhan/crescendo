package crescendo.util;

import horizon.system.AbstractObject;

import java.security.Key;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;

public class StringEncryptor extends AbstractObject {
	public static Key keyFrom(String s) {
		try {
			return SecretKeyFactory.getInstance("DES").generateSecret(new SecretKeySpec(s.getBytes("UTF-8"), "DES"));
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	private static Key key;
	private Cipher cipher;

	private Cipher cipher() throws Exception {
		return cipher != null ? cipher : (cipher = Cipher.getInstance("DES/ECB/PKCS5Padding"));
	}

	public StringEncryptor setCipher(Cipher cipher) {
		this.cipher = cipher;
		return this;
	}

	private Key key() throws Exception {
		return key != null ? key : (key = keyFrom("crescendo-secret-key"));
	}

	public StringEncryptor setKey(Key key) {
		StringEncryptor.key = key;
		return this;
	}

	private String encode(byte[] bytes) throws Exception{
		return Base64.getEncoder().encodeToString(bytes);
	}

	public String encrypt(String s) {
		if (isEmpty(s)) return s;

		try {
			Cipher cipher = cipher();
			cipher.init(Cipher.ENCRYPT_MODE, key());
			return encode(cipher.doFinal(s.getBytes("UTF-8")));
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	private byte[] decode(String s) throws Exception {
		return Base64.getDecoder().decode(s);
	}

	public String decrypt(String s) {
		if (isEmpty(s)) return s;

		try {
			Cipher cipher = cipher();
			cipher.init(Cipher.DECRYPT_MODE, key());
			return new String(cipher.doFinal(decode(s)), "UTF-8");
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	public static void main(String[] args) {
		String encrypted = new StringEncryptor().encrypt("안녕하세요, 여러분");
		System.out.println(encrypted);
		String decrypted = new StringEncryptor().decrypt(encrypted);
		System.out.println(decrypted);
	}

}