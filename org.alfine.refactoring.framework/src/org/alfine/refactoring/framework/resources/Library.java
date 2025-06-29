package org.alfine.refactoring.framework.resources;

import java.nio.file.Path;

public class Library {

	private Path     binaryArchive; /* Required */
	private Path     sourceArchive; /* Optional */
	private boolean  export;        /* Whether the library is exported to dependees. */

	public Library(Path ba, Path sa, boolean export) {
		this.binaryArchive = ba;
		this.sourceArchive = sa;
		this.export        = export;
	}

	public Path getSourcePath() {
		return sourceArchive.endsWith("?") ? null : this.sourceArchive;
	}
	
	public Path getBinaryPath() {
		return this.binaryArchive;
	}

	public boolean isExported() {
		return this.export;
	}
}
