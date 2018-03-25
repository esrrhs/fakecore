package com.github.esrrhs.fakecore.mysql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by esrrhs on 2018/2/19.
 */
public class MySQLMgr
{
	private static Logger log = LoggerFactory.getLogger(MySQLMgr.class);

	private static ConcurrentLinkedQueue<MySQLConn> free = new ConcurrentLinkedQueue<>();

	public static void init(int poolSize, MySQLInfo mySQLInfo)
	{
		for (int i = 0; i < poolSize; i++)
		{
			MySQLConn mySQLConn = new MySQLConn();
			if (!mySQLConn.conn(mySQLInfo))
			{
				throw new RuntimeException("conn fail");
			}
			free.add(mySQLConn);
		}
	}

	public static MySQLConn getConn()
	{
		return free.poll();
	}

	public static ConcurrentLinkedQueue<MySQLConn> getFree()
	{
		return free;
	}
}
