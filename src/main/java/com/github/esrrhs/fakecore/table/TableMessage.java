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

	public int getType()
	{
		return type;
	}

	public void setType(int type)
	{
		this.type = type;
	}

	public TablePlayerInfo getPlayerInfo()
	{
		return playerInfo;
	}

	public void setPlayerInfo(TablePlayerInfo playerInfo)
	{
		this.playerInfo = playerInfo;
	}

	public Message getMessage()
	{
		return message;
	}

	public void setMessage(Message message)
	{
		this.message = message;
	}

	public int getStateId()
	{
		return stateId;
	}

	public void setStateId(int stateId)
	{
		this.stateId = stateId;
	}

	public Object getParam()
	{
		return param;
	}

	public void setParam(Object param)
	{
		this.param = param;
	}

	public int getTimerType()
	{
		return timerType;
	}

	public void setTimerType(int timerType)
	{
		this.timerType = timerType;
	}

	public String getTimerid()
	{
		return timerid;
	}

	public void setTimerid(String timerid)
	{
		this.timerid = timerid;
	}
}
