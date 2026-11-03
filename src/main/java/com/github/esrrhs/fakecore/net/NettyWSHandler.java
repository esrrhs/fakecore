package com.github.esrrhs.fakecore.net;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Callbacks for {@link NettyWSServer} / {@link NettyWSClient}.
 */
public class NettyWSHandler
{
	private static final Logger log = LoggerFactory.getLogger(NettyWSHandler.class);

	public void onOpen(Channel channel)
	{
	}

	public void onClose(Channel channel)
	{
	}

	public void onMessage(Channel channel, String text)
	{
	}

	public void onMessage(Channel channel, ByteBuf data)
	{
	}

	public void onError(Channel channel, Throwable cause)
	{
		log.error("NettyWS onError {}", channel, cause);
	}

	public void onStart()
	{
	}
}
