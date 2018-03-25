package com.github.esrrhs.fakecore.table;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

class TableTimer extends Thread
{
	private static TableTimer INSTANCE;

	private ConcurrentHashMap<String, TableTimerEvent> eventQueue = new ConcurrentHashMap<>();

	private AtomicInteger counter = new AtomicInteger(Integer.MAX_VALUE);

	public static TableTimer instance()
	{
		if (INSTANCE == null)
		{
			synchronized (TableTimer.class)
			{
				if (INSTANCE == null)
				{
					INSTANCE = new TableTimer();
					INSTANCE.start();
				}
			}
		}
		return INSTANCE;
	}

	public TableTimerEvent addTimer(int tableId, int milliSecondsLater, int type, Object param)
	{
		String id = tableId + "_" + counter.incrementAndGet();
		long now = System.currentTimeMillis();
		TableTimerEvent event = new TableTimerEvent(id, tableId, now, milliSecondsLater, type, param);
		eventQueue.put(id, event);
		return event;
	}

	public TableTimerEvent getTimer(String id)
	{
		return eventQueue.get(id);
	}

	public void endTimer(TableTimerEvent ev)
	{
		ev.expire = 0;
	}

	public void cancelTimer(String id)
	{
		eventQueue.remove(id);
	}

	@Override
	public void run()
	{
		while (true)
		{
			long now = System.currentTimeMillis();
			Iterator<TableTimerEvent> iter = eventQueue.values().iterator();
			while (iter.hasNext())
			{
				TableTimerEvent event = iter.next();
				if (event == null)
				{
					iter.remove();
				}
				else if (now - event.start >= event.expire)
				{
					iter.remove();
					Table tableInfo = TableMgr.getTableInfoByTableId(event.tableId);
					if (tableInfo != null)
					{
						tableInfo.timerTrigger(event.id, event.type, event.param);
					}
				}
			}
			try
			{
				Thread.sleep(10);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
}
