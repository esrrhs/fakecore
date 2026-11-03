package com.github.esrrhs.fakecore.codec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Thin Base64 + zlib helpers. Encode/decode delegates to commons-codec while keeping
 * the historical package API (including compress/decompress).
 *
 * @deprecated Prefer {@link CodecUtil#base64Encode(byte[])} / {@link CodecUtil#base64Decode(String)}
 *             for new code. This class remains for binary compatibility.
 */
@Deprecated
public class Base64
{
	public static String encodeS(byte[] data)
	{
		if (data == null)
		{
			return null;
		}
		return org.apache.commons.codec.binary.Base64.encodeBase64String(data);
	}

	public static byte[] encode(byte[] data)
	{
		if (data == null)
		{
			return null;
		}
		return org.apache.commons.codec.binary.Base64.encodeBase64(data);
	}

	public static byte[] decode(byte[] data)
	{
		if (data == null)
		{
			return null;
		}
		return org.apache.commons.codec.binary.Base64.decodeBase64(data);
	}

	public static byte[] decode(String data)
	{
		if (data == null)
		{
			return null;
		}
		return org.apache.commons.codec.binary.Base64.decodeBase64(data);
	}

	public static byte[] compressBytes(byte input[])
	{
		int cachesize = 1024;

		Deflater compresser = new Deflater();

		compresser.reset();
		compresser.setInput(input);
		compresser.finish();
		byte output[] = new byte[0];
		ByteArrayOutputStream o = new ByteArrayOutputStream(input.length);
		try
		{
			byte[] buf = new byte[cachesize];
			int got;
			while (!compresser.finished())
			{
				got = compresser.deflate(buf);
				o.write(buf, 0, got);
			}
			output = o.toByteArray();
		}
		finally
		{
			try
			{
				o.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return output;
	}

	public static byte[] decompressBytes(byte input[])
	{
		int cachesize = 1024;
		Inflater decompresser = new Inflater();

		byte output[] = new byte[0];
		decompresser.reset();
		decompresser.setInput(input);
		ByteArrayOutputStream o = new ByteArrayOutputStream(input.length);
		try
		{
			byte[] buf = new byte[cachesize];
			int got;
			while (!decompresser.finished())
			{
				got = decompresser.inflate(buf);
				o.write(buf, 0, got);
			}
			output = o.toByteArray();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				o.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return output;
	}

}
