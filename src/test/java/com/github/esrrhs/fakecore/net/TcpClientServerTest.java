package com.github.esrrhs.fakecore.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.esrrhs.fakecore.support.TestSockets;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

class TcpClientServerTest
{
	private final List<TcpServer> servers = new CopyOnWriteArrayList<>();
	private final List<TcpClient> clients = new CopyOnWriteArrayList<>();
	private final List<EventLoopGroup> groups = new CopyOnWriteArrayList<>();

	@AfterEach
	void cleanup()
	{
		for (TcpClient client : clients)
		{
			client.shutdown();
		}
		for (TcpClient client : clients)
		{
			try
			{
				if (client != null)
				{
					// thread may already be finished
				}
			}
			catch (Exception ignored)
			{
			}
		}
		for (TcpServer server : servers)
		{
			server.stop();
		}
		for (EventLoopGroup group : groups)
		{
			group.shutdownGracefully(0, 2, TimeUnit.SECONDS).syncUninterruptibly();
		}
		clients.clear();
		servers.clear();
		groups.clear();
	}

	@Test
	void connectHandshakeExchangeAndRelease()
	{
		int port = TestSockets.freePort();
		RecordingHandler serverHandler = new RecordingHandler(null);
		TcpServer server = startServer(port, serverHandler);

		EventLoopGroup group = newGroup();
		RecordingHandler clientHandler = new RecordingHandler(null);
		TcpClient client = startClient(group, port, clientHandler);
		clientHandler.host = client;

		TestSockets.await(client::isConnected, 5000, "client should connect");
		assertTrue(server.listend());
		TestSockets.await(() -> countById(serverHandler.messages, Message.MSG_ID_HANDSHAKE) >= 1, 5000,
				"server should receive handshake");

		Channel serverChild = serverHandler.lastChannel();
		assertTrue(serverChild != null && serverChild.isActive());

		Message ping = new Message();
		ping.setId(Message.MSG_ID_PING);
		ping.setData("{\"gsId\":1,\"time\":1}".getBytes(StandardCharsets.UTF_8));
		server.sendMsg(serverChild, ping);
		TestSockets.await(() -> countById(clientHandler.messages, Message.MSG_ID_PING) >= 1, 5000,
				"client should receive ping");
		TestSockets.await(() -> countById(serverHandler.messages, Message.MSG_ID_PONG) >= 1, 5000,
				"server should receive pong reply");

		server.stop();
		assertFalse(server.listend());
		client.shutdown();
		TestSockets.await(() -> !client.isConnected(), 3000, "client should be disconnected after shutdown");
	}

	@Test
	void clientReconnectsAfterDisconnect()
	{
		int port = TestSockets.freePort();
		RecordingHandler serverHandler = new RecordingHandler(null);
		startServer(port, serverHandler);

		EventLoopGroup group = newGroup();
		TcpClient client = startClient(group, port, new RecordingHandler(null));
		TestSockets.await(client::isConnected, 5000, "initial connect");
		TestSockets.await(() -> countById(serverHandler.messages, Message.MSG_ID_HANDSHAKE) >= 1, 5000,
				"initial handshake");

		client.closeChannel();
		TestSockets.await(() -> !client.isConnected(), 3000, "channel closed");
		TestSockets.await(client::isConnected, 8000, "client should reconnect");
		TestSockets.await(() -> countById(serverHandler.messages, Message.MSG_ID_HANDSHAKE) >= 2, 5000,
				"reconnect should handshake again");
	}

	@Test
	void clientReconnectsWhenServerAppearsAfterRefusedConnect()
	{
		int port = TestSockets.freePort();
		EventLoopGroup group = newGroup();
		TcpClient client = startClient(group, port, new RecordingHandler(null));
		TestSockets.await(() -> !client.isConnected(), 2000, "no server yet");

		RecordingHandler serverHandler = new RecordingHandler(null);
		startServer(port, serverHandler);
		TestSockets.await(client::isConnected, 8000, "client should connect once server is up");
		TestSockets.await(() -> countById(serverHandler.messages, Message.MSG_ID_HANDSHAKE) >= 1, 5000,
				"handshake after delayed server start");
	}

	@Test
	void concurrentClientsExchangeAndRelease() throws Exception
	{
		int port = TestSockets.freePort();
		RecordingHandler serverHandler = new RecordingHandler(null);
		startServer(port, serverHandler);

		int n = 4;
		EventLoopGroup group = newGroup();
		for (int i = 0; i < n; i++)
		{
			startClient(group, port, new RecordingHandler(null));
		}
		TestSockets.await(() -> clients.stream().filter(TcpClient::isConnected).count() == n, 8000,
				"all clients should connect");
		TestSockets.await(() -> countById(serverHandler.messages, Message.MSG_ID_HANDSHAKE) >= n, 5000,
				"server should see all handshakes");

		AtomicInteger sent = new AtomicInteger();
		for (Channel ch : serverHandler.channels)
		{
			if (ch.isActive())
			{
				Message msg = new Message();
				msg.setId((short) 21);
				msg.setData(("c" + sent.getAndIncrement()).getBytes(StandardCharsets.UTF_8));
				servers.get(0).sendMsg(ch, msg);
			}
		}

		for (TcpClient client : clients)
		{
			client.shutdown();
		}
		for (TcpClient client : clients)
		{
			assertFalse(client.isConnected());
		}
		servers.get(0).stop();
		assertFalse(servers.get(0).listend());
	}

	private TcpServer startServer(int port, RecordingHandler handler)
	{
		TcpServer server = new TcpServer();
		handler.host = server;
		server.ini(port, handler, new ServerMessageCodec());
		servers.add(server);
		return server;
	}

	private TcpClient startClient(EventLoopGroup group, int port, RecordingHandler handler)
	{
		TcpClient client = new TcpClient();
		handler.host = client;
		client.ini(group, 7, "it-client", "127.0.0.1", port, 32, handler, new ServerMessageCodec());
		clients.add(client);
		return client;
	}

	private EventLoopGroup newGroup()
	{
		EventLoopGroup group = new NioEventLoopGroup(1);
		groups.add(group);
		return group;
	}

	private static long countById(List<Message> messages, short id)
	{
		return messages.stream().filter(m -> m.getId() == id).collect(Collectors.counting());
	}

	static final class RecordingHandler extends TcpHandler
	{
		volatile TcpObject host;
		final List<Message> messages = new CopyOnWriteArrayList<>();
		final List<Channel> channels = new CopyOnWriteArrayList<>();

		RecordingHandler(TcpObject host)
		{
			super(host);
			this.host = host;
		}

		@Override
		public void channelRegistered(ChannelHandlerContext ctx) throws Exception
		{
			channels.add(ctx.channel());
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception
		{
			channels.remove(ctx.channel());
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, Message message) throws Exception
		{
			messages.add(message);
			super.channelRead0(ctx, message);
		}

		Channel lastChannel()
		{
			return channels.isEmpty() ? null : channels.get(channels.size() - 1);
		}
	}
}
