package org.alfine.refactoring.framework;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.alfine.refactoring.framework.WorkspaceConfiguration.LibEntry;
import org.alfine.refactoring.framework.WorkspaceConfiguration.SrcEntry;

public class ProjectConfiguration {

	/* Unique ID generator for keeping track of load order,
	 * i.e., the order in which project configuration appears
	 * in the configuration file. */
	private static long idCounter;

	/* Unique ID identifying the project. */
	private final long id = idCounter++;

	private final String           name; /* The project name (Unique). */
	private final List<String>     deps; /* Project dependencies. */
	private final List<SrcEntry>   srcs; /* Source entries. */
	private final List<LibEntry>   libs; /* Library entries. */
	private final Set<String>      includedPackagesNames; /* Qualified package names of variable packages. */
	private final boolean          isValidConfig;

	public ProjectConfiguration(
		String           name,
		List<String>     deps,
		List<SrcEntry>   srcs,
		List<LibEntry>   libs,
		List<String>     includedPackagesNames,
		boolean isValidConfig
	) {
		this.name = name;
		this.deps = deps;
		this.srcs = srcs;
		this.libs = libs;
		this.includedPackagesNames = includedPackagesNames.stream().collect(Collectors.toSet());
		this.isValidConfig = isValidConfig;
	}

	public String getName() {
		return this.name;
	}

	public Collection<String> getDeps() {
		return this.deps;
	}

	public Collection<SrcEntry> getSrcs() {
		return this.srcs;
	}

	public Collection<LibEntry> getLibs() {
		return this.libs;
	}

	public Set<String> getIncludedPackagesNames() {
		return this.includedPackagesNames;
	}

	private boolean isConfigValid() {
		return this.isValidConfig;
	}

	/** Validate project configuration. */
	public boolean validate(Workspace ws) {

		if (!isConfigValid()) {
			System.err.printf("Project configuration for project `%s' contains syntax errors.", getName());
		}

		return isConfigValid();

		// TODO: Consider validating everything in the parser..
		//       moving these `exists' checks and check that deps
		//       are declared (parsed) before use.

		/*
		boolean result = true;

		for (String dep : deps) {
			if (!ws.hasProject(dep)) {
				System.err.printf("Invalid dependency `" + dep + "' in project `" + getName() + "'");
				result = false;
			}
		}

		for (SrcEntry p : srcs) {

			String dir = p.getDir();
			Path   jar = p.getJar();

			if (!ws.isSrcAvailable(dir, jar)) {
				System.err.printf("Couldn't find src `" + jar + "' for project `" + getName() + "'");
				result = false;
			}
		}

		for (LibEntry p : libs) {

			Path bin = p.getLib();
			Path src = p.getSrc();

			if (!ws.isLibAvailable(bin)) {
				System.err.printf("Couldn't find bin `" + src + "' for project `" + getName() + "'");
				result = false;
			}

			if (!ws.isSrcAvailable(src)) {
				System.err.printf("Couldn't find src `" + src + "' for project `" + getName() + "'");
				result = false;
			}
		}
		return result;
		*/
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof ProjectConfiguration) {
			return id == ((ProjectConfiguration)object).id;
		}
		return false;
	}

	@Override
	public String toString() {
		return name + "{ TODO }";
	}
}