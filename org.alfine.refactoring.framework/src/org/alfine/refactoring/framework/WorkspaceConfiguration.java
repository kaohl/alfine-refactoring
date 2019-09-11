package org.alfine.refactoring.framework;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.alfine.utils.Pair;

public class WorkspaceConfiguration {

	private Path location; /* Workspace folder. */
	private Path srcPath;  /* Folder containing source archives. */
	private Path libPath;  /* Folder containing binary archives. */
	private Path config;    /* Project configuration file. */

	private Map<String, ProjectConfiguration> projectMap; /* Projects loaded from configuration file. */
	private Vector<ProjectConfiguration>      projects;   /* Project order as they appear in configuration file. */

	public WorkspaceConfiguration(Path location, Path srcPath, Path libPath, Path config) {
		this.location = location;
		this.srcPath  = srcPath;
		this.libPath  = libPath;
		this.config    = config;

		Pair<Vector<ProjectConfiguration>, Map<String, ProjectConfiguration>> p;

		p = parseConfig(config);

		this.projects   = p.getFirst();
		this.projectMap = p.getSecond();
	}

	/** Return `Path' to workspace folder. */
	public Path getLocation() {
		return this.location;
	}

	/** Return `Path' to folder with source archives. */
	public Path getSrcPath() {
		return this.srcPath;
	}

	/** Return `Path' to folder with binary archives. */
	public Path getLibPath() {
		return this.libPath;
	}

	/** Return `Path' to workspace configuration file. */
	public Path getConfigPath() {
		return this.config;
	}

	/** Return projects loaded from configuration (load order preserved). */
	public Vector<ProjectConfiguration> getProjects() {
		return this.projects;
	}

	/** Return projects loaded from configuration. */
	public Map<String, ProjectConfiguration> getProjectMap() {
		return this.projectMap;
	}

	/** Return true if the specified string is a keyword. */
	private static boolean isKeyword(String s) {
		return "dep".equals(s) || "src".equals(s) || "lib".equals(s) || "exp".equals(s);
	}

	/** Create `ProjectConfiguration' from configuration string. */
	public static ProjectConfiguration parseProjectConfiguration(String cnf) {

		List<String> tokens = Arrays.asList(cnf.trim().split("\\s+"));

		String label = tokens.remove(0);

		Vector<String>              deps = new Vector<>(); /* Project dependencies. */
		Vector<Pair<String,String>> srcs = new Vector<>(); /* Source entries. */
		Vector<Pair<String,String>> libs = new Vector<>(); /* Library entries. */
		Vector<Pair<String,String>> exps = new Vector<>(); /* Exported archives. */
		Vector<Pair<String,String>> vars = new Vector<>(); /* Source entries open for transformation. */

		// We do not configure variable sources here. They specified as part of the refactoring configuration.

		// # single line comment
		// exp bin src
		// lib bin src
		// src dir jar
		// dep name
		// , where dir is any valid path to a folder in the specified archive; "/" means top-level,
		// and src should be "?" when not present in `exp' and `lib' options.

		// Example configuration file:
		//
		// junit-4.12 {
		//     # Depend on `hamcrest-core' project to get its exported archive on the classpath.
		//     dep hamcrest-core
		//     # We use `?' to indicate that the source is missing.
		//     exp junit-4.12.jar ?
		// }
		// extendj-8.1.2 {
		//     # Add specified jar as a source archive.
		//     src / extendj-8.1.2-gen-src.jar
		// }
		//
		// tests.type.wildcard01 {
		//     # Extract a subfolder of the specified jar and add as a source archive.
		//     src /tests/type/wildcard01 extendj-8.1.2-regression-src.jar
		// }

		String format = "Parse error while parsing configuration for project `%s':\n\t%s";

		boolean hasErrors = false;

		for (Iterator<String> it = tokens.iterator(); it.hasNext();) {

			String token = it.next();

			String dir = null;
			String jar = null;
			String bin = null;
			String src = null;
			String dep = null;

		Next: /* Go back and resume here if a keyword is found unexpectedly while parsing configuration. */

			/* Note: `token' is always assigned the read keyword before breaking out to `Next'. */

			switch (token) {
			case "src":

				if (!it.hasNext() || (token = dir = it.next()) == null || isKeyword(dir)) {

					System.err.printf(format, label, "Missing `dir' argument (0) in `src' option.");

					if (isKeyword(dir)) {
						hasErrors = true;
						break Next;
					}

				} else if (!it.hasNext() || (token = jar = it.next()) == null || isKeyword(jar)) {

					System.err.printf(format, label, "Missing `jar' argument (1) in `src' option.");

					if (isKeyword(jar)) {
						hasErrors = true;
						break Next;
					}

				} else {
					srcs.add(new Pair<String, String>(dir, jar));
				}

				break;

			case "lib":

				if (!it.hasNext() || (token = bin = it.next()) == null || isKeyword(bin)) {

					System.err.printf(format, label, "Missing `binary archive' argument (0) in `lib' option.");

					if (isKeyword(bin)) {
						hasErrors = true;
						break Next;
					}

				} else if (!it.hasNext() || (token = src = it.next()) == null || isKeyword(src)) {

					System.err.printf(format, label, "Missing `source archive' argument (1) in `lib' option.");

					if (isKeyword(src)) {
						hasErrors = true;
						break Next;
					}

				} else {
					libs.add(new Pair<String, String>(bin, src));
				}

				break;

			case "exp":

				if (!it.hasNext() || (token = bin = it.next()) == null || isKeyword(bin)) {

					System.err.printf(format, label, "Missing `binary archive' argument (0) in `exp' option.");

					if (isKeyword(bin)) {
						hasErrors = true;
						break Next;
					}

				} else if (!it.hasNext() || (token = src = it.next()) == null || isKeyword(src)) {

					System.err.printf(format, label, "Missing `source archive' argument (1) in `exp' option.");

					if (isKeyword(src)) {
						hasErrors = true;
						break Next;
					}

				} else {
					exps.add(new Pair<String, String>(bin, src));
				}

				break;

			case "dep":

				if (!it.hasNext() || (token = dep = it.next()) == null || isKeyword(dep)) {

					System.err.printf(format, label, "Missing `project name' argument (0) in `dep' option.");

					if (isKeyword(dep)) {
						hasErrors = true;
						break Next;
					}

				} else {
					deps.add(dep);
				}

				break;

			default:
				hasErrors = true;
				System.err.printf(format, label, "Discarding token: " + token);
			}
		}

		if (hasErrors) {
			System.out.printf("Project configuration for project `%s' contains syntax errors.", label);
		}

		return new ProjectConfiguration(label, deps, srcs, libs, exps, vars, !hasErrors);
	}

	/** Parse project configuration file and construct a `ProjectConfiguration' per project configuration.
	 *  Since it may be time-consuming to load project resources into corresponding project folders
	 *  we don't do this until the full configuration file has been parsed and checked. */
	public static Pair<Vector<ProjectConfiguration>, Map<String, ProjectConfiguration>> parseConfig(Path config) {

		Vector<ProjectConfiguration>      vec = new Vector<>();
		Map<String, ProjectConfiguration> map = new HashMap<>();

		StringBuilder cnf = new StringBuilder();

		try (BufferedReader br = Files.newBufferedReader(config)) {

			br.lines()
			.filter (line -> line.trim().charAt(0) == '#') /* Remove one-line comments. */
			.flatMap(line -> { return Arrays.asList(line.split("\\s+")).stream(); })
			.map   (word -> word.trim())
			.forEach(token -> {
				if (token.equals("}")) {

					ProjectConfiguration p = parseProjectConfiguration(cnf.toString());

					vec.add(p);
					map.put(p.getName(), p);

					cnf.delete(0, cnf.length());

				} else if (!token.equals("{")) {
					cnf.append(" " + token);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}

		return new Pair<Vector<ProjectConfiguration>, Map<String, ProjectConfiguration>>(vec, map);
	}
}
