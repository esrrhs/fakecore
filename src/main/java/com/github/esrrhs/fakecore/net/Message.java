package com.github.esrrhs.fakecore.net;

import org.java_websocket.WebSocket;

import io.netty.channel.Channel;

public class Message
{
	public static class PingPongData
	{
		public int gsId;
		public long time;
	}

	public static class HandShakeData
	{
		public int gsId;
	}

	public static final short MSG_ID_CLOSE = -1;
	public static final short MSG_ID_PING = -2;
	public static final short MSG_ID_PONG = -3;
	public static final short MSG_ID_HANDSHAKE = -4;

	private short id;
	private byte[] data = new byte[0];
	private String sessionId = "0";
	private Channel channel;
	private TcpObject tcpObject;
	private WebSocket webSocket;
	private WSObject wsObject;

	public Object getLink()
	{
		if (getChannel() != null)
		{
			return getChannel();
		}
		else if (getWebSocket() != null)
		{
			return getWebSocket();
		}
		return null;
	}

	public short getId()
	{
		return id;
	}

	public void setId(short id)
	{
		this.id = id;
	}

	public byte[] getData()
	{
		return data;
	}

	public void setData(byte[] data)
	{
		this.data = data;
	}

	public String getSessionId()
	{
		return sessionId;
	}

	public void setSessionId(String sessionId)
	{
		this.sessionId = sessionId;
	}

	public Channel getChannel()
	{
		return channel;
	}

	public void setChannel(Channel channel)
	{
		this.channel = channel;
	}

	public TcpObject getTcpObject()
	{
		return tcpObject;
	}

	public void setTcpObject(TcpObject tcpObject)
	{
		this.tcpObject = tcpObject;
	}

	public WebSocket getWebSocket()
	{
		return webSocket;
	}

	public void setWebSocket(WebSocket webSocket)
	{
		this.webSocket = webSocket;
	}

	public WSObject getWsObject()
	{
		return wsObject;
	}

	public void setWsObject(WSObject wsObject)
	{
		this.wsObject = wsObject;
	}

	public String getRemoteAddress()
	{
		if (getChannel() != null)
		{
			return getChannel().remoteAddress().toString();
		}
		else if (getWebSocket() != null)
		{
			return getWebSocket().getRemoteSocketAddress().toString();
		}
		return "";
	}

	public String getLocalAddress()
	{
		if (getChannel() != null)
		{
			return getChannel().localAddress().toString();
		}
		else if (getWebSocket() != null)
		{
			return getWebSocket().getLocalSocketAddress().toString();
		}
		return "";
	}

	public void closeLink()
	{
		if (getChannel() != null)
		{
			getChannel().close();
		}
		else if (getWebSocket() != null)
		{
			getWebSocket().close();
		}
	}

	public static void closeLink(Object link)
	{
		if (link instanceof Channel)
		{
			((Channel) link).close();
		}
		else if (link instanceof WebSocket)
		{
			((WebSocket) link).close();
		}
	}
}
