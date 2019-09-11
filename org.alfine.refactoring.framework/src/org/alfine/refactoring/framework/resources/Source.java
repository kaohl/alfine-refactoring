package org.alfine.refactoring.framework.resources;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.alfine.refactoring.framework.JavaProject;
import org.alfine.util.PUP;
import org.eclipse.jdt.core.IClasspathEntry;

/** Models a source classpath entry. */
public class Source {

	private JavaProject project; /* The project associated with this source. */
	private Resource    archive; /* The underlying archive. */
	private String      folder;  /* Sub-folder in associated archive. */

	public Source(JavaProject project, Resource archive, String folder) {
		this.project = project;
		this.archive = archive;

		// Assume `folder' has a leading "/" which should be removed.

		this.folder  = folder.substring(1);
	}

	public JavaProject getProject() {
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

	private String getUnpackDirName() {
		return "" + getResource().getLocation().getFileName() + ".dir";
	}

	private Path getSourceDir() {
		return getProject().getLocation().resolve(getUnpackDirName());
	}

	private Path getSourceRoot() {
		return getProject().getLocation().resolve(getUnpackDirName()).resolve(getFolder());
	}

	public IClasspathEntry getClasspath() {
		return null;
		// Use getSourceRoot() here.
	}

	/** Import resource into project. */
	public void unpack() {

		try {
			// PUP.main(new String[] {"" + getSourceDir(), } );
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	/** */
	public void export() {

		Path outUnpackDir = project.getWorkspace().getOutPath().resolve(getUnpackDirName());
		Path srcUnpackDir = getSourceDir();

		if (!Files.exists(outUnpackDir)) {
			// TODO: Extract archive in `outPath'.
		}

		/* Copy from `srcUnpackDir' into `outUnpackDir' starting at specified folder. */

		PUP.copy(srcUnpackDir, outUnpackDir, getFolder());
	}
}
