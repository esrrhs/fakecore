package com.github.esrrhs.fakecore.net;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class NetUtilTest
{
	@Test
	void getLocalIpReturnsNonEmpty()
	{
		String ip = NetUtil.getLocalIp();
		assertNotNull(ip);
		assertFalse(ip.trim().isEmpty());
	}
}
