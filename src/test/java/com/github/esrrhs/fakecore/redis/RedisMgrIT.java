package com.github.esrrhs.fakecore.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.esrrhs.fakecore.support.TemporaryDataServices;

import redis.clients.jedis.Jedis;

class RedisMgrIT
{
	private static TemporaryDataServices services;

	@TempDir
	Path tempDir;

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
		System.clearProperty(RedisMgr.CONFIG_PATH_PROPERTY);
		RedisMgr.shutdown();
	}

	@Test
	void initSetGetAndRelease() throws Exception
	{
		RedisMgr.init(writeConfig(2000, 8).toString());
		assertTrue(RedisMgr.isInitialized());
		try (Jedis jedis = RedisMgr.getJedis())
		{
			jedis.set("fakecore:it:key", "ok");
			assertEquals("ok", jedis.get("fakecore:it:key"));
		}

		RedisMgr.init(writeConfig(2000, 8).toString()); // skip second init
		assertTrue(RedisMgr.isInitialized());
		RedisMgr.shutdown();
		assertFalse(RedisMgr.isInitialized());
		assertThrows(IllegalStateException.class, RedisMgr::getJedis);

		RedisMgr.init(writeConfig(2000, 8).toString());
		try (Jedis jedis = RedisMgr.getJedis())
		{
			assertEquals("ok", jedis.get("fakecore:it:key"));
		}
	}

	@Test
	void concurrentIncrAndReturnToPool() throws Exception
	{
		RedisMgr.init(writeConfig(2000, 8).toString());
		try (Jedis setup = RedisMgr.getJedis())
		{
			setup.del("fakecore:it:counter");
		}

		int threads = 8;
		int increments = 50;
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
						try (Jedis jedis = RedisMgr.getJedis())
						{
							jedis.incr("fakecore:it:counter");
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
		try (Jedis jedis = RedisMgr.getJedis())
		{
			assertEquals(threads * increments, Integer.parseInt(jedis.get("fakecore:it:counter")));
		}
	}

	private Path writeConfig(int timeoutMs, int maxTotal) throws Exception
	{
		Path cfg = tempDir.resolve("redis-it.json");
		String json = "{\"ip\":\"" + services.redisHost() + "\",\"port\":" + services.redisPort()
				+ ",\"timeout\":" + timeoutMs + ",\"maxTotal\":" + maxTotal + ",\"maxIdle\":4,\"minIdle\":0,\"database\":0}";
		Files.write(cfg, json.getBytes(StandardCharsets.UTF_8));
		return cfg;
	}
}
