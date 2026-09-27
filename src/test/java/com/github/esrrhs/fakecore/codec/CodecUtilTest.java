package com.github.esrrhs.fakecore.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
	void htmlEscapeRoundTrip()
	{
		String html = "<div>\"a&b\"</div>";
		String escaped = CodecUtil.htmlEscape(html);
		assertNotNull(escaped);
		assertEquals(html, CodecUtil.htmlUnescape(escaped));
	}
}
