package com.github.esrrhs.fakecore.table;

import com.github.esrrhs.fakecore.net.Message;

public final class TableMessage
{
	public int type;
	public TablePlayerInfo playerInfo;
	public Message message;
	public int stateId;
	public Object param;
	public int timerType;
	public String timerid;

	public enum Type
	{
		SwitchState, Timeout, Request
	}
}
