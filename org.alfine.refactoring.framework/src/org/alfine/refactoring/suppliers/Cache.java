package org.alfine.refactoring.suppliers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class reflects the contents of a cache folder
 *  whose path is determined at application start-up. */
public final class Cache {

	private static final Map<String, String> cachePathMap =
		new HashMap<String, String>();

	private Path location;

	/** Create cache (user must clear by deleting cache folder manually).*/
	public Cache(Path location) {

		this.location = location;
		
		if (!Files.exists(location)) {
			try {
				Files.createDirectory(location);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void installCachePath(String refactoringID, String relativePath) {
		Logger logger = LoggerFactory.getLogger(Cache.class);
		logger.debug("install cache path: id = {}, relative path = {}", refactoringID, relativePath);
		Cache.cachePathMap.put(refactoringID, relativePath);
	}
	
	/** Return cache-local path for specified refactoring ID.*/
	public Path getCacheFilePath(String id) {
		Logger logger = LoggerFactory.getLogger(Cache.class);

		String path   = Cache.cachePathMap.get(id);
		Path   result = this.location.resolve(path);

		logger.debug("id = {}, rel = {}, abs = {}", id, path, result);

		return result;
	}

	/** Return lazily populated cache line stream associated with specified ID. */
	public Stream<String> getCacheLines(String refactoringID) {
		// Duplicate. See IOUtils.
		Path path = getCacheFilePath(refactoringID);
		try {
			return Files.newBufferedReader(path).lines();
		} catch (IOException e) {
			Logger logger = LoggerFactory.getLogger(Cache.class);
			logger.debug("Failed to open cache file, path = {}, exists = {}", path.toString(), Files.exists(path));

			e.printStackTrace();

			return java.util.stream.Stream.empty();
		}
	}

	/** Make a refactoring descriptor supplier by running the specified function. */
	public Iterator<RefactoringDescriptor> makeSupplier(Function<Cache, Iterator<RefactoringDescriptor>> fn) {
		return fn.apply(this);
	}

	public void write(RefactoringOpportunityContext context, RefactoringDescriptor descriptor) {
		// TODO: Use IOUtils.appendLineToFile(...) instead of `write`.
		// write(getCacheFilePath(descriptor.getRefactoringID()), descriptor.getCacheLine());

		Path descriptors = this.location.resolve(context.getContextPath()).resolve("descriptors.txt");
		write(descriptors, descriptor.getCacheLine());
	}

	/** Write opportunity to cache. */
	private static void write(Path path, String line) {
		// Duplicate. See IOUtils.appendLineToFile.
		if (!Files.exists(path)) {
			try {
				Files.createDirectories(path.getParent());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try (BufferedWriter bw =
				Files.newBufferedWriter(
					path,
					java.nio.file.StandardOpenOption.CREATE,
					java.nio.file.StandardOpenOption.APPEND)) {
			bw.write(line + System.getProperty("line.separator"));
		} catch (Exception e) {
			e.printStackTrace();			
		}
	}
}
