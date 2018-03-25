package com.github.esrrhs.fakecore.net;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class MsgDecoder extends ByteToMessageDecoder
{
	private MessageCodec messageCodec;

	public MsgDecoder(MessageCodec messageCodec)
	{
		this.messageCodec = messageCodec;
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
	{
		Message m = messageCodec.decode(in);
		if (m != null)
		{
			out.add(m);
		}
	}
}
