package com.github.esrrhs.fakecore.args;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArgsMgrTest
{
	@Test
	void parseEmptyArgsLeavesObject()
	{
		ArgsMgr.parse(new String[0]);
		assertNotNull(ArgsMgr.getJsonObject());
	}

	@Test
	void parseJsonArgument()
	{
		ArgsMgr.parse(new String[] { "{\"gsId\":42,\"name\":\"demo\"}" });
		assertEquals(42, ArgsMgr.getJsonObject().getIntValue("gsId"));
		assertEquals("demo", ArgsMgr.getJsonObject().getString("name"));
		assertTrue(ArgsMgr.getJsonObject().containsKey("gsId"));
		assertFalse(ArgsMgr.getJsonObject().containsKey("missing"));
	}

	@Test
	void parseOverwritesPrevious()
	{
		ArgsMgr.parse(new String[] { "{\"gsId\":1}" });
		ArgsMgr.parse(new String[] { "{\"gsId\":99}" });
		assertEquals(99, ArgsMgr.getJsonObject().getIntValue("gsId"));
	}
}
