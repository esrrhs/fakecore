package com.github.esrrhs.fakecore.table;

/**
 * Game logic plugin bound to a {@link Table}.
 * <p>
 * Java 8 default methods keep existing implementations compiling while allowing
 * the table runtime to invoke lifecycle hooks.
 */
public interface GamePlugin
{
	void construct(Table gameTable);

	boolean init(TablePlayerInfo creator, Object param);

	default void onPlayerEnter(TablePlayerInfo player)
	{
	}

	default void onPlayerLeave(TablePlayerInfo player)
	{
	}

	default void onPlayerDisconnect(TablePlayerInfo player)
	{
	}

	default void onPlayerReconnect(TablePlayerInfo player)
	{
	}

	default void onDestroy(Table table)
	{
	}
}
