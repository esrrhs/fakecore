package com.github.esrrhs.fakecore.mysql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.*;
import java.util.ArrayList;

/**
 * Created by esrrhs on 2018/2/7.
 */
public class MySQLConn implements Closeable
{
	private static Logger log = LoggerFactory.getLogger(MySQLConn.class);

	private MySQLInfo mySQLInfo;
	private Connection conn;
	private Statement stmt;
	private ArrayList<ResultSet> resultSets = new ArrayList<>();

	public MySQLConn()
	{
	}

	public Connection getConn()
	{
		return conn;
	}

	public boolean conn(MySQLInfo mySQLInfo)
	{
		this.mySQLInfo = mySQLInfo;
		try
		{
			Class.forName("com.mysql.cj.jdbc.Driver");

			log.info("start connect db...");
			conn = DriverManager.getConnection(mySQLInfo.getUrl(), mySQLInfo.getUser(), mySQLInfo.getPasswd());

			stmt = conn.createStatement();

			log.info("connect db ok");

			return true;
		}
		catch (Exception e)
		{
			log.error("MySQLMgr init fail {} {} {} {}", mySQLInfo.getUrl(), mySQLInfo.getUser(), mySQLInfo.getPasswd(),
					e);
			return true;
		}
	}

	public MySQLInfo getMySQLInfo()
	{
		return mySQLInfo;
	}

	@Override
	public void close()
	{
		try
		{
			if (!resultSets.isEmpty())
			{
				for (ResultSet resultSet : resultSets)
				{
					resultSet.close();
				}
				resultSets.clear();
			}
			MySQLMgr.getFree().add(this);
		}
		catch (SQLException e)
		{
			log.error("close ", e);
		}
	}

	public int executeUpdate(String sql)
	{
		int ret = 0;
		try
		{
			ret = stmt.executeUpdate(sql);
		}
		catch (Exception e)
		{
			log.error("executeUpdate ", e);
		}
		return ret;
	}

	public ResultSet executeQuery(String sql)
	{
		try
		{
			ResultSet resultSet = stmt.executeQuery(sql);
			resultSets.add(resultSet);
			return resultSet;
		}
		catch (Exception e)
		{
			log.error("executeQuery ", e);
		}
		return null;
	}
}
