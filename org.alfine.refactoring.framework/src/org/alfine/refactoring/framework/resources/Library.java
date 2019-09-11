package org.alfine.refactoring.framework.resources;

import java.nio.file.Path;

import org.eclipse.jdt.core.IClasspathEntry;

public class Library {
	private Resource binaryArchive; /* Required */
	private Resource sourceArchive; /* Optional */
	private boolean  export;        /* Whether the library is exported to dependees. */

	public Library(Resource ba, Resource sa, boolean export) {
		this.binaryArchive = ba;
		this.sourceArchive = sa;
		this.export        = export;
	}

	public Path getSourcePath() {
		return sourceArchive.getLocation();
	}
	
	public Path getBinaryPath() {
		return binaryArchive.getLocation();
	}

	public boolean isExported() {
		return this.export;
	}

	public IClasspathEntry getClasspathEntry() {
		return null; //JavaCore.newLibraryEntry(path, sourceAttachmentPath, sourceAttachmentRootPath, isExported)
	}
}
