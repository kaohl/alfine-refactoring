package org.alfine.refactoring.framework.resources;

import java.nio.file.Path;

/** Model project resources in workspace. (We assume that the workspace loader imports all archives by default.)*/
public abstract class Resource {

	public static final Resource NULL = new Resource(null) {};

	private Path location; /* Location in input source folder. */

	public Resource(Path location) {
		this.location = location;
	}

	public Path getLocation() {
		return this.location;
	}

	
	/* 1. Include whole source archive
	 * 2. Include everything below a subdirectory of an artifact
	 * 3. Include Library (and optionally export it)
	 */
	
	// 1. Unzip all source artifacts in a resource folder outside all projects.
	// 2. Add correct dir in unzipped archive to project classpaths that reference them.
	// This way we should be able to transform resources in place and just repack source archives when done.
	
	// We should not deal with import and export here,
	// we just provide a means of referring to workspace
	// resources. The workspace loader deals with import
	// and export of source and binary artifacts!
}
