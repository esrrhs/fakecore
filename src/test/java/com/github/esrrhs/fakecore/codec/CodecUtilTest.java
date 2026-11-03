package com.github.esrrhs.fakecore.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CodecUtilTest
{
	@Test
	void base64RoundTrip()
	{
		byte[] data = "fakecore".getBytes();
		String encoded = CodecUtil.base64Encode(data);
		assertNotNull(encoded);
		assertArrayEquals(data, CodecUtil.base64Decode(encoded));
	}

	@Test
	void base64NullAndEmpty()
	{
		assertNull(CodecUtil.base64Encode(null));
		assertNull(CodecUtil.base64Encode(new byte[0]));
		assertNull(CodecUtil.base64Decode(null));
		assertNull(CodecUtil.base64Decode(""));
	}

	@Test
	void htmlEscapeRoundTrip()
	{
		String html = "<div>\"a&b\"</div>";
		String escaped = CodecUtil.htmlEscape(html);
		assertNotNull(escaped);
		assertEquals(html, CodecUtil.htmlUnescape(escaped));
	}

	@Test
	void base64UtilDelegates()
	{
		byte[] data = "hello".getBytes();
		String enc = Base64Util.encode(data);
		assertArrayEquals(data, Base64Util.decode(enc));
		assertArrayEquals(data, Base64.decode(enc));
	}
}
