package org.alfine.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class PUP /* Pack UnPack */ {

	private static boolean verbose = false;

	private static void setVerbose() {
		PUP.verbose = true;
	}

	public static boolean isVerbose() {
		return PUP.verbose;
	}

	public static void main(String[] args) throws Exception {

		// Unjar a jar file or jar a directory recursively.
		// Nested jar files will be unpacked into <nested-jar-file-name>.dir.

		Options options = new Options();

		Option cmdOption = new Option("c", "cmd", true, "Select command `pack` or `unpack`.");
		cmdOption.setRequired(true);
		options.addOption(cmdOption);

		Option verboseOption = new Option("v", "verbose", false, "Output packed or unpacked files.");
		verboseOption.setRequired(false);
		options.addOption(verboseOption);

		Option srcOption = new Option("s", "src", true, "Path to jar file for `unpack` and directory for `pack`.");
		srcOption.setRequired(true);
		options.addOption(srcOption);

		Option dstOption = new Option("d", "dest", true, "Path to jar for `pack` and directory for `unpack`.");
		dstOption.setRequired(true);
		options.addOption(dstOption);

		CommandLine cmd = null;

		try {
			cmd = new DefaultParser().parse(options, args);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			new HelpFormatter().printHelp("pup", options);
			System.exit(1);
		}

		if (cmd.hasOption(verboseOption.getOpt())) {
			setVerbose();
		}

		Path src = Paths.get(cmd.getOptionValue(srcOption.getOpt()));
		Path dst = Paths.get(cmd.getOptionValue(dstOption.getOpt()));

		if (Files.notExists(src)) {
			throw new IllegalArgumentException("Source file does not exists `" + src + "`.");
		}

		switch (cmd.getOptionValue(cmdOption.getOpt())) {
			case "pack":
				if (Files.isDirectory(src)) {
					jar(src, dst);
				} else {
					throw new IllegalArgumentException("Expected `src` to point to a directory.");
				}
				break;
			case "unpack":
				if (!Files.isDirectory(src)) {
					unjar(new JarFile(src.toString()), dst);
				} else {
					throw new IllegalArgumentException("Expected `src` to point to an archive file.");
				}
				break;
			default:
				throw new IllegalArgumentException("Invalid command-argument. Valid values are `pack` and `unpack`.");
		}
	}

	private static void write(OutputStream out, InputStream in) throws Exception {
		int b;
		while ((b = in.read()) != -1)
			out.write(b);
	}

	private static void addJarEntry(ZipOutputStream out, Path src, Path entry) throws Exception {
		// Note: A directory entry is defined to be one whose name ends with a "/".
		String entryPath = entry.toString();
		if (isVerbose()) {
			if (Files.isDirectory(src)) {
				System.out.println("AddJarEntry: src=" + src + " as dir entry=" + entryPath + "/");
			} else {
				System.out.println("AddJarEntry: src=" + src + " as entry=" + entryPath);
			}
		}
		if (Files.isDirectory(src)) {
			out.putNextEntry(new JarEntry(entryPath + "/"));
			out.closeEntry();
		} else {
			out.putNextEntry(new JarEntry(entryPath));
			try (InputStream in = Files.newInputStream(src)) {
				write(out, in);
			} finally {
				out.closeEntry();
			}
		}
	}

	/** Return a files listing for the specified directory. */
	private static List<Path> listFiles(Path dir) {
		List<Path> entries = new LinkedList<>();
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
			ds.forEach(p -> entries.add(p));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error while creating files listing for directory: `" + dir + "`.");
		}
		return entries;
	}

	private static void jarrec(ZipOutputStream out, Path src, Path entry) throws Exception {
		List<Path> files = listFiles(src);
		for (Path target: files) {
			if (Files.isDirectory(target)) {
				Path mark = target.resolve(".jar");
				if (Files.exists(mark) && !Files.isDirectory(mark)) {
					String jarName;
					jarName = target.getFileName().toString();
					jarName = jarName.substring(0, jarName.lastIndexOf("."));
					// Produce jar from target in source tree.
					Path tmpJarFile = src.resolve(jarName);
					jar(target, tmpJarFile);
					// Add produced nested jar file as an entry in this jar.
					addJarEntry(out, tmpJarFile, entry.resolve(jarName));
					// Delete the produced jar from source tree.
					Files.delete(tmpJarFile);
				} else {
					// Add directory to jar file.
					addJarEntry(out, target, entry.resolve(target.getFileName() + "/"));
					// Descend into directory and add its content to 'out'.
					jarrec(out, target, entry.resolve(target.getFileName()));
				}
			} else {
				// Add all files except mark file: ".jar".
				if (!src.resolve(".jar").equals(target)) {
					addJarEntry(out, target, entry.resolve(target.getFileName()));
				}
			}
		}
	}

	public static void jar(Path src, Path dest) throws Exception {
		// Assume src is a directory, and that dest is the path to the produced archive.
		System.out.println("**** Open jar: src=" + src + ", dest=" + dest);
		try (
			OutputStream    os   = Files.newOutputStream(dest);
			ZipOutputStream out  = new ZipOutputStream(os)
		) {
			jarrec(out, src, Paths.get(""));
		} catch (Exception e) {
			e.printStackTrace();
		}
        System.out.println("**** Close jar: src=" + src + ", dest=" + dest);
    }

	/** Mark the specified folder as the top-level of an archive by
	 *  placing an empty file called `.jar` in the specified folder.*/
	private static void markDir(Path dir) throws Exception {
		if (!Files.exists(dir)) {
			Files.createDirectories(dir);
		}
		Files.createFile(dir.resolve(".jar"));
	}

	public static void unjar(JarFile jf, Path dest) throws Exception {
		System.out.println("**** Unjar file: `" + jf.getName());
		markDir(dest);
		for (Enumeration<JarEntry> it = jf.entries(); it.hasMoreElements();) {
			JarEntry je     = it.nextElement();
			String   name   = je.getName();
			Path     target = dest.resolve(name);
			if (PUP.isVerbose()) {
				System.out.println("extract " + je.getName() + "\n\t" + target.toAbsolutePath());
			}
			if (je.isDirectory()) {
				Files.createDirectory(target);
			} else {
				// Extract non-directory entry.
				try (
					InputStream  in  = jf.getInputStream(je);
					OutputStream out = Files.newOutputStream(target)
				) {
					int b;
					while ((b = in.read()) != -1)
						out.write(b);
				} catch (Exception e) {
					throw e;
				}
				// TODO: We could put these recursive unjar calls on
				//       a work queue and do them iteratively if too
				//       many files gets opened. We don't really expect
				//       to find that many levels nesting though...
				if (name.endsWith(".jar")) {
					String dir = name + ".dir";
					// Extract and delete extracted jar file (content will be
					// available in `dest.resolve(dir)`).
					System.out.println("nested unjar: " + target.toString() + " --> " + dest.resolve(dir));
					unjar(new JarFile(target.toString()), dest.resolve(dir));
					Files.delete(target);
				}
			}
		}
	}

	/** Copy files from `src' into `dest' recursively. */
	public static void treeCopy(Path src, Path dest) throws Exception {
		if (isVerbose()) {
			System.out.println("treeCopy: exploring\n\tsrc = " + src.toString() + "\n\t dst = " + dest.toString());
		}
		if (!Files.exists(dest)) {
			Files.createDirectories(dest);
		}
		listFiles(src).forEach(p -> {
			if (Files.isDirectory(p)) {
				try {
					treeCopy(p, dest.resolve(p.getFileName()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				if (!Files.exists(dest)) {
					try {
						if (isVerbose()) {
							System.out.println("treeCopy: mkdir " + dest);
						}
						Files.createDirectories(dest);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				try (OutputStream out = Files.newOutputStream(dest.resolve(p.getFileName()))) {
					if (isVerbose()) {
						System.out.println("treeCopy: copy " + p);
					}
					Files.copy(p, out);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
