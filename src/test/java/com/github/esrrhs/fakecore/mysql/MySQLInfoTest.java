package com.github.esrrhs.fakecore.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MySQLInfoTest
{
	@Test
	void defaultsAndSetters()
	{
		MySQLInfo info = new MySQLInfo();
		assertEquals(30000, info.getConnectionTimeoutMs());
		assertEquals(600000, info.getIdleTimeoutMs());
		assertEquals(1800000, info.getMaxLifetimeMs());

		info.setUrl("jdbc:mysql://localhost:3306/db");
		info.setUser("u");
		info.setPasswd("p");
		info.setConnectionTimeoutMs(1000);
		info.setIdleTimeoutMs(2000);
		info.setMaxLifetimeMs(3000);

		assertEquals("jdbc:mysql://localhost:3306/db", info.getUrl());
		assertEquals("u", info.getUser());
		assertEquals("p", info.getPasswd());
		assertEquals(1000, info.getConnectionTimeoutMs());
		assertEquals(2000, info.getIdleTimeoutMs());
		assertEquals(3000, info.getMaxLifetimeMs());
	}
}
