package org.alfine.refactoring.framework.launch;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.framework.WorkspaceConfiguration;
import org.alfine.refactoring.framework.launch.CommandLineArguments.RefactoringType;
import org.alfine.refactoring.processors.RefactoringProcessor;
import org.alfine.refactoring.processors.ResultTracker;
import org.alfine.refactoring.suppliers.RandomExtractConstantFieldSupplier;
import org.alfine.refactoring.suppliers.RandomExtractMethodSupplier;
import org.alfine.refactoring.suppliers.RandomInlineConstantFieldSupplier;
import org.alfine.refactoring.suppliers.RandomInlineMethodSupplier;
import org.alfine.refactoring.suppliers.RandomRenameSupplier;
import org.alfine.refactoring.suppliers.RefactoringSupplier;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jdt.core.JavaCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements IApplication {
	@Override
	public Object start(IApplicationContext context) throws Exception {

		String[]             args      = null;
		CommandLineArguments arguments = null;

		Logger logger = LoggerFactory.getLogger(Main.class);

		args      = (String [])context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		arguments = new CommandLineArguments(args);

		boolean prepareWorkspace = arguments.getPrepare();      // Setup workspace and cache opportunities then exit if true.
		String  cacheFolder      = arguments.getCacheFolder();  // Location of project specific cache files.
		String  srcFolder        = arguments.getSrcFolder();    // Location of source archives to be imported.
		String  libFolder        = arguments.getLibFolder();    // Location of binary archives to be imported.
		String  outputFolder     = arguments.getOutputFolder(); // Output folder for source archives on success.
		String  refactoringOutputReportFolder = arguments.getRefactoringOutputReportFolder(); // Folder into which we write refactoring report files.
		
		String alfineRT = arguments.getAlfineRT();
		Workspace.RT    = alfineRT;
		// System.getProperties().putIfAbsent(org.alfine.refactoring.framework.JavaProject.ALFINE_RT, alfineRT);
		
		//boolean verbose          = arguments.getVerbose();      // Execute with extra console output (mostly for debugging).

		// https://help.eclipse.org/2019-09/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Fguide%2Fjdt_api_options.htm&anchor=builder
//		JavaCore.getDefaultOptions().put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "1.8");
//		JavaCore.getDefaultOptions().put(JavaCore.COMPILER_COMPLIANCE, "1.8");
//		JavaCore.getDefaultOptions().put(JavaCore.compli.COMPILER_COMPLIANCE, "1.8");
		
		Hashtable<String, String> options = JavaCore.getDefaultOptions();
		JavaCore.setComplianceOptions("1.8", options);
		JavaCore.setOptions(options);
		
		int     drop             = arguments.getDrop();         // Drop the first n refactorings in the supplier stream. 
		int     limit            = arguments.getLimit();        // Number of refactoring attempts before we give up.
		long    shuffleSeed        = arguments.getShuffleSeed();    // Seed passed to Random instance used for shuffling opportunities.
		long    selectSeed       = arguments.getSelectSeed();   // Seed passed to supply iterator used for selecting next opportunity.

		RefactoringType type     = arguments.getRefactoring(); // Refactoring type.
		long            seed     = arguments.getSeed();        // Number generator seed.
		int             offset    = arguments.getOffset();       // Number generator initial offset.
		int             length   = arguments.getLength();      // Rename symbol max length. (In case of a rename refactoring.)
		boolean         fixed     = arguments.getFixed();       // Whether length of generated symbols is fixed or random.

		String location = Platform.getInstanceLocation().getURL().getFile();
		logger.info("Location: {}", location);

		Path locationPath    = Paths.get(location);
		Path srcFolderPath   = locationPath.resolve(srcFolder);
		Path libFolderPath   = locationPath.resolve(libFolder);
		Path outFolderPath   = locationPath.resolve(outputFolder);
		Path cacheFolderPath = locationPath.resolve(cacheFolder);

		// The `packages.config` is a Java properties file mapping project
		// names to list of package names open for transformation.
		
		Workspace workspace = new Workspace(
			new WorkspaceConfiguration(
				locationPath,
				srcFolderPath,
				libFolderPath,
				srcFolderPath.resolve("workspace.config"),
				srcFolderPath.resolve("variable.config"),
				srcFolderPath.resolve("packages.config"),
				srcFolderPath.resolve("units.config"),
				srcFolderPath.resolve("methods.config")
			),
			srcFolderPath,
			libFolderPath,
			outFolderPath,
			prepareWorkspace, /* If true, workspace is set up and refactoring opportunities written to file. */
			cacheFolderPath /* `RefactoringDescriptor` cache in workspace. */
		);
		
		// Write a helper `packages.config` to the source directory.
		String packagesConfigHelperFileName = "packages.config.helper";
		Path   packagesConfigHelperPath     = srcFolderPath.resolve(packagesConfigHelperFileName);

		workspace.writePackagesConfigHelper(packagesConfigHelperPath);
		
		if (prepareWorkspace) {
			new RandomRenameSupplier(workspace).cacheOpportunities();
			new RandomInlineMethodSupplier(workspace).cacheOpportunities();
		    new RandomExtractMethodSupplier(workspace).cacheOpportunities();
		    new RandomInlineConstantFieldSupplier(workspace).cacheOpportunities();
			new RandomExtractConstantFieldSupplier(workspace).cacheOpportunities();

			return IApplication.EXIT_OK;
		}

		RefactoringSupplier supplier  = null;

		switch (type) {
		case NONE:
			break;
		case INLINE_CONSTANT:
			supplier = new RandomInlineConstantFieldSupplier(workspace);
			break;
		case EXTRACT_CONSTANT:
			supplier = new RandomExtractConstantFieldSupplier(workspace);
			break;
		case EXTRACT_METHOD:
			supplier = new RandomExtractMethodSupplier(workspace);
			break;
		case INLINE_METHOD:
			supplier = new RandomInlineMethodSupplier(workspace);
			break;
		case RENAME:

			if (!arguments.hasOption("length")) {
				System.out.println("Missing command-line option 'length' (-l) for rename refactorings.");
			}

			RandomRenameSupplier renameSupplier =
				new RandomRenameSupplier(workspace, seed, offset);

			renameSupplier.setMaxLength(length);
			renameSupplier.setLengthFixed(fixed); // TODO: Add as a command line option.

			supplier = renameSupplier;
			break;
		default:
			System.out.println("Unknown refactoring type.");
		}

		supplier.setShuffleSeed(shuffleSeed);
		supplier.setSelectSeed(selectSeed);

		// `reportFolder` is an independent folder into which we write refactoring
		// output reports and keep track of which refactorings have succeeded and
		// which have failed.
		
		Path reportFolder = Paths.get(refactoringOutputReportFolder);
		Path successTrackerFile = reportFolder.resolve("successTrackerFile.txt");
		Path failureTrackerFile = reportFolder.resolve("failureTrackerFile.txt");

		ResultTracker resultTracker = new ResultTracker(successTrackerFile, failureTrackerFile);
		
		boolean success = new RefactoringProcessor(supplier, resultTracker, reportFolder).processSupply(drop, limit);

		workspace.close(success);

		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		// nothing to do
	}
}
