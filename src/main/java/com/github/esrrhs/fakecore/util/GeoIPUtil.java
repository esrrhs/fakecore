package com.github.esrrhs.fakecore.util;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.record.Country;

/**
 * GeoIP lookup against an optional MaxMind GeoLite2-Country database on the classpath.
 * <p>
 * The large {@code GeoLite2-Country.mmdb} is no longer packaged in the default jar.
 * Place it on the classpath (see {@code optional-resources/} in the repo) to enable lookups.
 * When the DB is absent, {@link #getCountryCode(String)} returns {@code null}.
 */
public class GeoIPUtil
{
	private static Logger log = LoggerFactory.getLogger(GeoIPUtil.class);
	private static DatabaseReader reader;

	static
	{
		try
		{
			URL url = GeoIPUtil.class.getResource("/GeoLite2-Country.mmdb");
			if (url == null)
			{
				log.warn("GeoLite2-Country.mmdb not on classpath; GeoIPUtil will return null");
			}
			else
			{
				URLConnection urlConnection = url.openConnection();
				urlConnection.setUseCaches(false);
				InputStream inputStream = urlConnection.getInputStream();
				reader = new DatabaseReader.Builder(inputStream).build();
				log.info("GeoIPUtil loaded GeoLite2-Country.mmdb");
			}
		}
		catch (Exception e)
		{
			log.warn("GeoIPUtil failed to load GeoLite2-Country.mmdb: {}", e.toString());
			reader = null;
		}
	}

	public static boolean isAvailable()
	{
		return reader != null;
	}

	/**
	 * @return ISO country code, or {@code null} when the DB is missing / lookup fails.
	 *         For private / unmapped addresses when the DB is present, returns {@code "CN"}
	 *         to preserve historical behaviour.
	 */
	public static String getCountryCode(String ip)
	{
		if (reader == null)
		{
			return null;
		}
		try
		{
			InetAddress ipAddress = InetAddress.getByName(ip);
			CountryResponse response = reader.country(ipAddress);
			Country country = response.getCountry();
			return country.getIsoCode();
		}
		catch (AddressNotFoundException e)
		{
			return "CN";
		}
		catch (Exception e)
		{
			log.error("getCountryCode " + ip, e);
			return "CN";
		}
	}

	public static void main(String[] args)
	{
		System.out.println(getCountryCode("128.101.101.101")); // 'US' when DB present
		System.out.println(getCountryCode("220.181.102.176"));
		System.out.println(getCountryCode("127.0.0.1"));
	}
}
