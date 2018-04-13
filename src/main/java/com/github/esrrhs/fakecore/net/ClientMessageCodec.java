package com.github.esrrhs.fakecore.net;

import com.github.esrrhs.fakecore.codec.EncryptUtil;

import io.netty.buffer.ByteBuf;

/**
 * Created by esrrhs on 2018/2/9.
 */
public class ClientMessageCodec implements MessageCodec
{
	private String key;

	public ClientMessageCodec(String key)
	{
		this.key = key;
	}

	@Override
	public Message decode(ByteBuf in)
	{
		in.markReaderIndex();
		if (!in.isReadable() || in.readableBytes() < 6)
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

		short msgid = in.readShort();
		length -= 2;

		if (length < 0)
		{
			throw new RuntimeException("length < 0 " + length);
		}

		Message m = new Message();
		m.setId(msgid);
		byte[] bytes = new byte[length];
		in.readBytes(bytes);

		m.setData(EncryptUtil.rc4Decrypt(bytes, key));

		return m;
	}

	@Override
	public void encode(Message message, ByteBuf out)
	{
		byte[] data = message.getData();

		data = EncryptUtil.rc4Encrypt(data, key);

		int length = data.length + 2;

		out.writeInt(length);
		out.writeShort(message.getId());
		out.writeBytes(data);
	}
}
