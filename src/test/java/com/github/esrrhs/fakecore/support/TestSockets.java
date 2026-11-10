package com.github.esrrhs.fakecore.support;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Loopback helpers for tests. Ports are bound to 127.0.0.1 only.
 */
public final class TestSockets
{
	private TestSockets()
	{
	}

	public static int freePort()
	{
		try (ServerSocket ss = new ServerSocket())
		{
			ss.setReuseAddress(true);
			ss.bind(new InetSocketAddress("127.0.0.1", 0));
			int port = ss.getLocalPort();
			if (port == 3306 || port == 6379)
			{
				return freePort();
			}
			return port;
		}
		catch (IOException e)
		{
			throw new IllegalStateException("unable to allocate a local port", e);
		}
	}

	public static void await(BooleanSupplier condition, long timeoutMs, String message)
	{
		long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
		while (System.nanoTime() < deadline)
		{
			if (condition.getAsBoolean())
			{
				return;
			}
			try
			{
				Thread.sleep(20);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				fail(message + " (interrupted)");
			}
		}
		fail(message);
	}
}
