package com.github.esrrhs.fakecore.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * MySQL access backed by HikariCP.
 * <p>
 * Prefer {@link #getConnection()} with try-with-resources. {@link #getConn()} still
 * returns a {@link MySQLConn} wrapper that returns the connection to the pool on
 * {@link MySQLConn#close()}.
 */
public class MySQLMgr
{
	private static Logger log = LoggerFactory.getLogger(MySQLMgr.class);

	private static volatile HikariDataSource dataSource;
	private static volatile MySQLInfo mySQLInfo;

	/**
	 * @deprecated retained only so legacy callers of {@link #getFree()} compile;
	 *             the queue is no longer used for pooling.
	 */
	@Deprecated
	private static final ConcurrentLinkedQueue<MySQLConn> free = new ConcurrentLinkedQueue<>();

	public static synchronized void init(int poolSize, MySQLInfo info)
	{
		if (info == null || info.getUrl() == null || info.getUrl().trim().isEmpty())
		{
			throw new IllegalArgumentException("MySQLInfo.url is required");
		}
		if (poolSize <= 0)
		{
			throw new IllegalArgumentException("poolSize must be > 0");
		}
		if (dataSource != null)
		{
			log.info("MySQLMgr already initialized, skip");
			return;
		}

		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(info.getUrl());
		config.setUsername(info.getUser());
		config.setPassword(info.getPasswd());
		config.setMaximumPoolSize(poolSize);
		config.setMinimumIdle(Math.min(1, poolSize));
		config.setPoolName("fakecore-mysql");
		config.setDriverClassName("com.mysql.cj.jdbc.Driver");
		config.setConnectionTimeout(info.getConnectionTimeoutMs());
		config.setIdleTimeout(info.getIdleTimeoutMs());
		config.setMaxLifetime(info.getMaxLifetimeMs());
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

		try
		{
			dataSource = new HikariDataSource(config);
			mySQLInfo = info;
			log.info("MySQLMgr init ok url={} poolSize={}", info.getUrl(), poolSize);
		}
		catch (Exception e)
		{
			log.error("MySQLMgr init fail", e);
			throw new RuntimeException("MySQLMgr init fail: " + e.getMessage(), e);
		}
	}

	/**
	 * Borrow a raw JDBC connection. Caller must close it (returns to Hikari pool).
	 */
	public static Connection getConnection() throws SQLException
	{
		ensureInitialized();
		return dataSource.getConnection();
	}

	/**
	 * Borrow a {@link MySQLConn} wrapper. Close it to return the connection to the pool.
	 */
	public static MySQLConn getConn()
	{
		ensureInitialized();
		try
		{
			MySQLConn wrapper = new MySQLConn();
			wrapper.borrow(dataSource.getConnection(), mySQLInfo);
			return wrapper;
		}
		catch (SQLException e)
		{
			log.error("getConn fail", e);
			throw new RuntimeException("getConn fail: " + e.getMessage(), e);
		}
	}

	/**
	 * @deprecated The hand-rolled queue is unused under HikariCP. Returns an empty queue.
	 */
	@Deprecated
	public static ConcurrentLinkedQueue<MySQLConn> getFree()
	{
		return free;
	}

	public static MySQLInfo getMySQLInfo()
	{
		return mySQLInfo;
	}

	public static boolean isInitialized()
	{
		return dataSource != null && !dataSource.isClosed();
	}

	public static synchronized void shutdown()
	{
		if (dataSource != null)
		{
			dataSource.close();
			dataSource = null;
			mySQLInfo = null;
			free.clear();
			log.info("MySQLMgr shutdown");
		}
	}

	private static void ensureInitialized()
	{
		if (dataSource == null || dataSource.isClosed())
		{
			throw new IllegalStateException("MySQLMgr is not initialized. Call MySQLMgr.init(...) first.");
		}
	}
}
