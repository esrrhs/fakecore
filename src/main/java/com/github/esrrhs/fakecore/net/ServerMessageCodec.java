package com.github.esrrhs.fakecore.net;

import io.netty.buffer.ByteBuf;

/**
 * Created by esrrhs on 2018/2/9.
 */
public class ServerMessageCodec implements MessageCodec
{
	@Override
	public Message decode(ByteBuf in)
	{
		in.markReaderIndex();
		if (!in.isReadable() || in.readableBytes() < 4 + 8 + 2)
		{
			in.resetReaderIndex();
			return null;
		}

		int length = in.readInt();
		if (in.readableBytes() < length)
		{
			in.resetReaderIndex();
			return null;
		}

		long sessid = in.readLong();
		String sessionId = Long.toString(sessid, 36);
		length -= 8;

		short msgid = in.readShort();
		length -= 2;

		Message m = new Message();
		m.setId(msgid);
		byte[] bytes = new byte[length];
		in.readBytes(bytes);
		m.setData(bytes);
		m.setSessionId(sessionId);

		return m;
	}

	@Override
	public void encode(Message message, ByteBuf out)
	{
		int length = message.getData().length + 8 + 2;

		out.writeInt(length);
		long sessid = Long.valueOf(message.getSessionId(), 36);
		out.writeLong(sessid);
		out.writeShort(message.getId());
		out.writeBytes(message.getData());
	}
}
