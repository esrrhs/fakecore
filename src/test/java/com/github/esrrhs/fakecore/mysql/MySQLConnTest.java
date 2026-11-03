package com.github.esrrhs.fakecore.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class MySQLConnTest
{
	static class FakeJdbc implements InvocationHandler
	{
		final AtomicBoolean connClosed = new AtomicBoolean();
		final AtomicBoolean stmtClosed = new AtomicBoolean();
		final List<AtomicBoolean> rsClosed = new ArrayList<>();
		final AtomicInteger updates = new AtomicInteger();
		volatile String lastUpdateSql;
		volatile String lastQuerySql;
		volatile boolean throwOnUpdate;
		volatile boolean throwOnQuery;
		volatile boolean throwOnCloseConn;
		volatile boolean throwOnCloseStmt;
		volatile boolean throwOnCloseRs;

		Connection connection()
		{
			return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
					new Class<?>[] { Connection.class }, this);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
		{
			String name = method.getName();
			Class<?> declaring = method.getDeclaringClass();
			if (declaring == Object.class)
			{
				if ("toString".equals(name))
				{
					return "fake";
				}
				if ("hashCode".equals(name))
				{
					return System.identityHashCode(proxy);
				}
				if ("equals".equals(name))
				{
					return proxy == args[0];
				}
			}
			if (proxy instanceof Connection || declaring == Connection.class
					|| (Connection.class.isInstance(proxy) && method.getDeclaringClass() == Connection.class))
			{
				// fall through based on return type / name using interface of proxy
			}
			if (Connection.class.isAssignableFrom(method.getDeclaringClass())
					|| (method.getDeclaringClass() == Connection.class))
			{
				// handled below via name
			}

			if ("createStatement".equals(name) && method.getReturnType() == Statement.class)
			{
				return statement();
			}
			if ("close".equals(name) && method.getDeclaringClass() == Connection.class)
			{
				if (throwOnCloseConn)
				{
					throw new SQLException("close conn");
				}
				connClosed.set(true);
				return null;
			}
			if ("close".equals(name) && method.getDeclaringClass() == Statement.class)
			{
				if (throwOnCloseStmt)
				{
					throw new SQLException("close stmt");
				}
				stmtClosed.set(true);
				return null;
			}
			if ("close".equals(name) && method.getDeclaringClass() == ResultSet.class)
			{
				if (throwOnCloseRs)
				{
					throw new SQLException("close rs");
				}
				for (AtomicBoolean b : rsClosed)
				{
					if (!b.get())
					{
						b.set(true);
						break;
					}
				}
				return null;
			}
			if ("executeUpdate".equals(name))
			{
				if (throwOnUpdate)
				{
					throw new SQLException("update fail");
				}
				lastUpdateSql = (String) args[0];
				return updates.incrementAndGet();
			}
			if ("executeQuery".equals(name))
			{
				if (throwOnQuery)
				{
					throw new SQLException("query fail");
				}
				lastQuerySql = (String) args[0];
				AtomicBoolean closed = new AtomicBoolean();
				rsClosed.add(closed);
				return resultSet(closed);
			}
			if ("isClosed".equals(name))
			{
				return false;
			}
			Class<?> rt = method.getReturnType();
			if (rt == boolean.class)
			{
				return false;
			}
			if (rt == int.class)
			{
				return 0;
			}
			if (rt == long.class)
			{
				return 0L;
			}
			if (rt == void.class)
			{
				return null;
			}
			return null;
		}

		Statement statement()
		{
			return (Statement) Proxy.newProxyInstance(Statement.class.getClassLoader(),
					new Class<?>[] { Statement.class }, this);
		}

		ResultSet resultSet(AtomicBoolean closedFlag)
		{
			return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(),
					new Class<?>[] { ResultSet.class }, (proxy, method, args) -> {
						String name = method.getName();
						if ("close".equals(name))
						{
							if (throwOnCloseRs)
							{
								throw new SQLException("close rs");
							}
							closedFlag.set(true);
							return null;
						}
						if ("toString".equals(name))
						{
							return "rs";
						}
						Class<?> rt = method.getReturnType();
						if (rt == boolean.class)
						{
							return false;
						}
						if (rt == int.class)
						{
							return 0;
						}
						if (rt == long.class)
						{
							return 0L;
						}
						return null;
					});
		}
	}

	@Test
	void borrowExecuteAndClose() throws Exception
	{
		FakeJdbc fake = new FakeJdbc();
		MySQLInfo info = new MySQLInfo();
		info.setUrl("jdbc:mysql://x");
		MySQLConn conn = new MySQLConn();
		conn.borrow(fake.connection(), info);

		assertSame(info, conn.getMySQLInfo());
		assertEquals(1, conn.executeUpdate("update t set a=1"));
		assertEquals("update t set a=1", fake.lastUpdateSql);
		assertTrue(conn.executeQuery("select 1") != null);
		assertEquals("select 1", fake.lastQuerySql);

		conn.close();
		assertTrue(fake.stmtClosed.get());
		assertTrue(fake.connClosed.get());
		assertTrue(fake.rsClosed.get(0).get());
		assertNull(conn.getConn());
	}

	@Test
	void executeFailuresReturnDefaults() throws Exception
	{
		FakeJdbc fake = new FakeJdbc();
		fake.throwOnUpdate = true;
		fake.throwOnQuery = true;
		MySQLConn conn = new MySQLConn();
		conn.borrow(fake.connection(), new MySQLInfo());
		assertEquals(0, conn.executeUpdate("x"));
		assertNull(conn.executeQuery("y"));
		conn.close();
	}

	@Test
	void closeToleratesSqlExceptions() throws Exception
	{
		FakeJdbc fake = new FakeJdbc();
		fake.throwOnCloseConn = true;
		fake.throwOnCloseStmt = true;
		fake.throwOnCloseRs = true;
		MySQLConn conn = new MySQLConn();
		conn.borrow(fake.connection(), new MySQLInfo());
		conn.executeQuery("q");
		conn.close(); // should not throw
	}

	@Test
	void directConnFailsWithoutLiveDb()
	{
		MySQLInfo info = new MySQLInfo();
		info.setUrl("jdbc:mysql://127.0.0.1:1/x");
		info.setUser("u");
		info.setPasswd("p");
		MySQLConn conn = new MySQLConn();
		assertFalse(conn.conn(info));
	}

	@Test
	void closeWhenEmptyIsSafe()
	{
		new MySQLConn().close();
	}
}
