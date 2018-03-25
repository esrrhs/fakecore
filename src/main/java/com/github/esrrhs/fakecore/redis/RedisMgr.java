package com.github.esrrhs.fakecore.redis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by esrrhs on 2018/2/9.
 */
public class RedisMgr
{
	private static Logger log = LoggerFactory.getLogger(RedisMgr.class);

	private static JedisPool pool = null;

	public static void init()
	{
		try
		{
			if (pool == null)
			{
				byte[] data = Files.readAllBytes(Paths.get("./bin/config/redis.json"));

				JSONObject jsonObject = JSON.parseObject(new String(data));

				String ip = jsonObject.getString("ip");
				int prot = jsonObject.getInteger("port");

				JedisPoolConfig config = new JedisPoolConfig();
				pool = new JedisPool(config, ip, prot);
			}
			log.info("RedisMgr init ok");
		}
		catch (Exception e)
		{
			log.info("RedisMgr init fail", e);
			throw new RuntimeException("RedisMgr init fail");
		}
	}

	public static Jedis getJedis()
	{
		return pool.getResource();
	}
}
