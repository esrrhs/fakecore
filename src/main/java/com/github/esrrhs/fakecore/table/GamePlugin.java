package com.github.esrrhs.fakecore.table;

public interface GamePlugin
{
	void construct(Table gameTable);

	boolean init(TablePlayerInfo creator, Object param);
}
