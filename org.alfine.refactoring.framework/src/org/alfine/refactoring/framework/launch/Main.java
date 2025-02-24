package org.alfine.refactoring.framework.launch;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.framework.WorkspaceConfiguration;
import org.alfine.refactoring.framework.launch.CommandLineArguments.RefactoringType;
import org.alfine.refactoring.processors.RefactoringProcessor;
import org.alfine.refactoring.processors.ResultTracker;
import org.alfine.refactoring.suppliers.HotMethodRefactoringSupplier;
import org.alfine.refactoring.suppliers.RandomExtractConstantFieldSupplier;
import org.alfine.refactoring.suppliers.RandomExtractMethodSupplier;
import org.alfine.refactoring.suppliers.RandomInlineConstantFieldSupplier;
import org.alfine.refactoring.suppliers.RandomInlineMethodSupplier;
import org.alfine.refactoring.suppliers.RandomRenameSupplier;
import org.alfine.refactoring.suppliers.RefactoringDescriptorFactory;
import org.alfine.refactoring.suppliers.RefactoringSupplier;
import org.alfine.refactoring.suppliers.SingleRefactoringSupplier;
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

		boolean prepareWorkspaceAndCacheOpportunities = arguments.getPrepare();
		
		Hashtable<String, String> options = JavaCore.getDefaultOptions();
		JavaCore.setComplianceOptions(arguments.getCompilerComplianceVersion(), options);
		JavaCore.setOptions(options);

		Workspace workspace = new Workspace(
			new WorkspaceConfiguration(arguments),
			prepareWorkspaceAndCacheOpportunities /* If true, workspace is set up and refactoring opportunities written to file. */
		);

		// The `packages.config` is a Java properties file where each key in
		// the map is a project name and where the value is a list of package
		// names that should be open for transformation.
		//
		// The `packages.config.helper` file written here is for showing
		// the user what package fragment options are available.
		//
		// The user is expected to update the `packages.config' file to
		// whatever is appropriate for the intended experiment.
		Path packagesConfigHelperPath = workspace.getConfiguration().getSrcPath().resolve("packages.config.helper");

		workspace.writePackagesConfigHelper(packagesConfigHelperPath);

		if (prepareWorkspaceAndCacheOpportunities) {
			new RandomRenameSupplier(workspace).cacheOpportunities();
			new RandomInlineMethodSupplier(workspace).cacheOpportunities();
		    new RandomExtractMethodSupplier(workspace).cacheOpportunities();
		    new RandomInlineConstantFieldSupplier(workspace).cacheOpportunities();
			new RandomExtractConstantFieldSupplier(workspace).cacheOpportunities();

			if (WorkspaceConfiguration.hasMethodsConfig()) {
				new HotMethodRefactoringSupplier(workspace).cacheOpportunities();
			}

			return IApplication.EXIT_OK;
		}

		int     drop             = arguments.getDrop();         // Drop the first n refactorings in the supplier stream. 
		int     limit            = arguments.getLimit();        // Number of refactoring attempts before we give up.
		long    shuffleSeed      = arguments.getShuffleSeed();    // Seed passed to Random instance used for shuffling opportunities.
		long    selectSeed       = arguments.getSelectSeed();   // Seed passed to supply iterator used for selecting next opportunity.

		RefactoringType type     = arguments.getRefactoring(); // Refactoring type.
		long            seed     = arguments.getSeed();        // Number generator seed.
		int             offset   = arguments.getOffset();       // Number generator initial offset.
		int             length   = arguments.getLength();      // Rename symbol max length. (In case of a rename refactoring.)
		boolean         fixed    = arguments.getFixed();       // Whether length of generated symbols is fixed or random.

		RefactoringSupplier supplier  = null;

		switch (type) {
		case NONE:

			// NOTE: The user is supplying a specific refactoring descriptor.

			final String descriptor = arguments.getRefactoringDescriptor();
			if (descriptor != null) {
				supplier = new SingleRefactoringSupplier(RefactoringDescriptorFactory.get(descriptor));
			}
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
				logger.error("Missing command-line option 'length' (-l) for rename refactorings.");
			}

			RandomRenameSupplier renameSupplier =
				new RandomRenameSupplier(workspace, seed, offset);

			renameSupplier.setMaxLength(length);
			renameSupplier.setLengthFixed(fixed); // TODO: Add as a command line option.

			supplier = renameSupplier;
			break;
		default:
			logger.error("Unknown refactoring type.");
		}

		supplier.setShuffleSeed(shuffleSeed);
		supplier.setSelectSeed(selectSeed);

		// `reportFolder` is an independent folder into which we write refactoring
		// output reports and keep track of which refactorings have succeeded and
		// which have failed.

		String refactoringOutputReportFolder = arguments.getRefactoringOutputReportFolder();

		Path reportFolder       = Paths.get(refactoringOutputReportFolder);
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
