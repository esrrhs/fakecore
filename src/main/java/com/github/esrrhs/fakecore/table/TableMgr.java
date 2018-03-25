package com.github.esrrhs.fakecore.table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TableMgr
{
	private static Logger log = LoggerFactory.getLogger(TableMgr.class);

	public static Map<Integer, Map<Integer, Table>> roomTables = new ConcurrentHashMap<>();//roomId->(tableId->table)

	private static ConcurrentHashMap<Integer, Class<? extends GamePlugin>> gamePluginConcurrentHashMap = new ConcurrentHashMap<>();

	public static void addPlugin(int roomId, Class<? extends GamePlugin> plugin)
	{
		Map<Integer, Table> tableMap = new ConcurrentHashMap<>();
		roomTables.put(roomId, tableMap);

		gamePluginConcurrentHashMap.put(roomId, plugin);
	}

	public static Class<? extends GamePlugin> getPlugin(int roomId)
	{
		return gamePluginConcurrentHashMap.get(roomId);
	}

	public static Table createTable(TablePlayerInfo creator, int tableId, int roomId, Object param)
	{
		Table table = new Table(tableId, roomId, TableMgr.getPlugin(roomId));
		if (!table.init(creator, param))
		{
			return null;
		}
		TableMgr.addTable(table);
		return table;
	}

	public static ArrayList<Table> getAllTablesList()
	{
		ArrayList<Table> ret = new ArrayList<>();
		for (Map<Integer, Table> X : roomTables.values())
		{
			for (Table T : X.values())
			{
				ret.add(T);
			}
		}
		return ret;
	}

	public static Table getTableInfoByTableId(int tableId)
	{
		for (Map<Integer, Table> tables : roomTables.values())
		{
			Table tableInfo = tables.get(tableId);
			if (tableInfo != null)
			{
				return tableInfo;
			}
		}
		return null;
	}

	public static Table getPlayerTable(TablePlayerInfo playerInfo)
	{
		if (playerInfo == null)
		{
			return null;
		}
		Table tableInfo = getTableInfoByTableId(playerInfo.getTableId());
		if (tableInfo != null && tableInfo.isPlayerInTable(playerInfo))
		{
			return tableInfo;
		}
		return null;
	}

	public static Map<Integer, Table> getRoomTables(int roomId)
	{
		return roomTables.get(roomId);
	}

	public static int getTableCountOfRoom(int roomId)
	{
		Map<Integer, Table> tableMap = roomTables.get(roomId);
		if (tableMap != null)
		{
			return tableMap.size();
		}
		return 0;
	}

	public static int getTableCount()
	{
		int count = 0;
		for (Map<Integer, Table> tables : roomTables.values())
		{
			count += tables.size();
		}
		return count;
	}

	/**
	 * 初始化牌桌
	 * @param table
	 * @return
	 */
	public static void addTable(Table table)
	{
		Map<Integer, Table> tableMap = getRoomTables(table.getRoomId());
		tableMap.put(table.getTableId(), table);
	}

	/**
	 * 销毁牌桌
	 * @param table
	 */
	public static void destroyTable(Table table)
	{
		log.info("debug destroyTable tableId:{}", table.getTableId());
		Integer tableId = table.getTableId();
		Map<Integer, Table> tableMap = roomTables.get(table.getRoomId());
		tableMap.remove(tableId);
		table.setDestroy(true);
	}

}
