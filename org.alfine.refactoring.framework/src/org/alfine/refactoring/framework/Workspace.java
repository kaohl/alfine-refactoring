package org.alfine.refactoring.framework;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Vector;

import org.alfine.refactoring.framework.resources.Source;

public class Workspace {

	private Path location; /* Workspace folder. */
	private Path srcPath;  /* Folder containing source archives. */
	private Path libPath;  /* Folder containing binary archives. */
	private Path outPath;  /* Destination folder for transformed source archives. */

	/* Project configuration from configuration file. */
	private Map<String, ProjectConfiguration> projectMap;
	private Vector<ProjectConfiguration>      projectVec;
	private Map<String, JavaProject>         projects;

	public Workspace(WorkspaceConfiguration config) {
		this.location   = config.getLocation();
		this.projectVec = config.getProjects();
		this.projectMap = config.getProjectMap();

		initialize();
	}

	/** Return workspace folder. */
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

	/** Return `Path' to source output folder. */
	public Path getOutPath() {
		return this.outPath;
	}

	/** Return vector with projects in top-down configuration file order (declare before use!). */
	public Vector<ProjectConfiguration> getProjectVec() {
		return this.projectVec;
	}

	/** Return the specified project or null if it does not exist. */
	public ProjectConfiguration getProjectConfiguration(String name) {
		return this.projectMap.get(name);
	}

	/** Initializes the workspace by loading configured projects and resources. */
	private void initialize() {

		boolean validConfig = true;

		for (ProjectConfiguration p : getProjectVec()) {
			if (!p.validate(this)) {
				validConfig = false;
			}
			// Run through all to print errors.
		}

		if (!validConfig) {
			throw new RuntimeException("Workspace configuration contains errors.");
		}

		for (ProjectConfiguration p : getProjectVec()) {
			projects.put(p.getName(), new JavaProject(this, p, true));
		}
	}

	/** Return true if a project with the specified name exists. */
	public boolean hasProject(String name) {
		return this.projectMap.containsKey(name);
	}

	/** Return true if a file with the specified name exists in the configured source folder. */
	public boolean isSrcAvailable(String jar) {
		return Files.exists(srcPath.resolve(jar));
	}

	/** Return true if a file with the specified name exists in the configured source folder. */
	public boolean isSrcAvailable(String dir, String jar) {
		// TODO: Check that archive contains `dir'.
		return Files.exists(srcPath.resolve(jar));
	}

	/** Return true if a file with the specified name exists in the configured library folder. */
	public boolean isLibAvailable(String jar) {
		return Files.exists(libPath.resolve(jar));
	}

	public void exportSource() {

		Path output = getOutPath();

		for (String key : projects.keySet()) {
			projects.get(key).exportSource(output);
		}

		/* Finalize export of shared source archives. */

		Map<Path, Source> sharedArchives = null;

		sharedArchives = JavaProject.getSharedSourceArchives();

		for (Path p : sharedArchives.keySet()) {
			sharedArchives.get(p).exportResource(output);
		}
	}
}
