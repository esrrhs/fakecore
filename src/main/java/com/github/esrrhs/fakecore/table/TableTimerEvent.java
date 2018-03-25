package com.github.esrrhs.fakecore.table;

class TableTimerEvent
{
	public String id;
	public int tableId;
	public long start;
	public long expire;
	public int type;
	public Object param;

	public TableTimerEvent(String id, int tableId, long start, long expire, int type, Object param)
	{
		this.id = id;
		this.tableId = tableId;
		this.start = start;
		this.expire = expire;
		this.type = type;
		this.param = param;
	}
}
