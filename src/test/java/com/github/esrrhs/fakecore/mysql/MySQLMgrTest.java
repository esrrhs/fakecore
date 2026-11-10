package com.github.esrrhs.fakecore.mysql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MySQLMgrTest
{
	@AfterEach
	void cleanup()
	{
		MySQLMgr.shutdown();
	}

	@Test
	void initRejectsNullOrEmptyUrl()
	{
		assertThrows(IllegalArgumentException.class, () -> MySQLMgr.init(1, null));
		MySQLInfo info = new MySQLInfo();
		assertThrows(IllegalArgumentException.class, () -> MySQLMgr.init(1, info));
		info.setUrl("   ");
		assertThrows(IllegalArgumentException.class, () -> MySQLMgr.init(1, info));
	}

	@Test
	void initRejectsNonPositivePoolSize()
	{
		MySQLInfo info = new MySQLInfo();
		info.setUrl("jdbc:mysql://127.0.0.1:3306/x");
		assertThrows(IllegalArgumentException.class, () -> MySQLMgr.init(0, info));
		assertThrows(IllegalArgumentException.class, () -> MySQLMgr.init(-1, info));
	}

	@Test
	void uninitializedAccessThrows()
	{
		assertFalse(MySQLMgr.isInitialized());
		assertNull(MySQLMgr.getMySQLInfo());
		assertThrows(IllegalStateException.class, MySQLMgr::getConn);
		assertThrows(IllegalStateException.class, () -> MySQLMgr.getConnection());
		assertTrue(MySQLMgr.getFree().isEmpty());
	}

	@Test
	void shutdownWhenNotInitializedIsNoOp()
	{
		MySQLMgr.shutdown();
		assertFalse(MySQLMgr.isInitialized());
	}

	@Test
	void initFailsWithoutLiveDatabase()
	{
		MySQLInfo info = new MySQLInfo();
		info.setUrl("jdbc:mysql://127.0.0.1:1/nonexistent");
		info.setUser("nobody");
		info.setPasswd("nobody");
		info.setConnectionTimeoutMs(500);
		assertThrows(RuntimeException.class, () -> MySQLMgr.init(1, info));
		assertFalse(MySQLMgr.isInitialized());
	}

	@Test
	void initTimesOutWhenHandshakeNeverCompletes() throws Exception
	{
		try (ServerSocket silent = new ServerSocket())
		{
			silent.bind(new InetSocketAddress("127.0.0.1", 0), 50);
			int port = silent.getLocalPort();
			MySQLInfo info = new MySQLInfo();
			info.setUrl("jdbc:mysql://127.0.0.1:" + port
					+ "/x?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=800");
			info.setUser("nobody");
			info.setPasswd("nobody");
			info.setConnectionTimeoutMs(800);
			long t0 = System.currentTimeMillis();
			assertThrows(RuntimeException.class, () -> MySQLMgr.init(1, info));
			long elapsed = System.currentTimeMillis() - t0;
			assertFalse(MySQLMgr.isInitialized());
			assertTrue(elapsed >= 400, "expected a timeout rather than immediate refusal, elapsed=" + elapsed);
			assertTrue(elapsed < 15000, "timeout should stay bounded, elapsed=" + elapsed);
		}
	}
}
