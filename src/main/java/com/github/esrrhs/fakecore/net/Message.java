package com.github.esrrhs.fakecore.net;

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
}
