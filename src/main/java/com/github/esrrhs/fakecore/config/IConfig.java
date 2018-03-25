package com.github.esrrhs.fakecore.config;

import com.alibaba.fastjson.JSON;
import com.github.esrrhs.fakecore.file.FileMgr;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by esrrhs on 2018/2/4.
 */
public abstract class IConfig
{
	private static Logger log = LoggerFactory.getLogger(IConfig.class);

	protected abstract void load();

	protected <T> List<T> loadFromFile(String filePath, Class<T> clazz)
	{
		List<T> configList = null;
		try
		{
			String jsonStr = FileMgr.loadFromFile(filePath);
			if (!StringUtils.isEmpty(jsonStr))
			{
				configList = JSON.parseArray(jsonStr, clazz);
			}
		}
		catch (Exception e)
		{
			log.error("loadFromFile", e);
		}
		return configList;
	}

}
