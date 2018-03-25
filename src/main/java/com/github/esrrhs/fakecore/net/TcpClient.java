package com.github.esrrhs.fakecore.net;

import io.netty.channel.EventLoopGroup;

/**
 * Created by esrrhs on 2018/2/5.
 */
public class TcpClient extends TcpObject
{
	private TcpClientThread tcpClientThread;

	public void ini(EventLoopGroup group, int gsId, String gsName, String ip, int port, int queueSize,
			TcpHandler tcpHandler, MessageCodec messageCodec)
	{
		this.tcpClientThread = new TcpClientThread();
		this.tcpClientThread.ini(group, gsId, gsName, ip, port, queueSize, tcpHandler, messageCodec);
	}

	public void sendMsg(Message message)
	{
		this.tcpClientThread.sendMsg(message);
	}

	public boolean isConnected()
	{
		return this.tcpClientThread.isConnected();
	}

	public int getGsId()
	{
		return this.tcpClientThread.getGsId();
	}

	public long getCreateTime()
	{
		return this.tcpClientThread.getCreateTime();
	}

	public String getIp()
	{
		return this.tcpClientThread.getIp();
	}

	public int getPort()
	{
		return this.tcpClientThread.getPort();
	}

	public String getGsName()
	{
		return this.tcpClientThread.getGsName();
	}

	public void closeChannel()
	{
		this.tcpClientThread.closeChannel();
	}
}
