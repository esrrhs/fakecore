package com.github.esrrhs.fakecore.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class MessageCodecTest
{
	@Test
	void serverCodecRoundTrip()
	{
		ServerMessageCodec codec = new ServerMessageCodec();
		Message msg = new Message();
		msg.setId((short) 42);
		msg.setSessionId(Long.toString(123456789L, 36));
		msg.setData("payload".getBytes(StandardCharsets.UTF_8));

		ByteBuf buf = Unpooled.buffer();
		codec.encode(msg, buf);

		Message decoded = codec.decode(buf);
		assertEquals(msg.getId(), decoded.getId());
		assertEquals(msg.getSessionId(), decoded.getSessionId());
		assertArrayEquals(msg.getData(), decoded.getData());
		buf.release();
	}

	@Test
	void serverCodecIncompleteReturnsNull()
	{
		ServerMessageCodec codec = new ServerMessageCodec();
		ByteBuf buf = Unpooled.buffer();
		buf.writeInt(100);
		buf.writeBytes(new byte[] { 1, 2, 3 });
		assertNull(codec.decode(buf));
		buf.release();
	}

	@Test
	void clientCodecRoundTripWithRc4()
	{
		String key = "unit-test-key";
		ClientMessageCodec codec = new ClientMessageCodec(key);
		Message msg = new Message();
		msg.setId((short) 7);
		msg.setData("secret-bytes".getBytes(StandardCharsets.UTF_8));

		ByteBuf buf = Unpooled.buffer();
		codec.encode(msg, buf);
		Message decoded = codec.decode(buf);
		assertEquals(7, decoded.getId());
		assertArrayEquals(msg.getData(), decoded.getData());
		buf.release();
	}

	@Test
	void clientCodecBoundaryNeedsMoreBytes()
	{
		ClientMessageCodec codec = new ClientMessageCodec("k");
		ByteBuf buf = Unpooled.buffer();
		buf.writeInt(50);
		assertNull(codec.decode(buf));
		buf.release();
	}
}
