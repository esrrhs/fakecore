package com.github.esrrhs.fakecore.codec;

/**
 * Thin Base64 helpers. Delegates to commons-codec while keeping the historical package API.
 */
public class Base64Util
{
	private Base64Util()
	{
	}

	public static final String encode(byte[] data)
	{
		if (data == null)
		{
			return null;
		}
		return org.apache.commons.codec.binary.Base64.encodeBase64String(data);
	}

	public static final byte[] decode(String str)
	{
		if (str == null)
		{
			return null;
		}
		return org.apache.commons.codec.binary.Base64.decodeBase64(str);
	}
}
