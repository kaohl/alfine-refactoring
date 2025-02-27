package org.alfine.refactoring.framework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.alfine.refactoring.framework.launch.CommandLineArguments;
import org.alfine.utils.Pair;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkspaceConfiguration {

	public static class SrcEntry {

		private String  dir;
		private Path    jar;
		private boolean isVariable;

		public SrcEntry(String dir, Path jar, boolean isVariable) {
			this.dir        = dir;
			this.jar        = jar;
			this.isVariable = isVariable;
		}

		public String getDir() {
			return this.dir;
		}

		public Path getJar() {
			return this.jar;
		}

		public boolean isVariable() {
			return this.isVariable;
		}
	}

	public static class LibEntry {

		private Path  lib;
		private Path  src;
		private boolean exp;

		public LibEntry(Path lib, Path src, boolean exp) {
			this.lib = lib;
			this.src = src;
			this.exp = exp;
		}

		public Path getLib() {
			return this.lib;
		}

		public Path getSrc() {
			return this.src;
		}

		public boolean isExported() {
			return this.exp;
		}
	}

	private CommandLineArguments args;

	private Path location; /* Workspace folder. */
	private Path srcPath;  /* Folder containing source archives. */
	private Path libPath;  /* Folder containing binary archives. */
	private Path config;   /* Project configuration file. */

	private Map<String, ProjectConfiguration> projectMap; /* Projects loaded from configuration file. */
	private Vector<ProjectConfiguration>      projects;   /* Project order as they appear in configuration file. */

	private static Properties   includedPackagesNames;
	private static Properties   includedCompilationUnitsNames;
	private static List<String> includedMethodNames;

	public static boolean hasMethodsConfig() {
		return includedMethodNames.size() > 0;
	}

	public static boolean hasPackagesConfig() {
		return includedPackagesNames.size() > 0 && includedCompilationUnitsNames.size() > 0;
	}

	public WorkspaceConfiguration(CommandLineArguments arguments) {
		this.args     = arguments;
		this.location = Paths.get(Platform.getInstanceLocation().getURL().getFile());
		this.srcPath  = this.location.resolve(arguments.getSrcFolder());
		this.libPath  = this.location.resolve(arguments.getLibFolder());
		this.config   = srcPath.resolve("workspace.config");

		Logger logger = LoggerFactory.getLogger(WorkspaceConfiguration.class);
		logger.info("Location: {}", location);

		Path variableConfig                = srcPath.resolve("variable.config");
		Path includePackageConfig          = srcPath.resolve("packages.config");
		Path includeCompilationUnitsConfig = srcPath.resolve("units.config");
		Path includeMethodConfig           = srcPath.resolve("methods.config");

		includedPackagesNames         = parseIncludedPackagesNames(includePackageConfig);
		includedCompilationUnitsNames = parseIncludedCompilationUnitsNames(includeCompilationUnitsConfig);
		includedMethodNames           = parseIncludedMethodNames(includeMethodConfig);

		Pair<Vector<ProjectConfiguration>, Map<String, ProjectConfiguration>> p;

		p = parseConfig(config, srcPath, libPath, parseVariables(variableConfig));

		this.projects   = p.getFirst();
		this.projectMap = p.getSecond();
	}

	public CommandLineArguments getArguments() {
		return this.args;
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

	/** Return `Path' to folder for source archives on success. */
	public Path getOutPath() {
		return this.location.resolve(getArguments().getOutputFolder());
	}

	/** Return `Path' to refactoring opportunity cache folder. */
	public Path getCachePath() {
		return this.location.resolve(getArguments().getCacheFolder());
	}

	/** Return `Path' to workspace configuration file. */
	public Path getConfigPath() {
		return this.config;
	}
	
	public List<String> getIncludedCompilationUnitsNames() {
		List<String> result = new ArrayList<String>();
		Properties ps = includedCompilationUnitsNames;
		for (String name : ps.keySet().stream().map(Object::toString).collect(Collectors.toList())) {
			result.addAll(
				Arrays.asList(includedCompilationUnitsNames.getProperty(name, "").split(" "))
				.parallelStream()
				.map(String::trim)
				.collect(Collectors.toList())
			);
		}
		return result;
	}
	
	public static List<String> getIncludedCompilationUnitsNamesForProject(String name) {
		return Arrays.asList(includedCompilationUnitsNames.getProperty(name, "").split(" "))
				.parallelStream()
				.map(String::trim)
				.collect(Collectors.toList());
	}

	private static Properties parseIncludedCompilationUnitsNames(Path path) {
		Properties ps = new Properties();
		try (InputStream in = Files.newInputStream(path)){
			ps.load(in);
		} catch (Exception e) {
			System.err.println("Failed to load: " + e.getMessage());
		}
		return ps;
	}

	public static List<String> getIncludedPackagesNamesForProject(String name) {
		// TODO: Should we split on newline?
		return Arrays.asList(includedPackagesNames.getProperty(name, "").split(" "))
				.parallelStream()
				.map(String::trim)
				.collect(Collectors.toList());
	}

	/* Note: If no `packages.config` file exists (or is effectively empty of package fragments),
	 *       the scope of the refactoring framework is empty and no opportunities will be found. */
	private static Properties parseIncludedPackagesNames(Path path) {
		Properties ps = new Properties();
		try (InputStream in = Files.newInputStream(path)){
			ps.load(in);
		} catch (Exception e) {
			System.err.println("Failed to load: " + e.getMessage());
		}
		return ps;
	}

	public static List<String> getIncludedMethodNames() {
		return includedMethodNames;
	}

	private List<String> parseIncludedMethodNames(Path includeMethodConfig) {
		try {
			return new ArrayList<String>(Files.lines(includeMethodConfig).collect(Collectors.toList()));
		} catch (IOException e) {
			System.err.println("Failed to load: " + e.getMessage());
		}
		return new ArrayList<>(0);
	}

	/** Return names of all source archives that are to be considered variable. */
	public static Set<String> parseVariables(Path variableConfig) {

		System.out.println("Parsing variable configuration.");

		Set<String> variables = new HashSet<>();

		try (BufferedReader br = Files.newBufferedReader(variableConfig)) {

			br.lines()
			.map    (line -> line.trim())
			.filter  (line -> !line.equals(""))
			.forEach(name -> { System.out.println("variable = " + name); variables.add(name); });

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to read configuration file: " + variableConfig.toAbsolutePath());
		}

		return variables;
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

	public static boolean isValidSrcEntry(String dir, Path jar) {

		if (!Files.exists(jar)) {
			return false;
		}

		try (JarFile jf = new JarFile(jar.toString())){

			if (dir.equals("/")) {
				return true;
			} else {
				return !(null == jf.getEntry(dir.endsWith("/") ? dir : dir + "/"));
			}

		} catch (IOException e) {
			return false;
		}
	}

	public static boolean isValidLibEntry(Path lib, Path src) {
		/* Note: Source archive is optional. */
		/*
		System.err.println("isValidLibEntry(): src.toString = " + src.toString());
		System.err.println("Files.exists(lib) = " + Files.exists(lib));
		System.err.println("Files.exists(src) = " + Files.exists(src));
		System.err.println("src.toString().equals(\"?\") = " + src.endsWith("?"));
		 */
		return Files.exists(lib) && (src.endsWith("?") || Files.exists(src));
	}

	/** Create `ProjectConfiguration' from configuration string. 
	 * @param libDir 
	 * @param srcDir */
	public static ProjectConfiguration parseProjectConfiguration(
		Set<String> dependencies,
		String      cnf,
		Path        srcDir,
		Path        libDir,
		Set<String> variables
	) {

		List<String> tokens = new ArrayList<>(Arrays.asList(cnf.trim().split("\\s+")));

		String label = tokens.remove(0).replaceAll("/", ".");

		System.out.println("Loading configuration for project: " + label);

		if (dependencies.contains(label)) {
			throw new RuntimeException("Duplicate configurations for project: `" + label + "'");
		}

		dependencies.add(label);

		Vector<String>   deps = new Vector<>(); /* Project dependencies. */
		Vector<SrcEntry> srcs = new Vector<>(); /* Source entries. */
		Vector<LibEntry> libs = new Vector<>(); /* Library entries. */

		// TODO: `libs' and `exps' should be added in order they appear on classpath.
		//       We must therefore preserve their order.
		//
		// Pair<Pair<String, String>, Boolean> : works for both soures and libraries.
		// ((src, folder), variable?)
		// ((lib, src),    export?)
		
		
		// We do not configure variable sources here, they are specified as part of the refactoring configuration.

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

		// Once `hasErrors` has been set it will not be unset.
		// We always go through the whole configuration file
		// and propagate the flag into the corresponding
		// project configuration.
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

					System.err.printf(format, label, "Missing `dir' argument (0) in `src' option.\n");

					if (isKeyword(dir)) {
						hasErrors = true;
						break Next;
					}

				} else if (!it.hasNext() || (token = jar = it.next()) == null || isKeyword(jar)) {

					System.err.printf(format, label, "Missing `jar' argument (1) in `src' option.\n");

					if (isKeyword(jar)) {
						hasErrors = true;
						break Next;
					}

				} else {

					System.out.println("Adding src entry: dir=" + dir + ", jar=" + jar);

					Path jarPath = srcDir.resolve(jar);

					hasErrors = !isValidSrcEntry(dir, jarPath);
					srcs.add(new SrcEntry(dir, jarPath, variables.contains(jar)));
				}

				break;

			case "lib":

				if (!it.hasNext() || (token = bin = it.next()) == null || isKeyword(bin)) {

					System.err.printf(format, label, "Missing `binary archive' argument (0) in `lib' option.\n");

					if (isKeyword(bin)) {
						hasErrors = true;
						break Next;
					}

				} else if (!it.hasNext() || (token = src = it.next()) == null || isKeyword(src)) {

					System.err.printf(format, label, "Missing `source archive' argument (1) in `lib' option.\n");

					if (isKeyword(src)) {
						hasErrors = true;
						break Next;
					}

				} else {

					Path binJarPath = libDir.resolve(bin);
					Path srcJarPath = srcDir.resolve(src);

					if (!isValidLibEntry(binJarPath, srcJarPath)) {
						System.err.printf(format, label, "Invalid lib entry: `lib " + bin + " " + src + "\n");
						hasErrors = true;
					}
					System.out.println("Adding lib entry: bin=" + bin + ", src=" + src);
					libs.add(new LibEntry(binJarPath, srcJarPath, false));
				}

				break;

			case "exp":

				if (!it.hasNext() || (token = bin = it.next()) == null || isKeyword(bin)) {

					System.err.printf(format, label, "Missing `binary archive' argument (0) in `exp' option.\n");

					if (isKeyword(bin)) {
						hasErrors = true;
						break Next;
					}

				} else if (!it.hasNext() || (token = src = it.next()) == null || isKeyword(src)) {

					System.err.printf(format, label, "Missing `source archive' argument (1) in `exp' option.\n");

					if (isKeyword(src)) {
						hasErrors = true;
						break Next;
					}

				} else {

					Path binJarPath = libDir.resolve(bin);
					Path srcJarPath = srcDir.resolve(src);

					if (!isValidLibEntry(binJarPath, srcJarPath)) {
						System.err.printf(format, label, "Invalid exp entry: `exp " + bin + " " + src + "\n");
						hasErrors = true;
					}

					System.out.println("Adding lib entry: bin=" + bin + ", src=" + src);

					libs.add(new LibEntry(binJarPath, srcJarPath, true));
				}

				break;

			case "dep":

				if (!it.hasNext() || (token = dep = it.next()) == null || isKeyword(dep)) {

					System.err.printf(format, label, "Missing `project name' argument (0) in `dep' option.\n");

					if (isKeyword(dep)) {
						hasErrors = true;
						break Next;
					}

				} else {
					if (!dependencies.contains(dep)) {
						hasErrors = true;
						System.err.printf(format, label, "Project dependency `" + dep + "' has not yet been declared.\n");
					}

					System.out.println("Adding dependency: dep=" + dep);

					deps.add(dep);
				}

				break;

			default:
				hasErrors = true;
				System.err.printf(format, label, "Discarding token: " + token + "\n");
			}
		}

		if (hasErrors) {
			System.err.printf("Bad configuration for project `%s'.\n", label);
		}

		List<String> includedPackagesNamesList = getIncludedPackagesNamesForProject(label);
		return new ProjectConfiguration(label, deps, srcs, libs, includedPackagesNamesList, !hasErrors);
	}

	/** Parse project configuration file and construct a `ProjectConfiguration' per project configuration.
	 *  Since it may be time-consuming to load project resources into corresponding project folders
	 *  we don't do this until the full configuration file has been parsed and checked. */
	public static Pair<Vector<ProjectConfiguration>, Map<String, ProjectConfiguration>> parseConfig(Path config, Path srcDir, Path libDir, Set<String> variables) {

		// The set projects for which configuration has already been loaded.
		Set<String> dependencies = new HashSet<String>();

		Vector<ProjectConfiguration>      vec = new Vector<>();
		Map<String, ProjectConfiguration> map = new HashMap<>();

		StringBuilder cnf = new StringBuilder();

		try (BufferedReader br = Files.newBufferedReader(config)) {

			br.lines()
			.map   (line -> line.trim())
			.filter (line -> !"".equals(line))
			.filter (line -> line.charAt(0) != '#') /* Remove one-line comments. */
			.flatMap(line -> { return Arrays.asList(line.split("\\s+")).stream(); })
			.map   (word -> word.trim())
			.forEach(token -> {
				if (token.equals("}")) {

					ProjectConfiguration p = parseProjectConfiguration(dependencies, cnf.toString(), srcDir, libDir, variables);

					vec.add(p);
					map.put(p.getName(), p);

					cnf.delete(0, cnf.length());

				} else if (!token.equals("{")) {
					System.out.println("token = " + token);
					cnf.append(" " + token);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}

		return new Pair<Vector<ProjectConfiguration>, Map<String, ProjectConfiguration>>(vec, map);
	}
}
