package com.github.esrrhs.fakecore.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class NetUtil
{
	private static boolean containsIp(String ip)
	{
		Enumeration<NetworkInterface> netInterfaces;
		try
		{
			netInterfaces = NetworkInterface.getNetworkInterfaces();
			while (netInterfaces.hasMoreElements())
			{
				NetworkInterface ni = netInterfaces.nextElement();

				Enumeration<InetAddress> emu = ni.getInetAddresses();
				while (emu.hasMoreElements())
				{
					InetAddress ipaddr = emu.nextElement();
					if (ip.equals(ipaddr.getHostAddress()))
						return true;
				}
			}
		}
		catch (SocketException e)
		{
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 获取本地ip
	 * @return
	 */
	public static String getLocalIp()
	{
		String ip = null;
		try
		{
			ip = InetAddress.getLocalHost().getHostAddress();
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}
		return ip;
	}

}
