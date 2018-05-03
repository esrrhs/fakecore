package com.github.esrrhs.fakecore.table;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.esrrhs.fakecore.net.Message;

public final class Table extends Thread
{
	private static Logger log = LoggerFactory.getLogger(Table.class);

	private int roomId;
	private int tableId;

	private volatile boolean destroy = false;

	private ArrayBlockingQueue<TableMessage> queue = new ArrayBlockingQueue<>(256);
	private ArrayBlockingQueue<TableMessage> changeState = new ArrayBlockingQueue<>(2);

	private Map<Integer, TableState> states = new HashMap<>();
	private volatile int cur = -1;
	private int exceptionStateId = -1;

	private List<TablePlayerInfo> players = new ArrayList<>();

	private HashSet<String> waitTimer = new HashSet<>();

	private GamePlugin gamePlugin;

	public Table(int tableId, int roomId, Class<? extends GamePlugin> gamePlugin)
	{
		this.roomId = roomId;
		this.tableId = tableId;

		try
		{
			this.gamePlugin = gamePlugin.newInstance();
			this.gamePlugin.construct(this);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}

		if (getExceptionStateId() <= 0)
		{
			log.error("need exception state");
			throw new RuntimeException("need exception state " + this.getRoomId());
		}
	}

	public boolean init(TablePlayerInfo creator, Object param)
	{
		if (!gamePlugin.init(creator, param))
		{
			return false;
		}
		return true;
	}

	public void start(int initStateId)
	{
		this.start();
		changeState(initStateId, null);
		waitState(initStateId);
	}

	public int getTableId()
	{
		return tableId;
	}

	public void setTableTimeOut(int time)
	{
		String id = TableTimer.instance().addTimer(getTableId(), time, 0, null).id;
		addWaitTimer(id);
	}

	public void setTableTimeOut(int time, int type, Object param)
	{
		String id = TableTimer.instance().addTimer(getTableId(), time, type, param).id;
		addWaitTimer(id);
	}

	public boolean isPlayerInTable(TablePlayerInfo playerInfo)
	{
		return players.contains(playerInfo);
	}

	public int getPlayerCount()
	{
		return players.size();
	}

	public List<TablePlayerInfo> getPlayers()
	{
		List<TablePlayerInfo> playerInfos = new ArrayList<>();
		playerInfos.addAll(players);
		return playerInfos;
	}

	public TablePlayerInfo getPlayer(String userId)
	{
		for (TablePlayerInfo player : players)
		{
			if (player.getUserId().equals(userId))
			{
				return player;
			}
		}
		return null;
	}

	public TablePlayerInfo getPlayerBySeatId(int seatId)
	{
		for (TablePlayerInfo player : players)
		{
			if (player.getSeatId() == seatId)
			{
				return player;
			}
		}
		return null;
	}

	public void addPlayer(TablePlayerInfo newPlayer)
	{
		if (players.contains(newPlayer))
		{
			return;
		}
		newPlayer.setTableId(tableId);
		newPlayer.setRoomId(roomId + "");
		players.add(newPlayer);
	}

	public void removePlayer(TablePlayerInfo playerInfo)
	{
		if (playerInfo == null)
		{
			return;
		}
		players.remove(playerInfo);
		playerInfo.setTableId(0);
	}

	public int getRoomId()
	{
		return roomId;
	}

	public void addWaitTimer(String id)
	{
		this.waitTimer.add(id);
	}

	public void delWaitTimer(String id)
	{
		waitTimer.remove(id);
	}

	public boolean haveWaitTimer(String id)
	{
		return waitTimer.contains(id);
	}

	public void cancelTableAllWaitTimer()
	{
		for (String id : waitTimer)
		{
			TableTimer.instance().cancelTimer(id);
		}
		waitTimer.clear();
	}

	public GamePlugin getGamePlugin()
	{
		return gamePlugin;
	}

	public <T> T getGamePlugin(Class<T> c)
	{
		if (c.isInstance(gamePlugin))
		{
			return (T) gamePlugin;
		}
		return null;
	}

	public void setTimeOut(int time)
	{
		String id = TableTimer.instance().addTimer(getTableId(), time, 0, null).id;
		curState().addWaitTimer(id);
	}

	public void setTimeOut(int time, int type, Object param)
	{
		String id = TableTimer.instance().addTimer(getTableId(), time, type, param).id;
		curState().addWaitTimer(id);
	}

	public boolean isDestroy()
	{
		return destroy;
	}

	public void setDestroy(boolean destroy)
	{
		this.destroy = destroy;
	}

	protected void changeState(int stateId, Object param)
	{
		TableMessage msg = new TableMessage();
		msg.type = TableMessage.Type.SwitchState.ordinal();
		msg.stateId = stateId;
		msg.param = param;

		if (!changeState.isEmpty())
		{
			log.error("changeState overwrite {}", stateId);
			changeState.clear();
		}

		try
		{
			changeState.put(msg);
		}
		catch (Exception e)
		{
			log.error("putMsg fail ", e);
		}
	}

	public void timerTrigger(String id, int type, Object param)
	{
		TableMessage msg = new TableMessage();
		msg.type = TableMessage.Type.Timeout.ordinal();
		msg.param = param;
		msg.timerType = type;
		msg.timerid = id;
		try
		{
			queue.put(msg);
		}
		catch (Exception e)
		{
			log.error("timerTrigger fail ", e);
		}
	}

	public void putMsg(TablePlayerInfo playerInfo, Message message)
	{
		TableMessage msg = new TableMessage();
		msg.type = TableMessage.Type.Request.ordinal();
		msg.playerInfo = playerInfo;
		msg.message = message;
		try
		{
			queue.offer(msg, 100, TimeUnit.MILLISECONDS);
		}
		catch (Exception e)
		{
			log.error("putMsg fail ", e);
		}
	}

	public void waitState(int stateId)
	{
		while (curStateId() != stateId)
		{
			try
			{
				Thread.sleep(1);
			}
			catch (InterruptedException e)
			{
				log.info("waitState", e);
			}
		}
	}

	@Override
	public void run()
	{
		while (!destroy)
		{
			try
			{
				processStateMsg();

				TableMessage msg = queue.poll(50, TimeUnit.MILLISECONDS);
				if (msg != null)
				{
					processStateMsg();
					processTableMessage(msg);
				}
			}
			catch (Exception e)
			{
				log.info("queue poll ", e);
			}
		}
		log.info("Routine Quit {} {}", getRoomId(), getTableId());
	}

	private void processStateMsg() throws Exception
	{
		while (!changeState.isEmpty())
		{
			TableMessage msg = changeState.take();
			processTableMessage(msg);
		}
	}

	private void processTableMessage(TableMessage msg)
	{
		try
		{
			if (msg.type == TableMessage.Type.SwitchState.ordinal())
			{
				log.info("tableId {} change to state {}", getTableId(), msg.stateId);
				switchState(msg.stateId, msg.param);
			}
			else if (msg.type == TableMessage.Type.Timeout.ordinal())
			{
				if (haveWaitTimer(msg.timerid))
				{
					log.info("tableId {} time out", getTableId());
					delWaitTimer(msg.timerid);
					curState().onTableTimeout(msg.timerType, msg.param);
				}
				else if (curState().haveWaitTimer(msg.timerid))
				{
					log.info("tableId {} state {} time out", getTableId(), curStateId());
					curState().delWaitTimer(msg.timerid);
					curState().onTimeout(msg.timerType, msg.param);
				}
				else
				{
					log.info("tableId {} state {} has no timer {}", getTableId(), curStateId(), msg.timerid);
				}
			}
			else
			{
				curState().onTableMessage(msg);
			}
		}
		catch (Exception e)
		{
			log.error("processTableMessage Error", e);
			switchState(getExceptionStateId(), null);
		}
	}

	public boolean haveRoutineMsg()
	{
		return !queue.isEmpty() || !changeState.isEmpty();
	}

	public void addState(int stateId, TableState tableState)
	{
		states.put(stateId, tableState);
	}

	public int getExceptionStateId()
	{
		return exceptionStateId;
	}

	public void setExceptionStateId(int exceptionStateId)
	{
		this.exceptionStateId = exceptionStateId;
	}

	public TableState curState()
	{
		return states.get(cur);
	}

	public int curStateId()
	{
		return cur;
	}

	public long getCurStateBeginTime()
	{
		return states.get(cur).getStateEnterTime();
	}

	public void switchState(int id, Object param)
	{
		if (cur != -1)
		{
			states.get(cur).onLeave();
		}
		log.info("tableId:{} state machine switch fromState:{} | toState:{}", getTableId(), cur, id);
		cur = id;
		states.get(cur).onEnter(param);
	}

}
