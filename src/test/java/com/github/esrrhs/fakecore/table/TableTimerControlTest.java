package com.github.esrrhs.fakecore.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TableTimerControlTest
{
	static final int ROOM = 9202;
	static final int STATE_INIT = 1;
	static final int STATE_EXC = 99;

	static final AtomicInteger TABLE_TIMEOUTS = new AtomicInteger();
	static final AtomicInteger STATE_TIMEOUTS = new AtomicInteger();
	static volatile CountDownLatch LATCH;

	static class Plugin implements GamePlugin
	{
		@Override
		public void construct(Table gameTable)
		{
			gameTable.addState(STATE_INIT, new TableState(gameTable) {
				@Override
				protected void onEnter(Object param)
				{
					super.onEnter(param);
				}

				@Override
				protected void onTimeout(int type, Object param)
				{
					STATE_TIMEOUTS.incrementAndGet();
					CountDownLatch l = LATCH;
					if (l != null)
					{
						l.countDown();
					}
				}

				@Override
				protected void onTableTimeout(int type, Object param)
				{
					TABLE_TIMEOUTS.incrementAndGet();
					CountDownLatch l = LATCH;
					if (l != null)
					{
						l.countDown();
					}
				}
			});
			gameTable.addState(STATE_EXC, new TableState(gameTable));
			gameTable.setExceptionStateId(STATE_EXC);
		}

		@Override
		public boolean init(TablePlayerInfo creator, Object param)
		{
			return true;
		}
	}

	@AfterEach
	void cleanup()
	{
		for (Table t : TableMgr.getAllTablesList())
		{
			try
			{
				TableMgr.destroyTable(t);
			}
			catch (Exception ignored)
			{
			}
		}
		TableMgr.roomTables.clear();
		LATCH = null;
	}

	@Test
	void addGetCancelAndEndTimer()
	{
		TableTimer timer = TableTimer.instance();
		TableTimerEvent ev = timer.addTimer(777, 60_000, 3, "p");
		assertNotNull(ev);
		assertEquals(ev, timer.getTimer(ev.id));
		timer.endTimer(ev);
		assertEquals(0, ev.expire);
		timer.cancelTimer(ev.id);
		assertNull(timer.getTimer(ev.id));
	}

	@Test
	void tableLevelTimeoutFiresQuickly() throws Exception
	{
		TABLE_TIMEOUTS.set(0);
		STATE_TIMEOUTS.set(0);
		LATCH = new CountDownLatch(1);

		TableMgr.addPlugin(ROOM, Plugin.class);
		Table table = TableMgr.createTable(new TablePlayerInfo(), 7001, ROOM, null);
		assertNotNull(table);
		table.start(STATE_INIT);
		table.setTableTimeOut(40, 8, "tp");
		assertTrue(LATCH.await(2, TimeUnit.SECONDS));
		assertEquals(1, TABLE_TIMEOUTS.get());
	}

	@Test
	void cancelAllWaitTimersPreventsFire() throws Exception
	{
		TABLE_TIMEOUTS.set(0);
		LATCH = new CountDownLatch(1);
		TableMgr.addPlugin(ROOM + 1, Plugin.class);
		Table table = TableMgr.createTable(new TablePlayerInfo(), 7002, ROOM + 1, null);
		table.start(STATE_INIT);
		table.setTableTimeOut(80, 1, null);
		table.cancelTableAllWaitTimer();
		assertFalseLatchOrNoFire(LATCH, 200);
		assertEquals(0, TABLE_TIMEOUTS.get());
	}

	private static void assertFalseLatchOrNoFire(CountDownLatch latch, long waitMs) throws InterruptedException
	{
		boolean fired = latch.await(waitMs, TimeUnit.MILLISECONDS);
		org.junit.jupiter.api.Assertions.assertFalse(fired);
	}
}
