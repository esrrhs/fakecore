package com.github.esrrhs.fakecore.mysql;

/**
 * Created by esrrhs on 2018/2/19.
 */
public class MySQLInfo
{
	private String url;
	private String user;
	private String passwd;
	/** Optional pool / JDBC extras (milliseconds). */
	private int connectionTimeoutMs = 30000;
	private int idleTimeoutMs = 600000;
	private int maxLifetimeMs = 1800000;

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public String getUser()
	{
		return user;
	}

	public void setUser(String user)
	{
		this.user = user;
	}

	public String getPasswd()
	{
		return passwd;
	}

	public void setPasswd(String passwd)
	{
		this.passwd = passwd;
	}

	public int getConnectionTimeoutMs()
	{
		return connectionTimeoutMs;
	}

	public void setConnectionTimeoutMs(int connectionTimeoutMs)
	{
		this.connectionTimeoutMs = connectionTimeoutMs;
	}

	public int getIdleTimeoutMs()
	{
		return idleTimeoutMs;
	}

	public void setIdleTimeoutMs(int idleTimeoutMs)
	{
		this.idleTimeoutMs = idleTimeoutMs;
	}

	public int getMaxLifetimeMs()
	{
		return maxLifetimeMs;
	}

	public void setMaxLifetimeMs(int maxLifetimeMs)
	{
		this.maxLifetimeMs = maxLifetimeMs;
	}
}
