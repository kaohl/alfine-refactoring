package org.alfine.refactoring.framework;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Vector;

public class Workspace {

	private Path location; /* File representing workspace folder. */
	private Path srcPath;  /* Folder containing source archives. */
	private Path libPath;  /* Folder containing binary archives. */

	/* Projects loaded from configuration file. */
	private Map<String, ProjectConfiguration> projectMap;
	private Vector<ProjectConfiguration>      projectVec;

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

	/** Return vector with projects in top-down configuration file order (declare before use!). */
	public Vector<ProjectConfiguration> getProjectVec() {
		return this.projectVec;
	}

	/** Return the specified project or null if it does not exist. */
	public ProjectConfiguration getProject(String name) {
		return this.projectMap.get(name);
	}

	/** Initializes the workspace by loading configured projects and resources. */
	private void initialize() {

		boolean validConfig = true;

		for (ProjectConfiguration p : getProjectVec()) {
			if (!p.isValidate(this)) {
				validConfig = false;
			}
			// Run through all to print errors.
		}

		if (!validConfig) {
			throw new RuntimeException("Workspace configuration contains errors.");
		}

		for (ProjectConfiguration p : getProjectVec()) {
			p.open(this);
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
		// TODO: Check that archive contains path.
		return Files.exists(srcPath.resolve(jar));
	}

	/** Return true if a file with the specified name exists in the configured library folder. */
	public boolean isLibAvailable(String jar) {
		return Files.exists(libPath.resolve(jar));
	}
}
