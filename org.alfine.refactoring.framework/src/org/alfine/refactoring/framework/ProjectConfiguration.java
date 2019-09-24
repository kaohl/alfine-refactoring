package org.alfine.refactoring.framework;

import java.util.Vector;

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
	private final Vector<String>   deps; /* Project dependencies. */
	private final Vector<SrcEntry> srcs; /* Source entries. */
	private final Vector<LibEntry> libs; /* Library entries. */

	private final boolean isValidConfig;

	public ProjectConfiguration(
		String           name,
		Vector<String>   deps,
		Vector<SrcEntry> srcs,
		Vector<LibEntry> libs,
		boolean isValidConfig) {

		this.name = name;
		this.deps = deps;
		this.srcs = srcs;
		this.libs = libs;

		this.isValidConfig = isValidConfig;
	}

	public String getName() {
		return this.name;
	}

	public Vector<String> getDeps() {
		return this.deps;
	}

	public Vector<SrcEntry> getSrcs() {
		return this.srcs;
	}

	public Vector<LibEntry> getLibs() {
		return this.libs;
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