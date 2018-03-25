package com.github.esrrhs.fakecore.args;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * Created by esrrhs on 2018/2/9.
 */
public class ArgsMgr
{
	private static JSONObject jsonObject = new JSONObject();

	public static void parse(String[] args)
	{
		if (args.length > 0)
		{
			jsonObject = JSON.parseObject(args[0]);
		}
	}

	public static JSONObject getJsonObject()
	{
		return jsonObject;
	}
}
