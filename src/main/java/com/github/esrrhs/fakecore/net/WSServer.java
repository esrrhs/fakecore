package com.github.esrrhs.fakecore.net;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Created by esrrhs on 2018/3/20.
 */
public class WSServer extends WSObject
{
	private static Logger log = LoggerFactory.getLogger(WSServer.class);
	private int port;
	private WebSocketServer webSocketServer;
	private WSHandler wsHandler;

	public void ini(int port, WSHandler wsHandler)
	{
		this.port = port;
		this.wsHandler = wsHandler;
		this.webSocketServer = new WebSocketServer(new InetSocketAddress(this.port)) {

			@Override
			public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake)
			{
				wsHandler.onOpen(webSocket, clientHandshake);
			}

			@Override
			public void onClose(WebSocket webSocket, int code, String reason, boolean remote)
			{
				wsHandler.onClose(webSocket, code, reason, remote);
			}

			@Override
			public void onMessage(WebSocket webSocket, String s)
			{
				wsHandler.onMessage(webSocket, s);
			}

			@Override
			public void onMessage(WebSocket webSocket, ByteBuffer message)
			{
				wsHandler.onMessage(webSocket, message);
			}

			@Override
			public void onError(WebSocket webSocket, Exception e)
			{
				wsHandler.onError(webSocket, e);
			}

			@Override
			public void onStart()
			{
				wsHandler.onStart();
			}

		};
		this.webSocketServer.start();
	}

	public void sendMsg(WebSocket webSocket, Message msg)
	{
		webSocket.send(new String(msg.getData(), StandardCharsets.UTF_8));
	}

	public void onStart()
	{
		log.info("WSServer listnening on port {}", port);
	}
}
