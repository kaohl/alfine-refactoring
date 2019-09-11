package org.alfine.refactoring.framework;

import java.nio.file.Path;

public class JavaProject {

	private Workspace           workspace;
	private ProjectConfiguration config;
	private Path                projectDir;
	
	public JavaProject(Workspace workspace, ProjectConfiguration config) {
		this.workspace = workspace;
		this.config     = config;
		this.projectDir = workspace.getLocation().resolve(config.getName());

		initialize();
	}

	/** Return `ProjectConfiguration'. */
	private ProjectConfiguration getConfig() {
		return this.config;
	}

	/** Return associated workspace. */
	public Workspace getWorkspace() {
		return this.workspace;
	}

	/** Return project folder in workspace. */
	public Path getLocation() {
		return getWorkspace().getLocation().resolve(getConfig().getName());
	}

	private void initialize() {
		// TODO: See Source.java:unpack       Load resources into project.
	}
}
