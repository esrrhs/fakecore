package com.github.esrrhs.fakecore.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LinkTest
{
	@Test
	void accessorsWithoutNetwork()
	{
		Link link = new Link();
		assertNull(link.getChannel());
		assertNull(link.getTcpObject());
		assertNull(link.getWebSocket());
		assertNull(link.getWsObject());
		assertNull(link.getLink());
		assertEquals("", link.getRemoteAddress());
		assertEquals("", link.getLocalAddress());

		TcpObject tcp = new TcpObject();
		link.setTcpObject(tcp);
		assertSame(tcp, link.getTcpObject());

		WSObject ws = new WSObject();
		link.setWsObject(ws);
		assertSame(ws, link.getWsObject());

		link.close();
		link.send("noop");
		link.send(new byte[] { 1 });
	}

	@Test
	void constructorsAndEqualsHashCode()
	{
		TcpObject tcp = new TcpObject();
		Link a = new Link(null, tcp);
		Link b = new Link(null, tcp);
		assertSame(tcp, a.getTcpObject());
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a, a);
		assertNotEquals(a, null);
		assertNotEquals(a, "x");

		WSObject ws = new WSObject();
		Link wsLink = new Link((org.java_websocket.WebSocket) null, ws);
		assertSame(ws, wsLink.getWsObject());
		assertNotEquals(a, wsLink);
		assertTrue(wsLink.hashCode() != 0 || wsLink.hashCode() == 0);
	}
}
