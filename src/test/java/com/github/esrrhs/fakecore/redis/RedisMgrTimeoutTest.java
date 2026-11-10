package com.github.esrrhs.fakecore.redis;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RedisMgrTimeoutTest
{
	@TempDir
	Path tempDir;

	@AfterEach
	void cleanup()
	{
		RedisMgr.shutdown();
	}

	@Test
	void getJedisTimesOutWhenPeerNeverSpeaksRedis() throws Exception
	{
		try (ServerSocket silent = new ServerSocket())
		{
			silent.bind(new InetSocketAddress("127.0.0.1", 0));
			int port = silent.getLocalPort();
			Path cfg = tempDir.resolve("silent-redis.json");
			Files.write(cfg, ("{\"ip\":\"127.0.0.1\",\"port\":" + port + ",\"timeout\":300,\"maxTotal\":1}")
					.getBytes(StandardCharsets.UTF_8));
			RedisMgr.init(cfg.toString());
			long t0 = System.currentTimeMillis();
			assertThrows(Exception.class, RedisMgr::getJedis);
			long elapsed = System.currentTimeMillis() - t0;
			assertTrue(elapsed >= 200, "expected a timeout, not an immediate failure, elapsed=" + elapsed);
			assertTrue(elapsed < 8000, "timeout should stay bounded, elapsed=" + elapsed);
		}
	}
}
