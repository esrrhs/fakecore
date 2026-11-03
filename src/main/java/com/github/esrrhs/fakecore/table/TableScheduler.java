package com.github.esrrhs.fakecore.table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared striped executors for table message loops. Each tableId maps to a fixed
 * single-thread executor so ordering per table is preserved without one OS thread
 * per live table.
 */
public final class TableScheduler
{
	private static final Logger log = LoggerFactory.getLogger(TableScheduler.class);

	private static final int STRIPES = Math.max(4,
			Integer.getInteger("fakecore.table.stripes", Runtime.getRuntime().availableProcessors() * 2));

	private static final ExecutorService[] STRIPES_EXEC = new ExecutorService[STRIPES];

	static
	{
		for (int i = 0; i < STRIPES; i++)
		{
			final int stripe = i;
			STRIPES_EXEC[i] = Executors.newSingleThreadExecutor(new ThreadFactory() {
				private final AtomicInteger n = new AtomicInteger();

				@Override
				public Thread newThread(Runnable r)
				{
					Thread t = new Thread(r, "fakecore-table-stripe-" + stripe + "-" + n.incrementAndGet());
					t.setDaemon(true);
					return t;
				}
			});
		}
		log.info("TableScheduler stripes={}", STRIPES);
	}

	private TableScheduler()
	{
	}

	public static void submit(int tableId, Runnable task)
	{
		int index = Math.floorMod(tableId, STRIPES);
		STRIPES_EXEC[index].execute(task);
	}

	public static int stripeCount()
	{
		return STRIPES;
	}

	/**
	 * Test / process shutdown hook. Not required for normal library use.
	 */
	public static void shutdown()
	{
		for (ExecutorService exec : STRIPES_EXEC)
		{
			exec.shutdownNow();
		}
	}
}
