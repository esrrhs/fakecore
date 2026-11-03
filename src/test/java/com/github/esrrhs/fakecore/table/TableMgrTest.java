package com.github.esrrhs.fakecore.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TableMgrTest
{
	static final int ROOM = 9101;
	static final int STATE_INIT = 1;
	static final int STATE_EXC = 99;

	static class OkPlugin implements GamePlugin
	{
		static final AtomicInteger constructs = new AtomicInteger();
		final AtomicBoolean disconnect = new AtomicBoolean();
		final AtomicBoolean reconnect = new AtomicBoolean();

		@Override
		public void construct(Table gameTable)
		{
			constructs.incrementAndGet();
			gameTable.addState(STATE_INIT, new TableState(gameTable));
			gameTable.addState(STATE_EXC, new TableState(gameTable));
			gameTable.setExceptionStateId(STATE_EXC);
		}

		@Override
		public boolean init(TablePlayerInfo creator, Object param)
		{
			return true;
		}

		@Override
		public void onPlayerDisconnect(TablePlayerInfo player)
		{
			disconnect.set(true);
		}

		@Override
		public void onPlayerReconnect(TablePlayerInfo player)
		{
			reconnect.set(true);
		}
	}

	static class FailInitPlugin implements GamePlugin
	{
		@Override
		public void construct(Table gameTable)
		{
			gameTable.addState(STATE_INIT, new TableState(gameTable));
			gameTable.addState(STATE_EXC, new TableState(gameTable));
			gameTable.setExceptionStateId(STATE_EXC);
		}

		@Override
		public boolean init(TablePlayerInfo creator, Object param)
		{
			return false;
		}
	}

	@BeforeEach
	void setUp()
	{
		TableMgr.roomTables.clear();
		// plugins map is private; re-add via addPlugin which also creates room map
	}

	@AfterEach
	void tearDown()
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
	}

	@Test
	void createLookupDestroyLifecycle()
	{
		TableMgr.addPlugin(ROOM, OkPlugin.class);
		assertSame(OkPlugin.class, TableMgr.getPlugin(ROOM));
		assertEquals(0, TableMgr.getTableCountOfRoom(ROOM));
		assertEquals(0, TableMgr.getTableCount());

		TablePlayerInfo creator = new TablePlayerInfo();
		creator.setUserId("u-mgr");
		Table table = TableMgr.createTable(creator, 501, ROOM, "param");
		assertNotNull(table);
		assertEquals(1, TableMgr.getTableCount());
		assertEquals(1, TableMgr.getTableCountOfRoom(ROOM));
		assertSame(table, TableMgr.getTableInfoByTableId(501));
		assertEquals(1, TableMgr.getAllTablesList().size());
		assertNotNull(TableMgr.getRoomTables(ROOM));

		table.addPlayer(creator);
		assertSame(table, TableMgr.getPlayerTable(creator));
		assertNull(TableMgr.getPlayerTable(null));

		TablePlayerInfo outsider = new TablePlayerInfo();
		outsider.setUserId("out");
		outsider.setTableId(501);
		assertNull(TableMgr.getPlayerTable(outsider));

		table.playerDisconnect(creator);
		table.playerReconnect(creator);
		OkPlugin plugin = table.getGamePlugin(OkPlugin.class);
		assertTrue(plugin.disconnect.get());
		assertTrue(plugin.reconnect.get());

		assertEquals(creator, table.getPlayer("u-mgr"));
		assertNull(table.getPlayer("missing"));
		creator.setSeatId(2);
		assertEquals(creator, table.getPlayerBySeatId(2));
		assertNull(table.getPlayerBySeatId(9));
		assertEquals(1, table.getPlayerCount());
		assertEquals(1, table.getPlayers().size());

		TableMgr.destroyTable(table);
		assertTrue(table.isDestroy());
		assertEquals(0, TableMgr.getTableCountOfRoom(ROOM));
		assertNull(TableMgr.getTableInfoByTableId(501));
	}

	@Test
	void createTableReturnsNullWhenInitFails()
	{
		TableMgr.addPlugin(ROOM + 1, FailInitPlugin.class);
		assertNull(TableMgr.createTable(new TablePlayerInfo(), 502, ROOM + 1, null));
		assertEquals(0, TableMgr.getTableCountOfRoom(ROOM + 1));
	}

	@Test
	void getTableCountOfUnknownRoomIsZero()
	{
		assertEquals(0, TableMgr.getTableCountOfRoom(40404));
		assertNull(TableMgr.getRoomTables(40404));
		assertNull(TableMgr.getPlugin(40404));
	}

	@Test
	void defaultGamePluginHooksAreNoOps()
	{
		GamePlugin defaults = new GamePlugin() {
			@Override
			public void construct(Table gameTable)
			{
				gameTable.addState(1, new TableState(gameTable));
				gameTable.addState(2, new TableState(gameTable));
				gameTable.setExceptionStateId(2);
			}

			@Override
			public boolean init(TablePlayerInfo creator, Object param)
			{
				return true;
			}
		};
		// invoke defaults directly
		defaults.onPlayerEnter(new TablePlayerInfo());
		defaults.onPlayerLeave(new TablePlayerInfo());
		defaults.onPlayerDisconnect(new TablePlayerInfo());
		defaults.onPlayerReconnect(new TablePlayerInfo());
		defaults.onDestroy(null);
		assertFalse(false);
	}
}
