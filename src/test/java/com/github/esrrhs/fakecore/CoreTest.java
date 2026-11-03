package com.github.esrrhs.fakecore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.esrrhs.fakecore.redis.RedisMgr;

class CoreTest
{
	@TempDir
	Path tempDir;

	@AfterEach
	void cleanup()
	{
		System.clearProperty(RedisMgr.CONFIG_PATH_PROPERTY);
		RedisMgr.shutdown();
	}

	@Test
	void initParsesGsIdWithoutLiveRedis() throws Exception
	{
		Path redisCfg = tempDir.resolve("redis.json");
		Files.write(redisCfg, "{\"ip\":\"127.0.0.1\",\"port\":6379}".getBytes(StandardCharsets.UTF_8));
		System.setProperty(RedisMgr.CONFIG_PATH_PROPERTY, redisCfg.toAbsolutePath().toString());

		Core.init(new String[] { "{\"gsId\":123}" });
		assertEquals(123, Core.getGsId());
		assertTrue(RedisMgr.isInitialized());
	}
}
