package com.github.esrrhs.fakecore.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.github.esrrhs.fakecore.net.Message;

class TableStateMachineTest
{
	static final int STATE_INIT = 1;
	static final int STATE_PLAY = 2;
	static final int STATE_EXC = 99;

	static class DemoPlugin implements GamePlugin
	{
		final AtomicInteger enters = new AtomicInteger();
		final AtomicInteger leaves = new AtomicInteger();
		final AtomicInteger destroys = new AtomicInteger();
		Table table;

		@Override
		public void construct(Table gameTable)
		{
			this.table = gameTable;
			gameTable.addState(STATE_INIT, new TableState(gameTable) {
				@Override
				protected void onEnter(Object param)
				{
					super.onEnter(param);
				}

				@Override
				protected void onTableMessage(TableMessage msg)
				{
					changeState(STATE_PLAY, msg.message);
				}
			});
			gameTable.addState(STATE_PLAY, new TableState(gameTable) {
				@Override
				protected void onEnter(Object param)
				{
					super.onEnter(param);
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

		@Override
		public void onPlayerEnter(TablePlayerInfo player)
		{
			enters.incrementAndGet();
		}

		@Override
		public void onPlayerLeave(TablePlayerInfo player)
		{
			leaves.incrementAndGet();
		}

		@Override
		public void onDestroy(Table t)
		{
			destroys.incrementAndGet();
		}
	}

	static final AtomicReference<Integer> SEEN_TYPE = new AtomicReference<>();
	static final AtomicReference<CountDownLatch> TIMER_LATCH = new AtomicReference<>();

	static class TimerPlugin implements GamePlugin
	{
		Table table;

		@Override
		public void construct(Table gameTable)
		{
			this.table = gameTable;
			gameTable.addState(STATE_INIT, new TableState(gameTable) {
				@Override
				protected void onEnter(Object param)
				{
					super.onEnter(param);
					setTimeOut(50, 7, "p");
				}

				@Override
				protected void onTimeout(int type, Object param)
				{
					SEEN_TYPE.set(type);
					CountDownLatch latch = TIMER_LATCH.get();
					if (latch != null)
					{
						latch.countDown();
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

	@Test
	void changeStateAndDispatchMessage() throws Exception
	{
		Table table = new Table(1001, 1, DemoPlugin.class);
		TablePlayerInfo creator = new TablePlayerInfo();
		creator.setUserId("u1");
		assertTrue(table.init(creator, null));

		table.start(STATE_INIT);
		assertEquals(STATE_INIT, table.curStateId());

		Message m = new Message();
		m.setId((short) 1);
		m.setData(new byte[] { 1, 2, 3 });
		table.putMsg(creator, m);

		long deadline = System.currentTimeMillis() + 3000;
		while (table.curStateId() != STATE_PLAY && System.currentTimeMillis() < deadline)
		{
			Thread.sleep(10);
		}
		assertEquals(STATE_PLAY, table.curStateId());

		DemoPlugin plugin = table.getGamePlugin(DemoPlugin.class);
		table.addPlayer(creator);
		assertEquals(1, plugin.enters.get());
		table.removePlayer(creator);
		assertEquals(1, plugin.leaves.get());

		table.setDestroy(true);
		assertEquals(1, plugin.destroys.get());
	}

	@Test
	void timerTriggerInvokesStateHook() throws Exception
	{
		CountDownLatch latch = new CountDownLatch(1);
		TIMER_LATCH.set(latch);
		SEEN_TYPE.set(null);

		TableMgr.addPlugin(2, TimerPlugin.class);
		Table table = new Table(2002, 2, TimerPlugin.class);
		assertTrue(table.init(new TablePlayerInfo(), null));
		TableMgr.addTable(table);
		try
		{
			table.start(STATE_INIT);
			assertTrue(latch.await(3, TimeUnit.SECONDS));
			assertEquals(7, SEEN_TYPE.get().intValue());
		}
		finally
		{
			TableMgr.destroyTable(table);
		}
	}
}
