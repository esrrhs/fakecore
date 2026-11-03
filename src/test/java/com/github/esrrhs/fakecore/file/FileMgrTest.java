package com.github.esrrhs.fakecore.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileMgrTest
{
	@TempDir
	Path tempDir;

	@Test
	void loadLinesFromExistingFile() throws Exception
	{
		Path f = tempDir.resolve("lines.txt");
		Files.write(f, "a\nb\nc\n".getBytes(StandardCharsets.UTF_8));
		List<String> lines = FileMgr.loadLinesFromFile(f.toString());
		assertEquals(3, lines.size());
		assertEquals("a", lines.get(0));
		assertEquals("c", lines.get(2));
	}

	@Test
	void loadFromFileJoinsWithNewlines() throws Exception
	{
		Path f = tempDir.resolve("content.json");
		Files.write(f, "[{\"id\":1}]\n".getBytes(StandardCharsets.UTF_8));
		String content = FileMgr.loadFromFile(f.toString());
		assertTrue(content.contains("[{\"id\":1}]"));
		assertTrue(content.endsWith("\n"));
	}

	@Test
	void missingFileReturnsNull()
	{
		assertNull(FileMgr.loadLinesFromFile(tempDir.resolve("nope.txt").toString()));
		assertNull(FileMgr.loadFromFile(tempDir.resolve("nope2.txt").toString()));
	}
}
