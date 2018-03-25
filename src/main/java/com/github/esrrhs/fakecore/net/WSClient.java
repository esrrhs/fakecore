package com.github.esrrhs.fakecore.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by esrrhs on 2018/3/20.
 */
public class WSClient extends WSObject
{
	private static Logger log = LoggerFactory.getLogger(WSClient.class);
	private WSClientThread wsClientThread;

	public void ini(String ip, int port, WSHandler wsHandler, int queueSize)
	{
		this.wsClientThread = new WSClientThread();
		this.wsClientThread.ini(ip, port, wsHandler, queueSize);
	}

	public void sendMsg(Message message)
	{
		this.wsClientThread.sendMsg(message);
	}
}
