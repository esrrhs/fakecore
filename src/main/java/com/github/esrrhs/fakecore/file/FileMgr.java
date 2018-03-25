package com.github.esrrhs.fakecore.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by esrrhs on 2018/2/27.
 */
public class FileMgr
{
	private static Logger log = LoggerFactory.getLogger(FileMgr.class);

	public static List<String> loadLinesFromFile(String filePath)
	{
		try
		{
			List<String> lines = Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8);
			return lines;
		}
		catch (Exception e)
		{
			log.error("loadLinesFromFile", e);
		}
		return null;
	}

	public static String loadFromFile(String filePath)
	{
		try
		{
			List<String> lines = loadLinesFromFile(filePath);
			StringBuilder sb = new StringBuilder();
			for (String line : lines)
			{
				sb.append(line);
				sb.append("\n");
			}

			return sb.toString();
		}
		catch (Exception e)
		{
			log.error("loadContentFromFile", e);
		}
		return null;
	}
}
