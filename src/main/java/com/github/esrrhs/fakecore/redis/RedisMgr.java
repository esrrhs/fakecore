package com.github.esrrhs.fakecore.redis;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.esrrhs.fakecore.args.ArgsMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by esrrhs on 2018/2/9.
 */
public class RedisMgr
{
	private static Logger log = LoggerFactory.getLogger(RedisMgr.class);

	public static final String DEFAULT_CONFIG_PATH = "./bin/config/redis.json";
	public static final String CONFIG_PATH_PROPERTY = "fakecore.redis.config";
	public static final String CONFIG_PATH_ENV = "FAKECORE_REDIS_CONFIG";

	private static volatile JedisPool pool = null;

	public static void init()
	{
		init(resolveConfigPath());
	}

	/**
	 * Initialize using an explicit config file path.
	 */
	public static synchronized void init(String configPath)
	{
		try
		{
			if (pool != null)
			{
				log.info("RedisMgr already initialized, skip");
				return;
			}

			if (configPath == null || configPath.trim().isEmpty())
			{
				throw new IllegalArgumentException(
						"Redis config path is empty. Set system property '" + CONFIG_PATH_PROPERTY
								+ "', env '" + CONFIG_PATH_ENV + "', ArgsMgr key 'redisConfig', or pass a path.");
			}

			Path path = Paths.get(configPath);
			if (!Files.isRegularFile(path))
			{
				throw new IllegalStateException("Redis config file not found: " + path.toAbsolutePath()
						+ " (default remains " + DEFAULT_CONFIG_PATH + ")");
			}

			byte[] data = Files.readAllBytes(path);
			JSONObject jsonObject = JSON.parseObject(new String(data, StandardCharsets.UTF_8));
			if (jsonObject == null)
			{
				throw new IllegalStateException("Redis config is empty or invalid JSON: " + path.toAbsolutePath());
			}

			String ip = jsonObject.getString("ip");
			Integer port = jsonObject.getInteger("port");
			if (ip == null || ip.trim().isEmpty() || port == null)
			{
				throw new IllegalStateException(
						"Redis config must include non-empty 'ip' and 'port': " + path.toAbsolutePath());
			}

			String password = firstNonBlank(jsonObject.getString("password"), jsonObject.getString("passwd"));
			int timeoutMs = jsonObject.containsKey("timeout") ? jsonObject.getIntValue("timeout") : 2000;
			int maxTotal = jsonObject.containsKey("maxTotal") ? jsonObject.getIntValue("maxTotal") : 16;
			int maxIdle = jsonObject.containsKey("maxIdle") ? jsonObject.getIntValue("maxIdle") : 8;
			int minIdle = jsonObject.containsKey("minIdle") ? jsonObject.getIntValue("minIdle") : 0;
			int database = jsonObject.containsKey("database") ? jsonObject.getIntValue("database") : 0;

			JedisPoolConfig config = new JedisPoolConfig();
			config.setMaxTotal(maxTotal);
			config.setMaxIdle(maxIdle);
			config.setMinIdle(minIdle);
			config.setTestOnBorrow(true);

			if (password != null && !password.isEmpty())
			{
				pool = new JedisPool(config, ip, port, timeoutMs, password, database);
			}
			else
			{
				pool = new JedisPool(config, ip, port, timeoutMs, null, database);
			}

			log.info("RedisMgr init ok path={} {}:{} db={} maxTotal={}", path.toAbsolutePath(), ip, port, database,
					maxTotal);
		}
		catch (RuntimeException e)
		{
			log.error("RedisMgr init fail", e);
			throw e;
		}
		catch (Exception e)
		{
			log.error("RedisMgr init fail", e);
			throw new RuntimeException("RedisMgr init fail: " + e.getMessage(), e);
		}
	}

	/**
	 * Resolve config path: system property, env, ArgsMgr "redisConfig", then default.
	 */
	public static String resolveConfigPath()
	{
		String fromProp = System.getProperty(CONFIG_PATH_PROPERTY);
		if (fromProp != null && !fromProp.trim().isEmpty())
		{
			return fromProp.trim();
		}
		String fromEnv = System.getenv(CONFIG_PATH_ENV);
		if (fromEnv != null && !fromEnv.trim().isEmpty())
		{
			return fromEnv.trim();
		}
		try
		{
			if (ArgsMgr.getJsonObject() != null && ArgsMgr.getJsonObject().containsKey("redisConfig"))
			{
				String fromArgs = ArgsMgr.getJsonObject().getString("redisConfig");
				if (fromArgs != null && !fromArgs.trim().isEmpty())
				{
					return fromArgs.trim();
				}
			}
		}
		catch (Exception ignored)
		{
		}
		return DEFAULT_CONFIG_PATH;
	}

	public static Jedis getJedis()
	{
		if (pool == null)
		{
			throw new IllegalStateException("RedisMgr is not initialized. Call RedisMgr.init() first.");
		}
		return pool.getResource();
	}

	public static boolean isInitialized()
	{
		return pool != null;
	}

	/**
	 * Close the pool. Mainly for tests / graceful shutdown.
	 */
	public static synchronized void shutdown()
	{
		if (pool != null)
		{
			pool.close();
			pool = null;
			log.info("RedisMgr shutdown");
		}
	}

	private static String firstNonBlank(String a, String b)
	{
		if (a != null && !a.trim().isEmpty())
		{
			return a;
		}
		if (b != null && !b.trim().isEmpty())
		{
			return b;
		}
		return null;
	}
}
