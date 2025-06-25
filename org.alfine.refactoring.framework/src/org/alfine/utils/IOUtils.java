package org.alfine.utils;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class IOUtils {
	public static void appendLineToFile(Path path, String line) throws Exception {
		appendCharsToFile(path, (line + System.getProperty("line.separator")).toCharArray());
	}
	
	public static void appendToFile(Path path, String line) throws Exception {
		appendCharsToFile(path, line.toCharArray());
	}
	
	public static void appendToFile(Path path, byte[] bytes) throws Exception {
		try (OutputStream out =
				Files.newOutputStream(
					path,
					java.nio.file.StandardOpenOption.CREATE,
					java.nio.file.StandardOpenOption.APPEND)) {
			out.write(bytes);
		} catch (Exception e) {
			throw e;
		}
	}
	
	public static void appendCharsToFile(Path path, char[] chars) throws Exception {
		try (BufferedWriter bw =
				Files.newBufferedWriter(
					path,
					java.nio.file.StandardOpenOption.CREATE,
					java.nio.file.StandardOpenOption.APPEND)) {
			bw.write(chars);
		} catch (Exception e) {
			throw e;
		}
	}
	
	public static Stream<String> readLinesFromFile(Path path) throws Exception {
		return Files.newBufferedReader(path).lines();
	}
}
