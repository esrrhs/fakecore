package com.github.esrrhs.fakecore.mysql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Created by esrrhs on 2018/2/7.
 * <p>
 * Lightweight wrapper around a pooled {@link Connection}. {@link #close()} returns
 * the connection to HikariCP (does not keep a custom free list).
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

	/**
	 * Direct DriverManager connect (legacy). Prefer {@link MySQLMgr#getConn()}.
	 */
	public boolean conn(MySQLInfo mySQLInfo)
	{
		this.mySQLInfo = mySQLInfo;
		try
		{
			Class.forName("com.mysql.cj.jdbc.Driver");

			log.info("start connect db...");
			conn = java.sql.DriverManager.getConnection(mySQLInfo.getUrl(), mySQLInfo.getUser(),
					mySQLInfo.getPasswd());

			stmt = conn.createStatement();

			log.info("connect db ok");

			return true;
		}
		catch (Exception e)
		{
			log.error("MySQLConn.conn fail {} {} {}", mySQLInfo.getUrl(), mySQLInfo.getUser(), e.toString());
			return false;
		}
	}

	/**
	 * Bind an already-borrowed pooled connection.
	 */
	void borrow(Connection connection, MySQLInfo info) throws SQLException
	{
		this.mySQLInfo = info;
		this.conn = connection;
		this.stmt = connection.createStatement();
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
					try
					{
						resultSet.close();
					}
					catch (SQLException e)
					{
						log.error("close resultSet", e);
					}
				}
				resultSets.clear();
			}
			if (stmt != null)
			{
				try
				{
					stmt.close();
				}
				catch (SQLException e)
				{
					log.error("close stmt", e);
				}
				stmt = null;
			}
			if (conn != null)
			{
				try
				{
					conn.close(); // returns to Hikari pool when pooled
				}
				catch (SQLException e)
				{
					log.error("close conn", e);
				}
				conn = null;
			}
		}
		catch (Exception e)
		{
			log.error("close ", e);
		}
	}

	public int executeUpdate(String sql)
	{
		int ret = 0;
		try
		{
			log.info("executeUpdate {}", sql);
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
			log.info("executeQuery {}", sql);
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
