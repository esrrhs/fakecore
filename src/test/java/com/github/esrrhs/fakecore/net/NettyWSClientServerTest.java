package com.github.esrrhs.fakecore.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.esrrhs.fakecore.support.TestSockets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

class NettyWSClientServerTest
{
	private final List<NettyWSServer> servers = new CopyOnWriteArrayList<>();
	private final List<NettyWSClient> clients = new CopyOnWriteArrayList<>();

	@AfterEach
	void cleanup()
	{
		for (NettyWSClient client : clients)
		{
			client.close();
		}
		for (NettyWSServer server : servers)
		{
			server.stop();
		}
		clients.clear();
		servers.clear();
	}

	@Test
	void textBinaryExchangeAndRelease()
	{
		int port = TestSockets.freePort();
		RecordingHandler serverHandler = new RecordingHandler();
		NettyWSServer server = startServer(port, serverHandler);
		assertTrue(server.isListening());

		RecordingHandler clientHandler = new RecordingHandler();
		NettyWSClient client = startClient(port, clientHandler);
		TestSockets.await(client::isConnected, 5000, "websocket client should connect");
		TestSockets.await(() -> serverHandler.opens.get() >= 1 && clientHandler.opens.get() >= 1, 5000,
				"both sides should see open");

		client.sendText("hello-ws");
		TestSockets.await(() -> "hello-ws".equals(serverHandler.lastText.get()), 5000, "server should get text");

		Channel serverChild = serverHandler.lastChannel.get();
		server.sendText(serverChild, "from-server");
		TestSockets.await(() -> "from-server".equals(clientHandler.lastText.get()), 5000, "client should get text");

		byte[] payload = new byte[] { 1, 2, 3, 4 };
		client.sendBinary(payload);
		TestSockets.await(() -> serverHandler.lastBinary.get() != null, 5000, "server should get binary");
		assertArrayEquals(payload, serverHandler.lastBinary.get());

		Message msg = new Message();
		msg.setData("via-msg".getBytes(StandardCharsets.UTF_8));
		server.sendMsg(serverChild, msg);
		TestSockets.await(() -> "via-msg".equals(clientHandler.lastText.get()), 5000, "sendMsg should deliver text");

		client.close();
		TestSockets.await(() -> !client.isConnected(), 3000, "client close should disconnect");
		server.stop();
		assertFalse(server.isListening());
	}

	@Test
	void clientReconnectsAfterServerRestart()
	{
		int port = TestSockets.freePort();
		RecordingHandler serverHandler = new RecordingHandler();
		NettyWSServer server = startServer(port, serverHandler);
		RecordingHandler clientHandler = new RecordingHandler();
		NettyWSClient client = startClient(port, clientHandler);
		TestSockets.await(client::isConnected, 5000, "initial ws connect");
		TestSockets.await(() -> clientHandler.opens.get() >= 1, 5000, "initial ws open");
		int closesBefore = clientHandler.closes.get();

		server.stop();
		assertFalse(server.isListening());
		TestSockets.await(() -> clientHandler.closes.get() > closesBefore, 5000,
				"client should observe disconnect when server stops");

		RecordingHandler restarted = new RecordingHandler();
		startServer(port, restarted);
		TestSockets.await(client::isConnected, 8000, "client should reconnect");
		TestSockets.await(() -> restarted.opens.get() >= 1, 5000, "new server should see the reconnect");

		client.sendText("after-reconnect");
		TestSockets.await(() -> "after-reconnect".equals(restarted.lastText.get()), 5000,
				"traffic should work after reconnect");
	}

	@Test
	void concurrentClientsAndIdleClose() throws Exception
	{
		int port = TestSockets.freePort();
		RecordingHandler serverHandler = new RecordingHandler();
		NettyWSServer server = startServer(port, serverHandler);

		int n = 3;
		for (int i = 0; i < n; i++)
		{
			startClient(port, new RecordingHandler());
		}
		TestSockets.await(() -> clients.stream().filter(NettyWSClient::isConnected).count() == n, 8000,
				"all websocket clients should connect");
		TestSockets.await(() -> serverHandler.opens.get() >= n, 5000, "server should accept all clients");

		CountDownLatch latch = new CountDownLatch(n);
		int i = 0;
		for (NettyWSClient client : clients)
		{
			final String text = "c" + (i++);
			client.sendText(text);
			latch.countDown();
		}
		assertTrue(latch.await(1, TimeUnit.SECONDS));
		TestSockets.await(() -> serverHandler.texts.size() >= n, 5000, "server should receive concurrent texts");

		for (NettyWSClient client : clients)
		{
			client.close();
		}
		TestSockets.await(() -> clients.stream().noneMatch(NettyWSClient::isConnected), 5000,
				"all clients released");
		server.stop();
		assertFalse(server.isListening());
	}

	@Test
	void sendIgnoresNullAndDisconnected()
	{
		NettyWSClient client = new NettyWSClient();
		clients.add(client);
		client.sendText("nope");
		client.sendBinary(new byte[] { 1 });
		client.sendMsg(null);
		Message empty = new Message();
		empty.setData(null);
		client.sendMsg(empty);
		assertFalse(client.isConnected());
	}

	private NettyWSServer startServer(int port, RecordingHandler handler)
	{
		NettyWSServer server = new NettyWSServer();
		server.ini(port, handler);
		servers.add(server);
		return server;
	}

	private NettyWSClient startClient(int port, RecordingHandler handler)
	{
		NettyWSClient client = new NettyWSClient();
		client.ini("127.0.0.1", port, handler);
		clients.add(client);
		return client;
	}

	static final class RecordingHandler extends NettyWSHandler
	{
		final AtomicReference<Channel> lastChannel = new AtomicReference<>();
		final AtomicReference<String> lastText = new AtomicReference<>();
		final AtomicReference<byte[]> lastBinary = new AtomicReference<>();
		final List<String> texts = new CopyOnWriteArrayList<>();
		final java.util.concurrent.atomic.AtomicInteger opens = new java.util.concurrent.atomic.AtomicInteger();
		final java.util.concurrent.atomic.AtomicInteger closes = new java.util.concurrent.atomic.AtomicInteger();

		@Override
		public void onOpen(Channel channel)
		{
			lastChannel.set(channel);
			opens.incrementAndGet();
		}

		@Override
		public void onClose(Channel channel)
		{
			closes.incrementAndGet();
		}

		@Override
		public void onMessage(Channel channel, String text)
		{
			lastText.set(text);
			texts.add(text);
		}

		@Override
		public void onMessage(Channel channel, ByteBuf data)
		{
			try
			{
				byte[] bytes = new byte[data.readableBytes()];
				data.readBytes(bytes);
				lastBinary.set(bytes);
			}
			finally
			{
				data.release();
			}
		}
	}
}
