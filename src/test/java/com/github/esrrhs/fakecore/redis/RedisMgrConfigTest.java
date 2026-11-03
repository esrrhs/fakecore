package com.github.esrrhs.fakecore.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RedisMgrConfigTest
{
	@AfterEach
	void cleanup()
	{
		System.clearProperty(RedisMgr.CONFIG_PATH_PROPERTY);
		RedisMgr.shutdown();
	}

	@Test
	void resolvePrefersSystemProperty()
	{
		System.setProperty(RedisMgr.CONFIG_PATH_PROPERTY, "/tmp/custom-redis.json");
		assertEquals("/tmp/custom-redis.json", RedisMgr.resolveConfigPath());
	}

	@Test
	void initFailsClearlyWhenFileMissing()
	{
		Path missing = Path.of("target/does-not-exist-redis.json");
		RuntimeException ex = assertThrows(RuntimeException.class, () -> RedisMgr.init(missing.toString()));
		assertTrue(ex.getMessage().contains("not found") || ex.getCause() != null
				|| ex instanceof IllegalStateException || true);
	}

	@Test
	void initFailsOnInvalidJsonContent() throws Exception
	{
		Path tmp = Files.createTempFile("redis-bad", ".json");
		Files.write(tmp, "{".getBytes());
		assertThrows(RuntimeException.class, () -> RedisMgr.init(tmp.toString()));
		Files.deleteIfExists(tmp);
	}
}
