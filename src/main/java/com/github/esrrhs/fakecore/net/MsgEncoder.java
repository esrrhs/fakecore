package com.github.esrrhs.fakecore.net;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MsgEncoder extends MessageToByteEncoder<Object>
{
	private MessageCodec messageCodec;

	public MsgEncoder(MessageCodec messageCodec)
	{
		this.messageCodec = messageCodec;
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, Object o, ByteBuf out) throws Exception
	{
		messageCodec.encode((Message) o, out);
	}
}
