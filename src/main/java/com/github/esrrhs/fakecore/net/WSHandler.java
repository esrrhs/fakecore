package com.github.esrrhs.fakecore.net;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.Handshakedata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Created by esrrhs on 2018/3/20.
 */
public class WSHandler
{
	private static Logger log = LoggerFactory.getLogger(WSHandler.class);

	private WSObject host;

	public WSHandler(WSObject host)
	{
		this.host = host;
	}

	public void onOpen(WebSocket webSocket, Handshakedata clientHandshake)
	{

	}

	public void onClose(WebSocket webSocket, int code, String reason, boolean remote)
	{

	}

	public void onMessage(WebSocket webSocket, String s)
	{

	}

	public void onMessage(WebSocket webSocket, ByteBuffer message)
	{
	}

	public void onError(WebSocket webSocket, Exception e)
	{
		log.error("onError " + webSocket.getRemoteSocketAddress(), e);
	}

	public void onStart()
	{
		if (host instanceof WSServer)
		{
			((WSServer) host).onStart();
		}
	}
}
