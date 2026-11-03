package com.github.esrrhs.fakecore.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class MessageCodecEdgeTest
{
	@Test
	void serverDecodeEmptyBuffer()
	{
		ServerMessageCodec codec = new ServerMessageCodec();
		ByteBuf buf = Unpooled.buffer();
		assertNull(codec.decode(buf));
		buf.release();
	}

	@Test
	void clientDecodeEmptyAndTooShort()
	{
		ClientMessageCodec codec = new ClientMessageCodec("k");
		ByteBuf empty = Unpooled.buffer();
		assertNull(codec.decode(empty));
		empty.release();

		ByteBuf shortBuf = Unpooled.buffer();
		shortBuf.writeBytes(new byte[] { 1, 2, 3 });
		assertNull(codec.decode(shortBuf));
		shortBuf.release();
	}

	@Test
	void clientDecodeNegativePayloadLengthThrows()
	{
		ClientMessageCodec codec = new ClientMessageCodec("k");
		ByteBuf buf = Unpooled.buffer();
		buf.writeInt(1); // length claims only 1 byte after int, but short needs 2 -> length-2 = -1
		buf.writeShort((short) 1);
		assertThrows(RuntimeException.class, () -> codec.decode(buf));
		buf.release();
	}

	@Test
	void serverEncodeEmptyPayload()
	{
		ServerMessageCodec codec = new ServerMessageCodec();
		Message msg = new Message();
		msg.setId((short) 0);
		msg.setSessionId("0");
		msg.setData(new byte[0]);
		ByteBuf out = Unpooled.buffer();
		codec.encode(msg, out);
		Message decoded = codec.decode(out);
		assertEquals(0, decoded.getId());
		assertEquals(0, decoded.getData().length);
		out.release();
	}
}
