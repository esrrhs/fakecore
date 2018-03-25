package com.github.esrrhs.fakecore;

import com.github.esrrhs.fakecore.args.ArgsMgr;
import com.github.esrrhs.fakecore.config.ConfigMgr;
import com.github.esrrhs.fakecore.redis.RedisMgr;

/**
 * Created by esrrhs on 2018/2/5.
 */
public class Core
{
	private static int gsId;

	public static int getGsId()
	{
		return gsId;
	}

	public static void init(String[] args)
	{
		RedisMgr.init();
		ArgsMgr.parse(args);
		ConfigMgr.load();
		gsId = ArgsMgr.getJsonObject().getIntValue("gsId");
	}
}
