package com.github.esrrhs.fakecore.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class MessageTest
{
	@Test
	void accessorsAndNestedTypes()
	{
		Message m = new Message();
		assertEquals(0, m.getId());
		assertEquals("0", m.getSessionId());
		assertArrayEquals(new byte[0], m.getData());
		assertNull(m.getLink());

		m.setId(Message.MSG_ID_PING);
		m.setSessionId("abc");
		byte[] data = new byte[] { 9 };
		m.setData(data);
		Link link = new Link();
		m.setLink(link);

		assertEquals(Message.MSG_ID_PING, m.getId());
		assertEquals("abc", m.getSessionId());
		assertSame(data, m.getData());
		assertSame(link, m.getLink());

		Message.PingPongData ping = new Message.PingPongData();
		ping.gsId = 1;
		ping.time = 2L;
		assertEquals(1, ping.gsId);
		assertEquals(2L, ping.time);

		Message.HandShakeData hs = new Message.HandShakeData();
		hs.gsId = 3;
		assertEquals(3, hs.gsId);

		assertEquals(-1, Message.MSG_ID_CLOSE);
		assertEquals(-2, Message.MSG_ID_PING);
		assertEquals(-3, Message.MSG_ID_PONG);
		assertEquals(-4, Message.MSG_ID_HANDSHAKE);
	}
}
