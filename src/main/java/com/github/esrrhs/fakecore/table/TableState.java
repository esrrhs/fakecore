package com.github.esrrhs.fakecore.table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;

public class TableState
{
	private static Logger log = LoggerFactory.getLogger(TableState.class);

	protected Table gameTable;
	protected HashSet<String> waitTimer = new HashSet<>();
	protected long stateEnterTime;

	public TableState(Table gameTable)
	{
		this.gameTable = gameTable;
	}

	public void changeState(int state)
	{
		cancelAllWaitTimer();
		gameTable.changeState(state, null);
	}

	public void changeState(int state, Object param)
	{
		cancelAllWaitTimer();
		gameTable.changeState(state, param);
	}

	public void setTimeOut(int time)
	{
		String id = TableTimer.instance().addTimer(gameTable.getTableId(), time, 0, null).id;
		addWaitTimer(id);
	}

	public void setTimeOut(int time, int type, Object param)
	{
		String id = TableTimer.instance().addTimer(gameTable.getTableId(), time, type, param).id;
		addWaitTimer(id);
	}

	protected void onLeave()
	{
		cancelAllWaitTimer();
	}

	protected void onEnter(Object param)
	{
		stateEnterTime = System.currentTimeMillis();
	}

	protected void onTimeout(int type, Object param)
	{

	}

	protected void onTableTimeout(int type, Object param)
	{

	}

	protected void onTableMessage(TableMessage msg)
	{
	}

	protected boolean haveWaitTimer(String id)
	{
		return waitTimer.contains(id);
	}

	protected void addWaitTimer(String id)
	{
		this.waitTimer.add(id);
	}

	protected void delWaitTimer(String id)
	{
		waitTimer.remove(id);
	}

	public void cancelAllWaitTimer()
	{
		for (String id : waitTimer)
		{
			TableTimer.instance().cancelTimer(id);
		}
		waitTimer.clear();
	}

	protected void cancelTableAllWaitTimer()
	{
		gameTable.cancelTableAllWaitTimer();
	}

	public void cancelTimer(int type)
	{
		Iterator<String> iterator = waitTimer.iterator();
		while (iterator.hasNext())
		{
			String id = iterator.next();
			TableTimerEvent ev = TableTimer.instance().getTimer(id);
			if (ev != null && ev.type == type)
			{
				TableTimer.instance().endTimer(ev);
				iterator.remove();
			}
		}
	}

	public long getStateEnterTime()
	{
		return stateEnterTime;
	}

	public long getStateTime()
	{
		return System.currentTimeMillis() - stateEnterTime;
	}

}
