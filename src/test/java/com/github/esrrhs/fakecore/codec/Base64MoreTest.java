package com.github.esrrhs.fakecore.codec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class Base64MoreTest
{
	@Test
	void nullGuards()
	{
		assertNull(Base64.encodeS(null));
		assertNull(Base64.encode(null));
		assertNull(Base64.decode((byte[]) null));
		assertNull(Base64.decode((String) null));
	}

	@Test
	void compressRoundTrip()
	{
		byte[] input = "compress me please for fakecore coverage".getBytes(StandardCharsets.UTF_8);
		byte[] compressed = Base64.compressBytes(input);
		byte[] round = Base64.decompressBytes(compressed);
		assertArrayEquals(input, round);
	}

	@Test
	void encodeDecodeBytes()
	{
		byte[] data = "xyz".getBytes(StandardCharsets.UTF_8);
		byte[] enc = Base64.encode(data);
		assertArrayEquals(data, Base64.decode(enc));
	}
}
