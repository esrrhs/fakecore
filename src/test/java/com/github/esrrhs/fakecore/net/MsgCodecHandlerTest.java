package com.github.esrrhs.fakecore.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;

class MsgCodecHandlerTest
{
	@Test
	void serverEncoderDecoderThroughPipeline()
	{
		ServerMessageCodec codec = new ServerMessageCodec();
		EmbeddedChannel ch = new EmbeddedChannel(new MsgEncoder(codec), new MsgDecoder(codec));

		Message msg = new Message();
		msg.setId((short) 11);
		msg.setSessionId(Long.toString(99L, 36));
		msg.setData("hi".getBytes(StandardCharsets.UTF_8));

		assertEquals(true, ch.writeOutbound(msg));
		ByteBuf encoded = ch.readOutbound();
		assertEquals(true, ch.writeInbound(encoded));

		Message decoded = ch.readInbound();
		assertEquals(11, decoded.getId());
		assertEquals(msg.getSessionId(), decoded.getSessionId());
		assertArrayEquals(msg.getData(), decoded.getData());
		ch.finish();
	}

	@Test
	void decoderWaitsForFullFrame()
	{
		ServerMessageCodec codec = new ServerMessageCodec();
		EmbeddedChannel ch = new EmbeddedChannel(new MsgDecoder(codec));
		ByteBuf partial = io.netty.buffer.Unpooled.buffer();
		partial.writeInt(100);
		partial.writeBytes(new byte[] { 1, 2 });
		ch.writeInbound(partial);
		assertNull(ch.readInbound());
		ch.finish();
	}

	@Test
	void clientEncoderDecoderThroughPipeline()
	{
		ClientMessageCodec codec = new ClientMessageCodec("pipe-key");
		EmbeddedChannel ch = new EmbeddedChannel(new MsgEncoder(codec), new MsgDecoder(codec));
		Message msg = new Message();
		msg.setId((short) 5);
		msg.setData("z".getBytes(StandardCharsets.UTF_8));
		ch.writeOutbound(msg);
		ByteBuf encoded = ch.readOutbound();
		ch.writeInbound(encoded);
		Message decoded = ch.readInbound();
		assertEquals(5, decoded.getId());
		assertArrayEquals(msg.getData(), decoded.getData());
		ch.finish();
	}
}
