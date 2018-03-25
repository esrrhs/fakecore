package com.github.esrrhs.fakecore.net;

import io.netty.buffer.ByteBuf;

/**
 * Created by esrrhs on 2018/2/9.
 */
public interface MessageCodec
{
    Message decode(ByteBuf in);
	void encode(Message message, ByteBuf out);
}
