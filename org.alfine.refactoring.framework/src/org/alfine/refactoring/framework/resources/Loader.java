package org.alfine.refactoring.framework.resources;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfine.refactoring.framework.ProjectConfiguration;

public class Loader {

	private class SourceArchive extends Resource {

		private Set<Source> uses; /* Uses of archive. Used for exporting sources from multiple projects to shared archive. */

		public SourceArchive(Path location) {
			super(location);

			this.uses = new HashSet<Source>();
		}

		public void registerUse(Source source) {
			this.uses.add(source);
		}

		public void export(Path outputDir) {
			// TODO: Merge original archive with changed resources in `uses'.
		}
	}

	private class BinaryArchive extends Resource {
		public BinaryArchive(Path location) {
			super(location);
		}
	}

	/* Shared resources. */

	private static Map<String, SourceArchive> sourceArchives; /* Source archives (must be located under project folders). */
	private static Map<String, BinaryArchive> binaryArchives; /* Binary archives (can be located outside project folders).*/
	private static Map<String, Library>       libraries;      /* Loaded libraries. */

	/* Instance specific resources. */

	private ProjectConfiguration project; /* The associated project. */
	private Path    srcDir;  /* Location of source archives. */
	private Path    libDir;  /* Location of binary archives. */
	private Path    outDir;  /* Output directory for transformed source archives. */

	public Loader(ProjectConfiguration p, Path srcDir, Path libDir, Path outDir) {
		this.srcDir = srcDir;
		this.libDir = libDir;
		this.outDir = outDir;
	}

	public Path getOutputDir() {
		return this.outDir;
	}

	public void exportSource() {
		for (String archiveName : Loader.sourceArchives.keySet()) {
			Loader.sourceArchives.get(archiveName).export(getOutputDir());
		}
	}

	public Library loadLibrary(String sourceArchiveName, String binaryArchiveName, boolean export) {

		if (!Loader.libraries.containsKey(binaryArchiveName)) {

			SourceArchive sa = loadSourceArchive(sourceArchiveName);
			BinaryArchive ba = loadBinaryArchive(binaryArchiveName);

			Loader.libraries.put(binaryArchiveName, new Library(ba, sa, export));
		}

		return Loader.libraries.get(binaryArchiveName);
	}

	public Source loadSource(String archiveName, String path) {

		// TODO: Extract folders up to `path' and everything below `path'
		//       into associated project folder. If path is "/", then we
		//       extract everything into associated project folder.

		SourceArchive archive = loadSourceArchive(archiveName);
		Source        source  = new Source(project, archive, path);

		archive.registerUse(source);

		return source;
	}

	private SourceArchive loadSourceArchive(String name) {

		Path location = srcDir.resolve(name);

		if (!Files.exists(location)) {
			throw new RuntimeException("Source archive does not exists: " + location.toString());
		}

		return new SourceArchive(location);
	}

	private BinaryArchive loadBinaryArchive(String name) {

		Path location = libDir.resolve(name);

		if (!Files.exists(location)) {
			throw new RuntimeException("Binary archive does not exists: " + location.toString());
		}

		return new BinaryArchive(location);
	}
}
