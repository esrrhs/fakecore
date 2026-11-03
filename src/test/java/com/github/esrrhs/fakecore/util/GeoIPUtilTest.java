package com.github.esrrhs.fakecore.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class GeoIPUtilTest
{
	@Test
	void missingDbReturnsNull()
	{
		// Default test classpath does not include GeoLite2-Country.mmdb
		assertFalse(GeoIPUtil.isAvailable());
		assertNull(GeoIPUtil.getCountryCode("8.8.8.8"));
	}
}
