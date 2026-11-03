package com.github.esrrhs.fakecore.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigMgrTest
{
	static class Sample
	{
		public int id;
		public String name;
	}

	static class CountingConfig extends IConfig
	{
		final AtomicInteger loads = new AtomicInteger();
		List<Sample> loaded;
		final String path;

		CountingConfig(String path)
		{
			this.path = path;
		}

		@Override
		protected void load()
		{
			loads.incrementAndGet();
			loaded = loadFromFile(path, Sample.class);
		}
	}

	@TempDir
	Path tempDir;

	@Test
	void addAndLoadInvokesConfigs() throws Exception
	{
		Path f = tempDir.resolve("cfg.json");
		Files.write(f, "[{\"id\":7,\"name\":\"x\"}]".getBytes(StandardCharsets.UTF_8));
		CountingConfig cfg = new CountingConfig(f.toString());
		ConfigMgr.add("unit-" + System.nanoTime(), cfg);
		ConfigMgr.load();
		assertEquals(1, cfg.loads.get());
		assertNotNull(cfg.loaded);
		assertEquals(1, cfg.loaded.size());
		assertEquals(7, cfg.loaded.get(0).id);
		assertEquals("x", cfg.loaded.get(0).name);
	}

	@Test
	void loadFromFileMissingReturnsNull()
	{
		CountingConfig cfg = new CountingConfig(tempDir.resolve("missing.json").toString());
		cfg.load();
		assertNull(cfg.loaded);
	}

	@Test
	void loadFromFileEmptyReturnsNullList() throws Exception
	{
		Path f = tempDir.resolve("empty.json");
		Files.write(f, new byte[0]);
		CountingConfig cfg = new CountingConfig(f.toString());
		cfg.load();
		// empty file yields empty string after FileMgr; StringUtils.isEmpty -> null list
		assertNull(cfg.loaded);
	}

	@Test
	void loadFromFileInvalidJsonCaught() throws Exception
	{
		Path f = tempDir.resolve("bad.json");
		Files.write(f, "not-json".getBytes(StandardCharsets.UTF_8));
		CountingConfig cfg = new CountingConfig(f.toString());
		cfg.load();
		assertNull(cfg.loaded);
	}
}
