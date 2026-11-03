package com.github.esrrhs.fakecore.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class EncryptUtilTest
{
	@Test
	void rc4RoundTripBytes()
	{
		byte[] plain = "hello fakecore".getBytes(StandardCharsets.UTF_8);
		String key = "secret-key";
		byte[] enc = EncryptUtil.rc4Encrypt(plain, key);
		assertNotNull(enc);
		byte[] dec = EncryptUtil.rc4Decrypt(enc, key);
		assertArrayEquals(plain, dec);
	}

	@Test
	void rc4NullSafe()
	{
		assertNull(EncryptUtil.rc4Encrypt(null, "k"));
		assertNull(EncryptUtil.rc4Encrypt("x".getBytes(), null));
		assertNull(EncryptUtil.rc4Decrypt(null, "k"));
	}

	@Test
	void rc4StringHexRoundTrip()
	{
		String plain = "hello python";
		String key = "123456";
		String hex = RC4.encry_RC4_string(plain, key);
		assertNotNull(hex);
		assertEquals(plain, RC4.decry_RC4(hex, key));
	}

	@Test
	void desedeRoundTrip()
	{
		byte[] key = new byte[24];
		for (int i = 0; i < key.length; i++)
		{
			key[i] = (byte) (i + 1);
		}
		byte[] plain = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
		byte[] enc = EncryptUtil.encryptMode(key, plain);
		assertNotNull(enc);
		byte[] dec = EncryptUtil.decryptMode(key, enc);
		assertArrayEquals(plain, dec);
	}
}
