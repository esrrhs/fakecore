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

public class GeoIPUtil
{
	private static Logger log = LoggerFactory.getLogger(GMailUtil.class);
	private static DatabaseReader reader;

	static
	{
		try
		{
			URL url = GeoIPUtil.class.getResource("/GeoLite2-Country.mmdb");

			URLConnection urlConnection = url.openConnection();
			urlConnection.setUseCaches(false);

			InputStream inputStream = urlConnection.getInputStream();

			// This creates the DatabaseReader object. To improve performance, reuse
			// the object across lookups. The object is thread-safe.
			reader = new DatabaseReader.Builder(inputStream).build();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}

	public static String getCountryCode(String ip)
	{
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
        System.out.println(getCountryCode("128.101.101.101")); // 'US'
        System.out.println(getCountryCode("220.181.102.176"));
        System.out.println(getCountryCode("127.0.0.1"));
    }
}
