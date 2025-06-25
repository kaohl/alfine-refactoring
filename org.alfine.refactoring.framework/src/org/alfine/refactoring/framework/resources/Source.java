package org.alfine.refactoring.framework.resources;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

import org.alfine.util.PUP;

// The framework loads Source and Library objects.
// A project have a set of Source(s) and Libraries.
// A Source that contains many source roots must be extracted in a temporary target folder first
// and then imported piecewise by each project that uses it.
// A Source with a `parent' is an archive with many roots within.
// A Source with a parent exports into unzip folder of corresponding parent source target directory.
// Corresponding parent Source (with children) uses PUP to export target to output folder.

public class Source {

	private Source parent;
	private Path   source; /* Path to imported archive. */
	private String folder; /* Source root within archive. */
	private Path   target; /* Folder into which we unpack archive for source root extraction into projects folders. */

	/** Create a source root resource. */
	public Source(Path source, String folder, Path target) {
		this.parent = null;
		this.source = source;
		this.folder = folder;
		this.target = target;
	}

	/** Create a nested source root resource with root as `folder' within `parent' archive. */
	public Source(Source parent, String folder, Path target) {
		this.parent = parent;
		this.folder = folder;
		this.target = target;
	}

	/** Return the directory into which the parent archive was unpacked. */
	private Source getParent() {
		return this.parent;
	}

	/** Return `Path' to source archive or `null' if this archive has a parent. */
	public Path getSource() {
		return this.source;
	}

	/** Return the source root folder within associated source archive. */
	public String getFolder() {
		return this.folder;
	}

	/** Return `Path' to folder into which this archive was (or will be) unpacked. */
	public Path getTarget() {
		return this.target;
	}

	/** Import resources into targeted location. */
	public void importResource() {

		if (getParent() == null) {

			// Unjar `source' into `target' folder.

			try {

				if (!Files.exists(getTarget().toAbsolutePath())) {
					PUP.unjar(
						new JarFile(getSource().toAbsolutePath().toString()),
						getTarget().toAbsolutePath()
					);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		} else {

			getParent().importResource();

			if (!Files.exists(getTarget())) {
				try {
					PUP.treeCopy(
						getParent().getTarget().resolve(getFolder()),
						getTarget()
					);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				new Exception("Resource already exists.").printStackTrace();
			}
		}
	}

	public void exportResource(Path output) {

		// Package sources into a new archive named after the original source archive.

		if (getParent() == null) {

			// Jar `target' folder to `output' directory.

			try {

				output = output.resolve(getSource().getFileName());

				Path src = getTarget().toAbsolutePath();
				Path dst = output.toAbsolutePath();

				PUP.jar(src, dst);

			} catch (Exception e) {
				e.printStackTrace();
			}

		} else {

			if (Files.exists(getParent().getTarget()) && Files.exists(getTarget())) {
				try {
					PUP.treeCopy(
						getTarget(),
						getParent().getTarget().resolve(getFolder())
					);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				new Exception("Resource(s) could not be found.").printStackTrace();
			}
		}
	}
}