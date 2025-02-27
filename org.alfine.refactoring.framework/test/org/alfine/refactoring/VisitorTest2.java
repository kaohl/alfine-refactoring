package org.alfine.refactoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Stream;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.framework.WorkspaceConfiguration;
import org.alfine.refactoring.framework.launch.CommandLineArguments;
import org.alfine.refactoring.suppliers.HotMethodRefactoringSupplier;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.JavaCore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VisitorTest2 {

	Logger logger = LoggerFactory.getLogger(VisitorTest.class);
	
	private static Path getLocation() {
		return Paths.get(Platform.getInstanceLocation().getURL().getFile());
	}

	private static Path getBmAssetsLocation(String bm) {
		return getLocation().resolve("assets").resolve(bm);
	}

	private static Path getSrcLocation(String bm) {
		return getBmAssetsLocation(bm).resolve("src");
	}

	private static Path getLibLocation(String bm) {
		return getBmAssetsLocation(bm).resolve("lib");
	}

	private static Path getWorkspaceConfigPath(String bm) {
		return getSrcLocation(bm).resolve("workspace.config");
	}

	private static Path getMethodsConfigPath(String bm) {
		return getSrcLocation(bm).resolve("methods.config");
	}

	private static Path getVariableConfigPath(String bm) {
		return getSrcLocation(bm).resolve("variable.config");
	}

	private static WorkspaceConfiguration getDefaultWorkspaceConfiguration(String[] args) {
		CommandLineArguments arguments = new CommandLineArguments(args);

		// Set default JavaCore compiler compliance. (TODO: Evaluate whether this is needed now that we set the compiler compliance on all projects.)
		Hashtable<String, String> options = JavaCore.getDefaultOptions();
		JavaCore.setComplianceOptions(arguments.getCompilerComplianceVersion(), options);
		JavaCore.setOptions(options);

		return new WorkspaceConfiguration(arguments);
	}

	private static void createAssetsLocation(String bm) throws Exception {
		if (!Files.exists(getSrcLocation(bm))) {
			Files.createDirectories(getSrcLocation(bm));
		}
		if (!Files.exists(getLibLocation(bm))) {
			Files.createDirectories(getLibLocation(bm));
		}
	}

	private static void configureMethods(String bm, List<String> methods) throws Exception {
		createAssetsLocation(bm);
		Files.deleteIfExists(getMethodsConfigPath(bm));
		Files.write(getMethodsConfigPath(bm), methods, StandardOpenOption.CREATE_NEW);
	}

	private static void configureSources(String bm, List<Archive> archives, List<Archive> libraries, String workspaceConfig) throws Exception {
		createAssetsLocation(bm);
		Path src = getSrcLocation(bm);
		for (Archive archive : archives) {
			archive.writeSourceArchive(src);
		}
		Path lib = getLibLocation(bm);
		for (Archive archive : libraries) {
			archive.writeBinaryArchive(lib);
		}
		Files.write(getWorkspaceConfigPath(bm), workspaceConfig.getBytes(), StandardOpenOption.CREATE_NEW);
	}

	private static void configureVariable(String bm, List<String> variables) throws Exception {
		createAssetsLocation(bm);
		Files.write(getVariableConfigPath(bm), String.join(System.getProperty("line.separator"), variables).getBytes(), StandardOpenOption.CREATE_NEW);
	}

	@BeforeEach
	void beforeEach() {
		clean();
	}

	@AfterEach
	void afterEach() {
		//clean();
	}

	void clean() {
		try {
			// "test" is used as <bm> in automated tests.
			FileUtils.deleteDirectory(getBmAssetsLocation("test").toFile());
		} catch (Exception e) {
			e.printStackTrace();
		}

		Path loc = getLocation();
		try (Stream<Path> paths = Files.list(loc)) {
			paths.forEach(path -> {
				if (!(path.endsWith("assets") || path.endsWith(".metadata"))) {
					logger.debug("Cleanup: {}", path);
					try {
						IWorkspace     workspace     = ResourcesPlugin.getWorkspace();
						IWorkspaceRoot workspaceRoot = workspace.getRoot();
						IProject       project       = workspaceRoot.getProject(path.getFileName().toString());
						project.delete(true, true, null);
					} catch (Exception e) {
						try {
							if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
								FileUtils.deleteDirectory(path.toFile());
							} else {
								Files.delete(path);
							}
						} catch (Exception e2) {
							logger.error(String.format("Exception thrown, trying to delete project: {}", path), e);
							logger.error(String.format("Exception thrown, trying to delete file: {}", path), e2);
						}
					}
				}
			});
		} catch (Exception e) {
			logger.error("Exception during workspace cleanup.", e);
		}
	}

	private Workspace getWorkspace(String bm, String compliance) {
		String[] args = new String[] {
			"--cache"     , "oppcache",
			"--compliance", compliance,
			"--src"       , "assets/" + bm + "/src",
			"--lib"       , "assets/" + bm + "/lib",
			"--out"       , "output",
			"--report"    , "report"
		};

		Workspace workspace = new Workspace(
			getDefaultWorkspaceConfiguration(args),
			true  /* Create projects and cache opportunities. */
		);
		return workspace;
	}

	@Test
	public void test_1() throws Exception {
		Archive archive_1 = new Archive("test.jar")
		.add("t/X.java", """
			package t;
			public class X {
				public void f() {}
			}
		""")
		.add("t/Y.java", """
			package t;
			public class Y {
				public void g() {}
			}
		""");
		configureMethods("test", Arrays.asList(
			"t.X.f()",
			"t.Y.g()"
		));
		configureSources("test", Arrays.asList(archive_1), Arrays.asList(), """
			test {
				src / test.jar
			}
		""");
		configureVariable("test", Arrays.asList(
			"test.jar"
		));

		Workspace workspace = getWorkspace("test", "1.8");

		HotMethodRefactoringSupplier supplier = new HotMethodRefactoringSupplier(workspace);
		supplier.cacheOpportunities();

		assertTrue(Files.exists(getLocation().resolve("oppcache/rename/type/descriptors.txt")));
		assertTrue(Files.exists(getLocation().resolve("oppcache/rename/method/t/X/descriptors.txt"))); // TODO: Join context parts with "."?
		assertTrue(Files.exists(getLocation().resolve("oppcache/rename/method/t/Y/descriptors.txt")));

		// TODO: Why are these not refreshed between invocations? (Doesn't clean() run?)
		assertEquals(1, Files.readAllLines(getLocation().resolve("oppcache/rename/method/t/X/descriptors.txt")).size());
		assertEquals(1, Files.readAllLines(getLocation().resolve("oppcache/rename/method/t/Y/descriptors.txt")).size());
	}
}
