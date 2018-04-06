package com.github.esrrhs.fakecore.net;

import org.java_websocket.WebSocket;

import io.netty.channel.Channel;

/**
 * Created by esrrhs on 2018/4/6.
 */
public class Link
{
	private Channel channel;
	private TcpObject tcpObject;
	private WebSocket webSocket;
	private WSObject wsObject;

	public Link()
	{
	}

	public Link(Channel channel, TcpObject tcpObject)
	{
		this.channel = channel;
		this.tcpObject = tcpObject;
	}

	public Link(WebSocket webSocket, WSObject wsObject)
	{
		this.wsObject = wsObject;
		this.webSocket = webSocket;
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

	public void close()
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

	public void send(Object object)
	{
		if (getChannel() != null)
		{
			getChannel().writeAndFlush(object);
		}
		else if (getWebSocket() != null)
		{
			if (object instanceof String)
			{
				getWebSocket().send((String) object);
			}
			else if (object instanceof byte[])
			{
				getWebSocket().send((byte[]) object);
			}
		}
	}
}