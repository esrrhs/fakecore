package com.github.esrrhs.fakecore.net;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by esrrhs on 2018/3/20.
 */
public class WSClientThread extends Thread
{
	private static Logger log = LoggerFactory.getLogger(WSClient.class);
	private String ip;
	private int port;
	private WebSocketClient webSocketClient;
	private WSHandler wsHandler;
	private ArrayBlockingQueue<Message> queue;

	public void ini(String ip, int port, WSHandler wsHandler, int queueSize)
	{
		URI uri = null;
		try
		{
			uri = new URI("ws://" + ip + ":" + port);
		}
		catch (Exception e)
		{
			log.error("ini fail " + ip + " " + port + "", e);
		}
		this.ip = ip;
		this.port = port;
		this.wsHandler = wsHandler;
		this.queue = new ArrayBlockingQueue<Message>(queueSize);
		this.webSocketClient = new WebSocketClient(uri) {
			@Override
			public void onOpen(ServerHandshake serverHandshake)
			{
				wsHandler.onOpen(this, serverHandshake);
			}

			@Override
			public void onMessage(String s)
			{
				wsHandler.onMessage(this, s);
			}

			@Override
			public void onMessage(ByteBuffer message)
			{
				wsHandler.onMessage(this, message);
			}

			@Override
			public void onClose(int code, String reason, boolean remote)
			{
				wsHandler.onClose(this, code, reason, remote);
			}

			@Override
			public void onError(Exception e)
			{
				wsHandler.onError(this, e);
			}
		};
		this.start();
	}

	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				if (!webSocketClient.isOpen())
				{
					webSocketClient.connectBlocking();
					if (!webSocketClient.isOpen())
					{
						Thread.sleep(1000);
						continue;
					}
				}

				if (queue.isEmpty())
				{
					sleep(10);
					continue;
				}

				for (int i = 0; i < 100; i++)
				{
					Message msg = queue.poll();
					if (msg != null)
					{
						write(msg);
					}
					else
					{
						break;
					}
				}
			}
			catch (Exception e)
			{
				log.error("WSClientThread run error " + ip + " " + port, e);
			}
		}
	}

	private void write(Message msg)
	{
		webSocketClient.send(new String(msg.getData(), StandardCharsets.UTF_8));
	}

	public void sendMsg(Message message)
	{
		try
		{
			queue.offer(message, 100, TimeUnit.MILLISECONDS);
		}
		catch (Exception e)
		{
			log.error("sendMsg ", e);
		}
	}
}
