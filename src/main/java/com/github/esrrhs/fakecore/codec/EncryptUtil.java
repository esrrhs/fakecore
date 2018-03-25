package com.github.esrrhs.fakecore.codec;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Random;

public class EncryptUtil
{
	private static Logger log = LoggerFactory.getLogger(EncryptUtil.class);

	private static final String Algorithm = "DESede"; //定义加密算法,可用 DES,DESede,Blowfish
	private static final byte[] ecodeStr =
	{ (byte) 0xef, 0x2b, (byte) 0xcc, (byte) 0xdc, (byte) 0x9b, 0x3b, (byte) 0xf7, 0x2a, 0x68, (byte) 0xad, (byte) 0xeb,
			0x72, (byte) 0xe3, 0x78, 0x2f, 0x5e, 0x7, 0x77, (byte) 0xd5, (byte) 0xc1, 0x7d, 0x40, 0x66, (byte) 0xb8 };

	//keybyte为加密密钥，长度为24字节
	//src为被加密的数据缓冲区（源）
	public static byte[] encryptMode(byte[] keybyte, byte[] src)
	{
		try
		{
			//生成密钥
			SecretKey deskey = new SecretKeySpec(keybyte, Algorithm);
			//加密
			Cipher c1 = Cipher.getInstance(Algorithm);
			c1.init(Cipher.ENCRYPT_MODE, deskey);
			return c1.doFinal(src);//在单一方面的加密或解密
		}
		catch (java.security.NoSuchAlgorithmException e1)
		{
			log.error("DES加密异常", e1);
		}
		catch (javax.crypto.NoSuchPaddingException e2)
		{
			log.error("DES加密异常", e2);
		}
		catch (Exception e3)
		{
			log.error("DES加密异常", e3);
		}
		return null;
	}

	//keybyte为加密密钥，长度为24字节
	//src为加密后的缓冲区
	public static byte[] decryptMode(byte[] keybyte, byte[] src)
	{
		try
		{
			//生成密钥
			SecretKey deskey = new SecretKeySpec(keybyte, Algorithm);
			//解密
			Cipher c1 = Cipher.getInstance(Algorithm + "/ECB/PKCS5Padding");
			c1.init(Cipher.DECRYPT_MODE, deskey);
			return c1.doFinal(src);
		}
		catch (java.security.NoSuchAlgorithmException e1)
		{
			e1.printStackTrace();
			log.error("decryptMode：", e1);
		}
		catch (javax.crypto.NoSuchPaddingException e2)
		{
			e2.printStackTrace();
			log.error("decryptMode：", e2);
		}
		catch (Exception e3)
		{
			e3.printStackTrace();
			log.error("decryptMode：", e3);
		}
		return null;
	}

	/*
	 *生成密钥 encodeKeyA.length=24
	 */
	public static byte[] genCroptyKey(byte[] encodeKeyA, String randomStrB)
	{
		if (encodeKeyA == null)
		{
			return null;
		}
		byte[] A = encodeKeyA;
		byte[] B = new byte[24];
		byte[] C = randomStrB.getBytes();
		int alen = A.length;
		int clen = C.length;
		if (alen != 24 || (clen < 8 || clen > 20))
			return null;
		int demension = alen - clen;
		for (int i = 0; i < C.length; i++)
		{
			B[i] = C[i];
		}

		int piont = 1;
		while (demension > 0)
		{
			if (demension > clen)
			{
				for (int i = 0; i < clen; i++)
				{
					B[clen * piont + i] = C[i];
				}
				piont++;

			}
			else
			{
				for (int i = 0; i < demension; i++)
				{
					B[clen * piont + i] = C[i];
				}
			}
			demension = demension - clen;

		}
		byte[] result = new byte[24];
		for (int i = 0; i < alen; i++)
		{ //0 ^  1 |  2 &

			switch ((i + 1) % 3)
			{
				case 0:
					result[i] = (byte) (A[i] ^ B[i]);
					break;
				case 1:
					result[i] = (byte) (A[i] & B[i]);
					break;
				case 2:
					result[i] = (byte) (A[i] | B[i]);
					break;
			}

		}

		System.out.println("key=" + Arrays.toString(result));

		return result;
	}

	public static String getBASE64(byte[] b)
	{
		String s = null;
		if (b != null)
		{
			//			s = new sun.misc.BASE64Encoder().encode(b);
			s = Base64.encodeS(b);
		}
		return s;
	}

	public static byte[] getFromBASE64(String s)
	{
		byte[] b = null;
		if (s != null)
		{
			try
			{
				//				BASE64Decoder decoder = new BASE64Decoder();
				//				b = decoder.decodeBuffer(s);
				b = Base64.decode(s);
				return b;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return b;
	}

	/**
	 * 传输加密参数，组合
	 * @param oraStr
	 * @param key
	 * @return
	 */
	public static String restructParam(String oraStr)
	{

		byte[] keys = EncryptUtil.ecodeStr;
		if (StringUtils.isNotBlank(oraStr))
		{

			String stampStr = EncryptUtil.RndString(10, null);
			byte[] encodeKeys = EncryptUtil.genCroptyKey(keys, stampStr);
			byte[] result = EncryptUtil.encryptMode(encodeKeys, oraStr.getBytes());
			String base64Str = EncryptUtil.getBASE64(result);
			try
			{
				String data = URLEncoder.encode(base64Str, "utf-8");
				String lastStr = data + "&stamp=" + URLEncoder.encode(stampStr, "utf-8");
				return lastStr;
			}
			catch (UnsupportedEncodingException e)
			{
				System.out.println("加密编码错误：" + oraStr);
				log.error("加密编码错误：", e);
			}
		}
		return null;
	}

	/**
	 * 生成随机字符串
	 */
	public static String RndString(int Length, int[] Seed)
	{
		String strSep = ",";
		// char[] chrSep = strSep.ToCharArray();

		//这里定义字符集
		String strChar = "0,1,2,3,4,5,6,7,8,9,a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z"
				+ ",A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,W,X,Y,Z";

		String[] aryChar = strChar.split(strSep, strChar.length());

		String strRandom = "";
		Random Rnd;
		if (Seed != null && Seed.length > 0)
		{
			Rnd = new Random(Seed[0]);
		}
		else
		{
			Rnd = new Random();
		}

		//生成随机字符串
		for (int i = 0; i < Length; i++)
		{
			strRandom += aryChar[Rnd.nextInt(aryChar.length)];
		}

		return strRandom;
	}

	public static byte[] rc4Encrypt(byte[] data, String key)
	{
		return RC4.encry_RC4_byte(data, key);
	}

	public static byte[] rc4Decrypt(byte[] data, String key)
	{
		return RC4.decry_RC4_byte(data, key);
	}

}
