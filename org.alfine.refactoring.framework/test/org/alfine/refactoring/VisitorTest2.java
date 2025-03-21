package org.alfine.refactoring;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
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
import org.alfine.refactoring.framework.launch.Main;
import org.alfine.refactoring.suppliers.HotMethodRefactoringSupplier;
import org.alfine.refactoring.suppliers.RefactoringOpportunityContext;
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

public class VisitorTest2 {

	private static Path getLocation() {
		return Paths.get(Platform.getInstanceLocation().getURL().getFile());
	}

	private static Path getCacheLocation() {
		return getLocation().resolve("oppcache");
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

	private static void configureWorkspaceConfig(String bm, String workspaceConfig) throws Exception {
		createAssetsLocation(bm);
		Files.write(getWorkspaceConfigPath(bm), workspaceConfig.getBytes(), StandardOpenOption.CREATE_NEW);
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
		configureWorkspaceConfig(bm, workspaceConfig);
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
			// "test" is used as bm-name in automated tests.
			FileUtils.deleteDirectory(getBmAssetsLocation("test").toFile());
		} catch (Exception e) {
			e.printStackTrace();
		}

		Path loc = getLocation();
		try (Stream<Path> paths = Files.list(loc)) {
			paths.forEach(path -> {
				if (!(path.endsWith("assets") || path.endsWith(".metadata"))) {
					System.out.println(String.format("Cleanup: %s", path));
					try {
						IWorkspace     workspace     = ResourcesPlugin.getWorkspace();
						IWorkspaceRoot workspaceRoot = workspace.getRoot();
						IProject       project       = workspaceRoot.getProject(path.getFileName().toString());
						project.delete(true, true, null);
					} catch (Exception e) {
						System.err.println(String.format("Exception thrown, trying to delete project: %s", path));
						e.printStackTrace();
					}
					if (!Files.exists(path)) {
						return;
					}
					try {
						if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
							FileUtils.deleteDirectory(path.toFile());
						} else {
							Files.delete(path);
						}
					} catch (Exception e) {
						System.err.println(String.format("Exception thrown, trying to delete file: %s", path));
						e.printStackTrace();
					}
				}
			});
		} catch (Exception e) {
			System.err.println("Exception during workspace cleanup.");
			e.printStackTrace();
		}
	}

	private Workspace getWorkspace(String[] args) {
		Workspace workspace = new Workspace(
			getDefaultWorkspaceConfiguration(args),
			true /* Create projects and cache opportunities. */
		);
		return workspace;
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
		return getWorkspace(args);
	}

	private Workspace getWorkspace(String bm, String compliance, String descriptor) {
		String[] args = new String[] {
			"--cache"     , "oppcache",
			"--compliance", compliance,
			"--src"       , "assets/" + bm + "/src",
			"--lib"       , "assets/" + bm + "/lib",
			"--out"       , "output",
			"--report"    , "report",
			"--descriptor", descriptor
		};
		try {
			Files.createDirectories(getLocation().resolve("report"));
			Files.createFile(getLocation().resolve("report/successTrackerFile.txt"));
			Files.createFile(getLocation().resolve("report/failureTrackerFile.txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return getWorkspace(args);
	}

	@Test
	public void test_1() throws Exception {
		TestBench.init();
		TestBench.methods(
			"t.X.f()",
			"t.Y.g()"
		);
		TestBench.src(
			new Archive("test.jar")
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
			""")
		);
		createTestWorkspace();
		TestBench.assertNRenameTypes(2);
		TestBench.assertNRenameMethods(1, "t.X");
		TestBench.assertNRenameMethods(1, "t.Y");
	}

	@Test
	public void test_2() throws Exception {
		TestBench.init();
		TestBench.methods(
			"t.X.f()"
		);
		TestBench.src(
			new Archive("test.jar").add("t/X.java", """
				package t;
				public class X {
					public void f() {}
					public void g() {}
				}
			""")
		);
		createTestWorkspace();
		TestBench.assertNRenameTypes(1);
		TestBench.assertNRenameMethods(1, "t.X");
	}

	@Test
	public void test_3() throws Exception {
		TestBench.init();
		TestBench.methods(
			"t.X.f()",
			"t.X.g(int)",
			"t.X.h(int, float, String)"
		);
		TestBench.src(
			new Archive("test.jar").add("t/X.java", """
				package t;
				public class X {
					public void f() {}
					public void g(int x) {}
					public void h(int x, float y, String z) {}
					public void i(int x, int y) {}
				}
			""")
		);
		createTestWorkspace();
		TestBench.assertNRenameMethodParam(0, "t.X.f()");
		TestBench.assertNRenameMethodParam(1, "t.X.g(int)");
		TestBench.assertNRenameMethodParam(3, "t.X.h(int, float, String)");
		TestBench.assertNRenameMethodParam(0, "t.X.i(int, int)");
	}

	@Test
	public void test_4() throws Exception {
		TestBench.init();
		TestBench.methods(
			"t.X.S.f(int, int)"
		);
		TestBench.src(
			new Archive("test.jar")
			.add("t/X.java", """
				package t;
				public class X {
				    static class S {
						public void f(int x) {}
						public void f(int x, int y) {}
					}
				}
			""")
		);
		createTestWorkspace();
		TestBench.assertNRenameTypes(2);
		TestBench.assertNRenameMethods(1, "t.X.S");
		TestBench.assertNRenameMethodParam(2, "t.X.S.f(int, int)");
	}

	@Test
	public void test_5() throws Exception {
		TestBench.init();
		TestBench.methods(
			"t.X.S.f(int)",
			"t.X.S.f(int, int)"
		);
		TestBench.src(
			new Archive("test.jar")
			.add("t/X.java", """
				package t;
				public class X {
				    static class S {
						public void f(int x) {}
						public void f(int x, int y) {}
					}
				}
			""")
		);
		createTestWorkspace();
		TestBench.assertNRenameTypes(2);
		TestBench.assertNRenameMethods(2, "t.X.S");
		TestBench.assertNRenameMethodParam(1, "t.X.S.f(int)");
		TestBench.assertNRenameMethodParam(2, "t.X.S.f(int, int)");
	}

// NOTE: We don't search TypeDeclaration in TypeDeclarationStatement (a type declared in a method body) at the moment.
//       In order to support it, we need to change how we toggle isCapture in visit(MethodDeclaration).
//       This is a case where a method becomes part of the opportunity context (which is handled already).
//
// TODO: I believe this works now except for the assertN functions which need to provide
//       String... variations to allow us to specify multiple methods in the context.
//
//	@Test
//	public void test_6() throws Exception {
//		TestBench.init();
//		TestBench.methods(
//			"t.X.f.Y.g(int)"
//		);
//		TestBench.src(
//			new Archive("test.jar")
//			.add("t/X.java", """
//				package t;
//				public class X {
//					public void f() {
//						class Y {
//							public void g(int x) {}
//						}
//					}
//				}
//			""")
//		);
//		createTestWorkspace();
//		TestBench.assertNRenameTypes(2);
//		TestBench.assertNRenameMethods(1, "t.X");
//		TestBench.assertNRenameMethods(1, "t.X.f.Y");
//		TestBench.assertNRenameMethodParam(0, "t.X.f()");
//		TestBench.assertNRenameMethodParam(2, "t.X.f.Y.g(int)");
//	}

	@Test
	public void test_7() throws Exception {
		TestBench.init();
		TestBench.methods(
			"t.X.f()"
		);
		TestBench.src(
			new Archive("test.jar")
			.add("t/X.java", """
				package t;
				public class X {
					public void f() {
						int x = 0;
					}
				}
			""")
		);
		createTestWorkspace();
		TestBench.assertNRenameTypes(1);
		TestBench.assertNRenameMethods(1, "t.X");
		TestBench.assertNRenameMethodParam(0, "t.X.f()");
		TestBench.assertNRenameMethodLocal(1, "t.X.f()");

		TestBench.assertNExtractConstant(1, "t.X.f()");
		TestBench.assertNExtractMethodBlocksOfSize(1, 1, "t.X.f()");
	}

	@Test
	public void test_8() throws Exception {
		TestBench.init();
		TestBench.methods(
			"t.X.f()"
		);
		TestBench.src(
			new Archive("test.jar")
			.add("t/X.java", """
				package t;
				public class X {
					private static final int x = 0;
					public int f() {
						return x * x;
					}
				}
			""")
		);
		createTestWorkspace();
		TestBench.assertNRenameTypes(1);
		TestBench.assertNRenameFields(1, "t.X");
		TestBench.assertNRenameMethods(1, "t.X");
		TestBench.assertNRenameMethodParam(0, "t.X.f()");
		TestBench.assertNRenameMethodLocal(0, "t.X.f()");

		TestBench.assertNInlineFields(2, "t.X.f()");
		TestBench.assertNExtractConstant(0, "t.X.f()");
		TestBench.assertNExtractMethodBlocksOfSize(1, 1, "t.X.f()");
	}

	@Test
	public void test_9() throws Exception {
		TestBench.init();
		TestBench.methods(
			"t.X.f()"
		);
		TestBench.src(
			new Archive("test.jar")
			.add("t/X.java", """
				package t;
				public class X {
					public static int i() {
						return 0;
					}
					public int f() {
						return i() * i();
					}
				}
			""")
		);
		createTestWorkspace();
		TestBench.assertNRenameTypes(1);
		TestBench.assertNRenameMethods(1, "t.X");
		TestBench.assertNInlineMethods(2, "t.X.f()");
		TestBench.assertNExtractConstant(0, "t.X.f()");
		TestBench.assertNExtractConstant(0, "t.X.i()"); // Because, i() is not hot.
		TestBench.assertNExtractMethodBlocksOfSize(1, 1, "t.X.f()");
		TestBench.assertNExtractMethodBlocksOfSize(0, 1, "t.X.i()"); // Because, i() is not hot.
	}

	@Test
	public void test_10() throws Exception {
		TestBench.init();
		TestBench.methods(
			"t.X.f()"
		);
		TestBench.src(
			new Archive("test.jar")
			.add("t/X.java", """
				package t;
				public class X<T> {
					public <S> S f() {
						return null;
					}
				}
			""")
		);
		createTestWorkspace();
		TestBench.assertNRenameTypes(1);
		TestBench.assertNRenameMethods(1, "t.X");
		TestBench.assertNExtractConstant(0, "t.X.f()"); // NullLiteral cannot be extracted as a constant.
		TestBench.assertNExtractMethodBlocksOfSize(1, 1, "t.X.f()");

		TestBench.assertNRenameTypeTypeParam(1, "t.X");
		TestBench.assertNRenameMethodTypeParam(1, "t.X.f()");
	}

	@Test
	public void test_11() throws Exception {
		TestBench.init();
		TestBench.methods(
			"t.X.f()"
		);
		TestBench.src(
			new Archive("test.jar")
			.add("t/X.java", """
				package t;
				public class X {
					public int f() {
						return 0;
					}
				}
			""")
		);
		String descriptor = "{\"args\":{\"element\":\"=test/test.jar.dir<t{X.java\",\"input\":\"=test/test.jar.dir<t{X.java\",\"name\":\"_x_\",\"replace\":\"false\",\"qualify\":\"false\",\"visibility\":\"2\",\"selection\":\"59 1\"},\"meta\":{\"id\":\"org.eclipse.jdt.ui.extract.constant\"}}";
		Workspace workspace = createTestWorkspace(descriptor);

		boolean success = Main.applyRefactoring(workspace.getConfiguration().getArguments());		
		if (!success) {
			printRefactoringOutput();
		}
		assertTrue(success);
	}

	private void printRefactoringOutput() throws Exception {
		System.err.println(String.join(System.getProperty("line.separator"), Files.lines(getLocation().resolve("report").resolve("refactoring-output.txt")).toList()));
	}

	@Test
	public void test_12() throws Exception {
		TestBench.init();
		TestBench.methods(
			"t.X.f()"
		);
		TestBench.src(
			new Archive("test.jar")
			.add("t/X.java", """
				package t;
				public class X {
					public int f() {
						return 0;
					}
				}
			""")
		);
		String descriptor = "{\"args\":{\"element\":\"=test/test.jar.dir<t{X.java\",\"input\":\"=test/test.jar.dir<t{X.java\",\"visibility\":\"2\",\"comments\":\"false\",\"replace\":\"true\",\"exceptions\":\"false\",\"name\":\"_x_\",\"selection\":\"52 9\"},\"meta\":{\"block_id\":\"47\",\"block_idx\":\"0\",\"block_size\":\"1\",\"id\":\"org.eclipse.jdt.ui.extract.method\"}}";
		Workspace workspace = createTestWorkspace(descriptor);

		boolean success = Main.applyRefactoring(workspace.getConfiguration().getArguments());		
		if (!success) {
			printRefactoringOutput();
		}
		assertTrue(success);
	}

	@Test
	public void test_13() throws Exception {
		TestBench.init();
		TestBench.methods(
			"t.X.f()"
		);
		TestBench.src(
			new Archive("test.jar")
			.add("t/X.java", """
				package t;
				public class X {
					private static final int x = 0;
					public int f() {
						return x;
					}
				}
			""")
		);
		String descriptor = "{\"args\":{\"element\":\"=test/test.jar.dir<t{X.java\",\"input\":\"=test/test.jar.dir<t{X.java\",\"selection\":\"93 1\",\"replace\":\"false\",\"remove\":\"false\"},\"meta\":{\"id\":\"org.eclipse.jdt.ui.inline.constant\"}}";
		Workspace workspace = createTestWorkspace(descriptor);

		assertTrue(Main.applyRefactoring(workspace.getConfiguration().getArguments()));
	}

	@Test
	public void test_14() throws Exception {
		TestBench.init();
		TestBench.methods(
			"t.X.f()"
		);
		TestBench.src(
			new Archive("test.jar")
			.add("t/X.java", """
				package t;
				public class X {
					public int f() {
						int x = 0;  // Inline temp works also when x is final.
						return  x;
					}
				}
			""")
		);
		String descriptor = "{\"args\":{\"input\":\"=test/test.jar.dir<t{X.java\",\"selection\":\"118 1\"},\"meta\":{\"id\":\"org.eclipse.jdt.ui.inline.temp\"}}";
		Workspace workspace = createTestWorkspace(descriptor);

		boolean success = Main.applyRefactoring(workspace.getConfiguration().getArguments());
		if (!success) {
			printRefactoringOutput();
		}
		assertTrue(success);
	}

	@Test
	public void test_15() throws Exception {
		TestBench.init();
		TestBench.methods(
			"t.X.f()"
		);
		TestBench.src(
			new Archive("test.jar")
			.add("t/X.java", """
				package t;
				public class X {
					public int f() {
						return  1 + 2;
					}
				}
			""")
		);
		String descriptor = "{\"args\":{\"input\":\"=test/test.jar.dir<t{X.java\",\"name\":\"_x_\",\"replace\":\"true\",\"replaceAllInThisFile\":\"false\",\"final\":\"false\",\"varType\":\"false\",\"selection\":\"60 5\"},\"meta\":{\"id\":\"org.eclipse.jdt.ui.extract.temp\"}}";
		Workspace workspace = createTestWorkspace(descriptor);

		boolean success = Main.applyRefactoring(workspace.getConfiguration().getArguments());
		if (!success) {
			printRefactoringOutput();
		}
		assertTrue(success);
	}

	@Test
	public void test_16() throws Exception {
		TestBench.init();
		TestBench.methods(
			"t.X.f()"
		);
		TestBench.src(
			new Archive("test.jar")
			.add("t/X.java", """
				package t;
				public class X {
					public int g() {
						return f();   // Should introduce indirection to f().
					}
					public int f() {
						return 7;
					}
				}
			""")
		);
		String descriptor = "{\"args\":{\"element1\":\"=test/test.jar.dir<t{X.java[X\",\"input\":\"=test/test.jar.dir<t{X.java[X~f\",\"name\":\"_x_\",\"references\":\"true\"},\"meta\":{\"id\":\"org.eclipse.jdt.ui.introduce.indirection\"}}";
		Workspace workspace = createTestWorkspace(descriptor);

		boolean success = Main.applyRefactoring(workspace.getConfiguration().getArguments());
		if (!success) {
			printRefactoringOutput();
		}
		assertTrue(success);
	}

	@Test
	public void test_17() throws Exception {
		TestBench.init();
		TestBench.methods(
			"t.X.f()"
		);
		TestBench.src(
			new Archive("test.jar")
			.add("t/X.java", """
				package t;
				public class X {
					public int f() {
						return Integer.MAX_VALUE;
					}
				}
			""")
		);
		createTestWorkspace();
		TestBench.assertNExtractConstant(0, "t.X.f()"); // Integer.MAX_VALUE is a binary reference and should be skipped.
		TestBench.assertNInlineFields(0, "t.X.f()");
	}

	@Test
	public void test_18() throws Exception {
		TestBench.init();
		TestBench.methods(
			"t.X.f()"
		);
		TestBench.src(
			new Archive("test.jar")
			.add("t/X.java", """
				package t;
				import java.lang.Math;
				public class X {
					public int f() {
						return Math.abs(7); // Should not attempt to inline binary methods.
					}
				}
			""")
		);
		createTestWorkspace();
		TestBench.assertNInlineMethods(0, "t.X.f()");
		TestBench.assertNMethodIndirections(1, "t.X.f()"); // The occurrence is for f().
	}

	private Workspace createTestWorkspace(String descriptor) {
		Workspace workspace = getWorkspace("test", "1.8", descriptor);
		HotMethodRefactoringSupplier supplier = new HotMethodRefactoringSupplier(workspace);
		supplier.cacheOpportunities();
		return workspace;
	}

	private Workspace createTestWorkspace() {
		Workspace workspace = getWorkspace("test", "1.8");
		HotMethodRefactoringSupplier supplier = new HotMethodRefactoringSupplier(workspace);
		supplier.cacheOpportunities();
		return workspace;
	}

	private static class TestBench {
		private TestBench() {}

		public static void init() throws Exception {
			configureWorkspaceConfig("test",
"""
test {
    src / test.jar
}
"""
			);
			configureVariable("test", Arrays.asList("test.jar"));
		}

		public static void methods(String... methods) throws Exception {
			configureMethods("test", Arrays.asList(methods));
		}

		public static void src(Archive... archives) throws Exception {
			Path src = getSrcLocation("test");
			for (Archive archive : archives) {
				archive.writeSourceArchive(src);
			}
		}

		public static void lib(Archive... archives) throws Exception {
			Path lib = getLibLocation("test");
			for (Archive archive : archives) {
				archive.writeBinaryArchive(lib);
			}
		}

		public static void assertNLines(int n, Path p) throws Exception {
			assertEquals(n > 0, Files.exists(p));
			if (n > 0) {
				assertEquals(n, Files.readAllLines(p).size());
			}
		}

		private static String classPath(String qName) {
			return qName.replace(".", "/");
		}

		private static String methodPath(String qNameWithSignature) {
			return RefactoringOpportunityContext.getMethodContext(qNameWithSignature).toString();
		}

		public static void assertNRenameTypes(int n) throws Exception {
			assertNLines(n, getCacheLocation().resolve("rename/type/descriptors.txt"));
		}

		public static void assertNRenameFields(int n, String qClassName) throws Exception {
			assertNLines(n, getCacheLocation().resolve("rename/field").resolve(classPath(qClassName)).resolve("descriptors.txt"));
		}

		public static void assertNRenameMethods(int n, String qName) throws Exception {
			assertNLines(n, getCacheLocation().resolve("rename/method").resolve(classPath(qName)).resolve("descriptors.txt"));
		}

		public static void assertNRenameMethodParam(int n, String qNameWithSignature) throws Exception {
			assertNLines(n, getCacheLocation().resolve("rename/param").resolve(methodPath(qNameWithSignature)).resolve("descriptors.txt"));
		}

		public static void assertNRenameMethodLocal(int n, String qNameWithSignature) throws Exception {
			assertNLines(n, getCacheLocation().resolve("rename/local").resolve(methodPath(qNameWithSignature)).resolve("descriptors.txt"));
		}

		public static void assertNRenameTypeTypeParam(int n, String qClassName) throws Exception {
			assertNLines(n, getCacheLocation().resolve("rename/type-type-param").resolve(classPath(qClassName)).resolve("descriptors.txt"));
		}

		public static void assertNRenameMethodTypeParam(int n, String qMethodWithSignature) throws Exception {
			assertNLines(n, getCacheLocation().resolve("rename/method-type-param").resolve(methodPath(qMethodWithSignature)).resolve("descriptors.txt"));
		}

		public static void assertNExtractConstant(int n, String qNameWithSignature) throws Exception {
			assertNLines(n, getCacheLocation().resolve("x-constant").resolve(methodPath(qNameWithSignature)).resolve("descriptors.txt"));
		}

		public static void assertNExtractMethodBlocksOfSize(int n, int size, String qNameWithSignature) throws Exception {
			assertNLines(n, getCacheLocation().resolve("x-method").resolve(methodPath(qNameWithSignature)).resolve(String.valueOf(size)).resolve("descriptors.txt"));
		}

		public static void assertNInlineFields(int n, String qMethodWithSignature) throws Exception {
			assertNLines(n, getCacheLocation().resolve("i-constant").resolve(methodPath(qMethodWithSignature)).resolve("descriptors.txt"));
		}

		public static void assertNInlineMethods(int n, String qMethodWithSignature) throws Exception {
			assertNLines(n, getCacheLocation().resolve("i-method").resolve(methodPath(qMethodWithSignature)).resolve("descriptors.txt"));
		}

		public static void assertNMethodIndirections(int n, String qMethodWithSignature) throws Exception {
			assertNLines(n, getCacheLocation().resolve("method-indirection").resolve(methodPath(qMethodWithSignature)).resolve("descriptors.txt"));
		}
	}
}
