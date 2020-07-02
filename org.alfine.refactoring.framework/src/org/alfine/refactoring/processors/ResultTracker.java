package org.alfine.refactoring.processors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alfine.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultTracker {
	private Path         successTrackerFile;
	private Path         failureTrackerFile;
	private Set<String>  successCacheLines;
	private Set<String>  failureCacheLines;
	private List<String> newSuccesses;
	private List<String> newFailures;
	
	public ResultTracker(Path successTrackerFile, Path failureTrackerFile) {
		this.successTrackerFile = successTrackerFile;
		this.failureTrackerFile = failureTrackerFile;
		this.successCacheLines  = loadEntriesFromFile(successTrackerFile).collect(Collectors.toSet());
		this.failureCacheLines  = loadEntriesFromFile(failureTrackerFile).collect(Collectors.toSet());
		this.newSuccesses       = new ArrayList<>();
		this.newFailures        = new ArrayList<>();
	}
	
	private static Stream<String> loadEntriesFromFile(Path path) {
		Stream<String> lines = java.util.stream.Stream.empty();
		try {
			lines = IOUtils.readLinesFromFile(path);
		} catch (Exception e) {
			Logger logger = LoggerFactory.getLogger(ResultTracker.class);
			logger.debug("Failed to open cache line tracker file, path = {}, exists = {}",
					path.toString(), Files.exists(path));
			e.printStackTrace();
		}
		return lines.filter(line -> !line.isEmpty());
	}
	
	public void putSuccess(String line) {
		if (!this.successCacheLines.contains(line)) {
			this.newSuccesses.add(line);
		}
	}
	
	public void putFailure(String line) {
		if (!this.failureCacheLines.contains(line)) {
			this.newFailures.add(line);
		}
	}
	
	public void update() {
		this.newSuccesses.forEach((line) -> {
			try {
				IOUtils.appendLineToFile(this.successTrackerFile, line);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		this.newFailures.forEach((line) -> {
			try {
				IOUtils.appendLineToFile(this.failureTrackerFile, line);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public boolean isKnownToFail(String cacheLine) {
		return this.failureCacheLines.contains(cacheLine);
	}

	public void put(String cacheLine, boolean success) {
		(success ? this.newSuccesses : this.newFailures).add(cacheLine);
	}
}
