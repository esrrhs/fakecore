package com.github.esrrhs.fakecore.config;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by esrrhs on 2018/2/4.
 */
public class ConfigMgr
{
	private static ConcurrentHashMap<String, IConfig> configConcurrentHashMap = new ConcurrentHashMap<String, IConfig>();

	public static void add(String name, IConfig config)
	{
		configConcurrentHashMap.put(name, config);
	}

	public static void load()
	{
		for (IConfig iConfig : configConcurrentHashMap.values())
		{
			iConfig.load();
		}
	}
}
