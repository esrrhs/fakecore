package com.github.esrrhs.fakecore.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class EncryptUtilMoreTest
{
	@Test
	void genCroptyKeyNullAndInvalidLengths()
	{
		assertNull(EncryptUtil.genCroptyKey(null, "abcdefgh"));
		assertNull(EncryptUtil.genCroptyKey(new byte[23], "abcdefgh"));
		assertNull(EncryptUtil.genCroptyKey(new byte[24], "short")); // < 8
		assertNull(EncryptUtil.genCroptyKey(new byte[24], "123456789012345678901")); // > 20
	}

	@Test
	void genCroptyKeyValidVariants()
	{
		byte[] keyA = new byte[24];
		for (int i = 0; i < 24; i++)
		{
			keyA[i] = (byte) (i + 3);
		}
		byte[] k8 = EncryptUtil.genCroptyKey(keyA, "abcdefgh");
		assertNotNull(k8);
		assertEquals(24, k8.length);

		byte[] k12 = EncryptUtil.genCroptyKey(keyA, "abcdefghijkl");
		assertNotNull(k12);

		byte[] k16 = EncryptUtil.genCroptyKey(keyA, "abcdefghijklmnop");
		assertNotNull(k16);
	}

	@Test
	void base64Helpers()
	{
		assertNull(EncryptUtil.getBASE64(null));
		assertNull(EncryptUtil.getFromBASE64(null));
		byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
		String enc = EncryptUtil.getBASE64(data);
		assertNotNull(enc);
		assertArrayEquals(data, EncryptUtil.getFromBASE64(enc));
	}

	@Test
	void rndStringWithAndWithoutSeed()
	{
		String a = EncryptUtil.RndString(8, null);
		assertEquals(8, a.length());
		String b = EncryptUtil.RndString(12, new int[] { 42 });
		String c = EncryptUtil.RndString(12, new int[] { 42 });
		assertEquals(b, c);
		assertEquals(12, b.length());
	}

	@Test
	void restructParamBlankAndValid()
	{
		assertNull(EncryptUtil.restructParam(null));
		assertNull(EncryptUtil.restructParam(""));
		assertNull(EncryptUtil.restructParam("   "));
		String out = EncryptUtil.restructParam("hello");
		assertNotNull(out);
		assertTrue(out.contains("&stamp="));
	}

	@Test
	void encryptDecryptRejectBadKey()
	{
		assertNull(EncryptUtil.encryptMode(new byte[8], "x".getBytes()));
		assertNull(EncryptUtil.decryptMode(new byte[8], new byte[] { 1, 2, 3 }));
	}
}
