package com.github.esrrhs.fakecore.table;

/**
 * Created by esrrhs on 2018/2/18.
 */
public class TablePlayerInfo
{
	private String userId;//用户ID
	private int tableId;//桌号
	private String roomId;//房间号
	private int seatId;//座位号

	public String getUserId()
	{
		return userId;
	}

	public void setUserId(String userId)
	{
		this.userId = userId;
	}

	public int getTableId()
	{
		return tableId;
	}

	public void setTableId(int tableId)
	{
		this.tableId = tableId;
	}

	public String getRoomId()
	{
		return roomId;
	}

	public void setRoomId(String roomId)
	{
		this.roomId = roomId;
	}

	public int getSeatId()
	{
		return seatId;
	}

	public void setSeatId(int seatId)
	{
		this.seatId = seatId;
	}
}
