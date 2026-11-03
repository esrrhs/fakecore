package com.github.esrrhs.fakecore.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.esrrhs.fakecore.args.ArgsMgr;

class RedisMgrMoreTest
{
	@TempDir
	Path tempDir;

	@AfterEach
	void cleanup()
	{
		System.clearProperty(RedisMgr.CONFIG_PATH_PROPERTY);
		RedisMgr.shutdown();
		ArgsMgr.parse(new String[0]);
	}

	@Test
	void resolveFallsBackToDefaultWithoutOverrides()
	{
		System.clearProperty(RedisMgr.CONFIG_PATH_PROPERTY);
		ArgsMgr.parse(new String[0]);
		assertEquals(RedisMgr.DEFAULT_CONFIG_PATH, RedisMgr.resolveConfigPath());
	}

	@Test
	void resolveUsesArgsMgrRedisConfig()
	{
		System.clearProperty(RedisMgr.CONFIG_PATH_PROPERTY);
		ArgsMgr.parse(new String[] { "{\"redisConfig\":\"/tmp/from-args-redis.json\"}" });
		assertEquals("/tmp/from-args-redis.json", RedisMgr.resolveConfigPath());
	}

	@Test
	void initRejectsEmptyPathAndMissingFields() throws Exception
	{
		assertThrows(IllegalArgumentException.class, () -> RedisMgr.init("  "));
		Path emptyIp = tempDir.resolve("bad-ip.json");
		Files.write(emptyIp, "{\"ip\":\"\",\"port\":6379}".getBytes(StandardCharsets.UTF_8));
		assertThrows(RuntimeException.class, () -> RedisMgr.init(emptyIp.toString()));

		Path noPort = tempDir.resolve("no-port.json");
		Files.write(noPort, "{\"ip\":\"127.0.0.1\"}".getBytes(StandardCharsets.UTF_8));
		assertThrows(RuntimeException.class, () -> RedisMgr.init(noPort.toString()));
	}

	@Test
	void initWithOptionalFieldsAndPasswordCreatesPool() throws Exception
	{
		Path cfg = tempDir.resolve("redis-full.json");
		Files.write(cfg, ("{\"ip\":\"127.0.0.1\",\"port\":6379,\"passwd\":\"secret\","
				+ "\"timeout\":100,\"maxTotal\":4,\"maxIdle\":2,\"minIdle\":0,\"database\":1}")
						.getBytes(StandardCharsets.UTF_8));
		RedisMgr.init(cfg.toString());
		assertTrue(RedisMgr.isInitialized());
		RedisMgr.init(cfg.toString()); // second init skipped
		assertThrows(Exception.class, RedisMgr::getJedis); // no live redis; borrow may fail
		RedisMgr.shutdown();
		assertFalse(RedisMgr.isInitialized());
		assertThrows(IllegalStateException.class, RedisMgr::getJedis);
	}

	@Test
	void initWithPasswordKeyVariant() throws Exception
	{
		Path cfg = tempDir.resolve("redis-password.json");
		Files.write(cfg, "{\"ip\":\"127.0.0.1\",\"port\":6379,\"password\":\"pw\"}"
				.getBytes(StandardCharsets.UTF_8));
		RedisMgr.init(cfg.toString());
		assertTrue(RedisMgr.isInitialized());
	}

	@Test
	void initViaResolveConfigPathProperty() throws Exception
	{
		Path cfg = tempDir.resolve("redis-prop.json");
		Files.write(cfg, "{\"ip\":\"127.0.0.1\",\"port\":6379}".getBytes(StandardCharsets.UTF_8));
		System.setProperty(RedisMgr.CONFIG_PATH_PROPERTY, cfg.toAbsolutePath().toString());
		RedisMgr.init();
		assertTrue(RedisMgr.isInitialized());
	}
}
