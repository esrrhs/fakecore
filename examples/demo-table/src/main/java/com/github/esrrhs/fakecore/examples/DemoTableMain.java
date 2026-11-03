package com.github.esrrhs.fakecore.examples;

import com.github.esrrhs.fakecore.net.Message;
import com.github.esrrhs.fakecore.table.GamePlugin;
import com.github.esrrhs.fakecore.table.Table;
import com.github.esrrhs.fakecore.table.TableMgr;
import com.github.esrrhs.fakecore.table.TableMessage;
import com.github.esrrhs.fakecore.table.TablePlayerInfo;
import com.github.esrrhs.fakecore.table.TableState;

/**
 * Runnable sketch of a GamePlugin driving table state transitions without Redis/MySQL.
 *
 * <pre>
 *   # from repo root after installing fakecore locally:
 *   ./mvnw -f examples/demo-table/pom.xml -q exec:java
 * </pre>
 */
public class DemoTableMain
{
	public static final int STATE_WAIT = 1;
	public static final int STATE_PLAY = 2;
	public static final int STATE_END = 3;
	public static final int STATE_EXC = 99;
	public static final int ROOM_ID = 100;
	public static final int TABLE_ID = 1;

	public static class DemoPlugin implements GamePlugin
	{
		private Table table;

		@Override
		public void construct(Table gameTable)
		{
			this.table = gameTable;
			gameTable.addState(STATE_WAIT, new TableState(gameTable) {
				@Override
				protected void onEnter(Object param)
				{
					super.onEnter(param);
					System.out.println("[WAIT] enter");
				}

				@Override
				protected void onTableMessage(TableMessage msg)
				{
					System.out.println("[WAIT] got msg id=" + msg.message.getId() + " -> PLAY");
					changeState(STATE_PLAY);
				}
			});
			gameTable.addState(STATE_PLAY, new TableState(gameTable) {
				@Override
				protected void onEnter(Object param)
				{
					super.onEnter(param);
					System.out.println("[PLAY] enter, schedule end in 100ms");
					setTimeOut(100, 1, null);
				}

				@Override
				protected void onTimeout(int type, Object param)
				{
					System.out.println("[PLAY] timeout type=" + type + " -> END");
					changeState(STATE_END);
				}
			});
			gameTable.addState(STATE_END, new TableState(gameTable) {
				@Override
				protected void onEnter(Object param)
				{
					super.onEnter(param);
					System.out.println("[END] enter, destroying table");
					gameTable.setDestroy(true);
				}
			});
			gameTable.addState(STATE_EXC, new TableState(gameTable) {
				@Override
				protected void onEnter(Object param)
				{
					super.onEnter(param);
					System.out.println("[EXC] enter");
					gameTable.setDestroy(true);
				}
			});
			gameTable.setExceptionStateId(STATE_EXC);
		}

		@Override
		public boolean init(TablePlayerInfo creator, Object param)
		{
			table.addPlayer(creator);
			return true;
		}

		@Override
		public void onPlayerEnter(TablePlayerInfo player)
		{
			System.out.println("player enter " + player.getUserId());
		}

		@Override
		public void onDestroy(Table t)
		{
			System.out.println("table destroyed id=" + t.getTableId());
		}
	}

	public static void main(String[] args) throws Exception
	{
		TablePlayerInfo creator = new TablePlayerInfo();
		creator.setUserId("demo-user");
		creator.setSeatId(0);

		TableMgr.addPlugin(ROOM_ID, DemoPlugin.class);
		Table table = new Table(TABLE_ID, ROOM_ID, DemoPlugin.class);
		if (!table.init(creator, null))
		{
			throw new IllegalStateException("init failed");
		}
		TableMgr.addTable(table);
		table.start(STATE_WAIT);

		Message start = new Message();
		start.setId((short) 1);
		start.setData("go".getBytes());
		table.putMsg(creator, start);

		long deadline = System.currentTimeMillis() + 5000;
		while (!table.isDestroy() && System.currentTimeMillis() < deadline)
		{
			Thread.sleep(20);
		}
		System.out.println("demo finished destroy=" + table.isDestroy() + " state=" + table.curStateId());
		if (!table.isDestroy())
		{
			TableMgr.destroyTable(table);
			System.exit(1);
		}
		System.exit(0);
	}
}
