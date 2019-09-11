package org.alfine.refactoring.framework.resources;

import java.nio.file.Path;

import org.alfine.refactoring.framework.ProjectConfiguration;

/** Models a source classpath entry. */
public class Source {

	private ProjectConfiguration  project; /* The project associated with this source. */
	private Resource archive; /* The underlying archive. */
	private String   folder;  /* Sub-folder in associated archive. */

	public Source(ProjectConfiguration project, Resource archive, String folder) {
		this.project = project;
		this.archive = archive;
		this.folder  = folder;
	}

	public ProjectConfiguration getProject() {
		return this.project;
	}

	public Resource getResource() {
		return this.archive;
	}

	public String getFolder() {
		return this.folder;
	}

	/** Return a path to the underlying resource.
	 *  The resulting path can be put on a project's classpath. */
	public Path getPath() {

		// Path projectDir = project.getProjectDir();
		// Path root       = new Path()

		return null;
	}
}
