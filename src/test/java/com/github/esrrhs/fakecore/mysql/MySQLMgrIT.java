package com.github.esrrhs.fakecore.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.esrrhs.fakecore.support.TemporaryDataServices;

class MySQLMgrIT
{
	private static TemporaryDataServices services;

	@BeforeAll
	static void startServices()
	{
		services = TemporaryDataServices.acquire();
	}

	@AfterAll
	static void stopServices()
	{
		TemporaryDataServices.release();
	}

	@AfterEach
	void shutdownPool()
	{
		MySQLMgr.shutdown();
	}

	@Test
	void initQueryUpdateAndRelease() throws Exception
	{
		MySQLMgr.init(4, info());
		assertTrue(MySQLMgr.isInitialized());
		assertEquals(services.mysqlJdbcUrl(), MySQLMgr.getMySQLInfo().getUrl());

		try (Connection c = MySQLMgr.getConnection(); Statement s = c.createStatement())
		{
			s.execute("CREATE TABLE IF NOT EXISTS it_kv (k VARCHAR(64) PRIMARY KEY, v VARCHAR(64) NOT NULL)");
			s.executeUpdate("DELETE FROM it_kv");
			assertEquals(1, s.executeUpdate("INSERT INTO it_kv(k,v) VALUES('a','1')"));
			try (ResultSet rs = s.executeQuery("SELECT v FROM it_kv WHERE k='a'"))
			{
				assertTrue(rs.next());
				assertEquals("1", rs.getString(1));
			}
		}

		try (MySQLConn conn = MySQLMgr.getConn())
		{
			assertNotNull(conn.getConn());
			assertEquals(1, conn.executeUpdate("UPDATE it_kv SET v='2' WHERE k='a'"));
			ResultSet rs = conn.executeQuery("SELECT v FROM it_kv WHERE k='a'");
			assertNotNull(rs);
			assertTrue(rs.next());
			assertEquals("2", rs.getString(1));
		}

		MySQLMgr.init(4, info()); // already initialized, skipped
		assertTrue(MySQLMgr.isInitialized());
		MySQLMgr.shutdown();
		assertFalse(MySQLMgr.isInitialized());
		assertThrows(IllegalStateException.class, MySQLMgr::getConn);
		assertThrows(IllegalStateException.class, MySQLMgr::getConnection);

		MySQLMgr.init(2, info());
		try (Connection c = MySQLMgr.getConnection(); Statement s = c.createStatement();
				ResultSet rs = s.executeQuery("SELECT v FROM it_kv WHERE k='a'"))
		{
			assertTrue(rs.next());
			assertEquals("2", rs.getString(1));
		}
	}

	@Test
	void concurrentBorrowersReturnConnectionsToPool() throws Exception
	{
		MySQLMgr.init(4, info());
		try (Connection setup = MySQLMgr.getConnection(); Statement s = setup.createStatement())
		{
			s.execute("CREATE TABLE IF NOT EXISTS it_counter (id INT PRIMARY KEY, n INT NOT NULL)");
			s.executeUpdate("INSERT INTO it_counter(id,n) VALUES(1,0) ON DUPLICATE KEY UPDATE n=0");
		}

		int threads = 8;
		int increments = 20;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);
		AtomicInteger errors = new AtomicInteger();
		for (int i = 0; i < threads; i++)
		{
			pool.submit(() -> {
				try
				{
					start.await(5, TimeUnit.SECONDS);
					for (int j = 0; j < increments; j++)
					{
						try (Connection c = MySQLMgr.getConnection(); Statement s = c.createStatement())
						{
							s.executeUpdate("UPDATE it_counter SET n = n + 1 WHERE id=1");
						}
					}
				}
				catch (Exception e)
				{
					errors.incrementAndGet();
				}
				finally
				{
					done.countDown();
				}
			});
		}
		start.countDown();
		assertTrue(done.await(30, TimeUnit.SECONDS));
		pool.shutdownNow();
		assertEquals(0, errors.get());

		try (Connection c = MySQLMgr.getConnection(); Statement s = c.createStatement();
				ResultSet rs = s.executeQuery("SELECT n FROM it_counter WHERE id=1"))
		{
			assertTrue(rs.next());
			assertEquals(threads * increments, rs.getInt(1));
		}
	}

	@Test
	void wrapperCloseReturnsConnectionForReuse() throws Exception
	{
		MySQLMgr.init(1, info());
		List<Integer> identities = new ArrayList<>();
		for (int i = 0; i < 3; i++)
		{
			try (MySQLConn conn = MySQLMgr.getConn())
			{
				identities.add(System.identityHashCode(conn.getConn()));
				ResultSet rs = conn.executeQuery("SELECT 1");
				assertNotNull(rs);
				assertTrue(rs.next());
			}
		}
		assertEquals(3, identities.size());
	}

	private static MySQLInfo info()
	{
		MySQLInfo info = new MySQLInfo();
		info.setUrl(services.mysqlJdbcUrl());
		info.setUser(services.mysqlUser());
		info.setPasswd(services.mysqlPassword());
		info.setConnectionTimeoutMs(5000);
		return info;
	}
}
