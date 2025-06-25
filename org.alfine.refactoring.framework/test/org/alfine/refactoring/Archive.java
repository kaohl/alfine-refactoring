package org.alfine.refactoring;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.zip.ZipOutputStream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public class Archive {
	private static class Entry {
		public final Path   path;
		public final byte[] content;
		private Entry(Path path, byte[] bytes) {
			this.path    = path;
			this.content = bytes;
		}
	}

	private final String      name;
	private final List<Entry> entries;

	public Archive(String name) {
		this.name    = name;
		this.entries = new LinkedList<>();
	}

	public Archive add(Path path, byte[] content) {
		this.entries.add(new Entry(path, content));
		return this;
	}

	public Archive add(Path path, String content) {
		return this.add(path, content.getBytes());
	}

	public Archive add(String path, String content) {
		return this.add(Paths.get(path), content);
	}

	public void writeSourceArchive(Path location) throws Exception {
		Set<String> addedPaths = new HashSet<>();
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(location.resolve(this.name), StandardOpenOption.CREATE_NEW))) {
			for (Entry entry : this.entries) {
				// Write directories as empty entries whose name ends with "/".
				Path dir = entry.path.getParent();
				for (int i = 0; i < dir.getNameCount(); ++i) {
					final String p = dir.subpath(0, i + 1).toString() + "/";
					if (!addedPaths.contains(p)) {
						addedPaths.add(p);
						zip.putNextEntry(new JarEntry(p));
						zip.closeEntry();
					}
				}
				zip.putNextEntry(new JarEntry(entry.path.toString()));
				zip.write(entry.content);
				zip.closeEntry();
			}
		}
	}

	public void writeBinaryArchive(Path location) throws Exception {
		Path     root = Files.createTempDirectory("java-test-source");
		String[] args = new String[this.entries.size()];
		for (int i = 0; i < entries.size(); ++i) {
			Entry clazz = entries.get(i);
			Path  path  = root.resolve(clazz.path);
			Files.createDirectories(path.getParent());
			Files.write(path, clazz.content, StandardOpenOption.CREATE_NEW);
			args[i] = path.toString();
		}

		// Compile to validate code.
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler.run(null, null, null, args) != 0) {
			throw new Exception("Compilation failed");
		}

		// Package class-files in a JAR.
		try (
			OutputStream    out = Files.newOutputStream(location.resolve(this.name), StandardOpenOption.CREATE_NEW);
			ZipOutputStream zip = new ZipOutputStream(out);
		) {
			Set<String> addedPaths = new HashSet<>();
			for (int i = 0; i < entries.size(); ++i) {
				Entry  entry = entries.get(i);
				Path   path  = root.resolve(entry.path);
				String name  = path.getFileName().toString();
				String stem  = name.substring(0, name.indexOf("."));
				Path   bin   = root.resolve(entry.path.getParent()).resolve(stem + ".class");
				
				// Write directories as empty entries whose name ends with "/".
				Path dir = entry.path.getParent();
				for (int j = 0; j < dir.getNameCount(); ++j) {
					final String p = dir.subpath(0, j + 1).toString() + "/";
					if (!addedPaths.contains(p)) {
						addedPaths.add(p);
						zip.putNextEntry(new JarEntry(p));
						zip.closeEntry();
					}
				}
				// Write the class file.
				zip.putNextEntry(new JarEntry(bin.toString()));
				zip.write(Files.readAllBytes(bin));
				zip.closeEntry();
			}
		} catch (Exception e) {
			throw e;
		}
	}
}
