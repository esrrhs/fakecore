package com.github.esrrhs.fakecore.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.codec.DecoderException;
import org.junit.jupiter.api.Test;

class CodecUtilMoreTest
{
	@Test
	void hexEncodeDecode() throws DecoderException
	{
		assertNull(CodecUtil.hexEncode(null));
		assertNull(CodecUtil.hexEncode(new byte[0]));
		assertNull(CodecUtil.hexDecode(null));
		assertNull(CodecUtil.hexDecode(""));
		byte[] data = new byte[] { 0x0a, 0x1b };
		String hex = CodecUtil.hexEncode(data);
		assertEquals("0a1b", hex);
		assertArrayEquals(data, CodecUtil.hexDecode(hex));
	}

	@Test
	void base64UrlAndBase36()
	{
		assertNull(CodecUtil.base64UrlEncode(null));
		assertNull(CodecUtil.base64UrlEncode(new byte[0]));
		byte[] data = "url+".getBytes();
		String urlSafe = CodecUtil.base64UrlEncode(data);
		assertNotNull(urlSafe);
		assertArrayEquals(data, CodecUtil.base64Decode(urlSafe));

		assertNull(CodecUtil.base36Encode(null));
		assertNull(CodecUtil.base36Encode(""));
		String enc = CodecUtil.base36Encode("ff");
		assertNotNull(enc);
		assertEquals("ff", CodecUtil.base36Decode(enc));

		assertNull(CodecUtil.base36Encode(null, 4));
		String padded = CodecUtil.base36Encode("1", 4);
		assertEquals(4, padded.length());
		assertTrue(padded.endsWith(CodecUtil.base36Encode("1")));

		assertThrows(IllegalArgumentException.class, () -> CodecUtil.base36Encode("1", 65));
		assertThrows(IllegalArgumentException.class, () -> CodecUtil.base36Decode("!!"));
		assertNull(CodecUtil.base36Decode(null));
		assertNull(CodecUtil.base36Decode(""));
	}

	@Test
	void urlHtmlXmlHelpers()
	{
		assertNull(CodecUtil.urlEncode(null));
		assertNull(CodecUtil.urlEncode("  "));
		assertEquals("a+b", CodecUtil.urlEncode("a b"));
		assertEquals("a b", CodecUtil.urlDecode("a+b"));
		assertNull(CodecUtil.urlDecode(null));
		assertEquals("", CodecUtil.urlDecode(""));
		assertThrows(IllegalArgumentException.class, () -> CodecUtil.urlEncode("x", "no-such-enc"));
		assertThrows(IllegalArgumentException.class, () -> CodecUtil.urlDecode("x", "no-such-enc"));

		assertNull(CodecUtil.htmlEscape(null));
		assertNull(CodecUtil.htmlEscape(""));
		assertNull(CodecUtil.htmlUnescape(null));
		String html = "<a>";
		assertEquals(html, CodecUtil.htmlUnescape(CodecUtil.htmlEscape(html)));

		assertNull(CodecUtil.xmlEscape(null));
		assertNull(CodecUtil.xmlUnescape(""));
		String xml = "<tag>&</tag>";
		assertEquals(xml, CodecUtil.xmlUnescape(CodecUtil.xmlEscape(xml)));
	}
}
