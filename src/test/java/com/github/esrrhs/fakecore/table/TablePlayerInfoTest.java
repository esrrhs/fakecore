package com.github.esrrhs.fakecore.table;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TablePlayerInfoTest
{
	@Test
	void beanAccessors()
	{
		TablePlayerInfo p = new TablePlayerInfo();
		p.setUserId("u");
		p.setTableId(10);
		p.setRoomId("r");
		p.setSeatId(3);
		assertEquals("u", p.getUserId());
		assertEquals(10, p.getTableId());
		assertEquals("r", p.getRoomId());
		assertEquals(3, p.getSeatId());
	}
}
