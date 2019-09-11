package org.alfine.refactoring.framework;

import java.util.Vector;

import org.alfine.utils.Pair;

public class ProjectConfiguration {

	/* Unique ID generator for keeping track of load order,
	 * i.e., the order in which project configuration appears
	 * in the configuration file. */
	private static long idCounter;

	/* Unique ID identifying the project. */
	private final long id = idCounter++;
	
	private final String                      name; /* The project name (Unique). */
	private final Vector<String>              deps; /* Project dependencies. */
	private final Vector<Pair<String,String>> srcs; /* Source entries. */
	private final Vector<Pair<String,String>> libs; /* Library entries. */
	private final Vector<Pair<String,String>> exps; /* Exported classpath entries. */
	private final Vector<Pair<String,String>> vars; /* Source entries open for transformation. */

	private final boolean isValidConfig;

	public ProjectConfiguration(
		String                      name,
		Vector<String>              deps,
		Vector<Pair<String,String>> srcs,
		Vector<Pair<String,String>> libs,
		Vector<Pair<String,String>> exps,
		Vector<Pair<String,String>> vars,
		boolean isValidConfig) {

		this.name = name;
		this.deps = deps;
		this.srcs = srcs;
		this.libs = libs;
		this.exps = exps;
		this.vars = vars;

		this.isValidConfig = isValidConfig;
		
	}

	public String getName() {
		return this.name;
	}

	private boolean isConfigValid() {
		return this.isValidConfig;
	}

	/** Validate project configuration. */
	public boolean validate(Workspace ws) {

		if (!isConfigValid()) {
			System.out.printf("Project configuration for project `%s' contains syntax errors.", getName());
		}

		boolean result = true;

		for (String dep : deps) {
			if (!ws.hasProject(dep)) {
				System.err.printf("Invalid dependency `" + dep + "' in project `" + getName() + "'");
				result = false;
			}
		}

		for (Pair<String, String> p : srcs) {

			String dir = p.getFirst();
			String jar = p.getSecond();

			if (!ws.isSrcAvailable(dir, jar)) {
				System.err.printf("Couldn't find src `" + jar + "' for project `" + getName() + "'");
				result = false;
			}
		}

		for (Pair<String, String> p : vars) {

			String dir = p.getFirst();
			String jar = p.getSecond();

			if (!ws.isSrcAvailable(dir, jar)) {
				System.err.printf("Couldn't find var `" + jar + "' for project `" + getName() + "'");
				result = false;
			}
		}

		for (Pair<String, String> p : libs) {

			String bin = p.getFirst();
			String src = p.getSecond();

			if (!ws.isLibAvailable(bin)) {
				System.err.printf("Couldn't find bin `" + src + "' for project `" + getName() + "'");
				result = false;
			}

			if (!ws.isSrcAvailable(src)) {
				System.err.printf("Couldn't find src `" + src + "' for project `" + getName() + "'");
				result = false;
			}
		}

		for (Pair<String, String> p : exps) {

			String bin = p.getFirst();
			String src = p.getSecond();

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