package org.alfine.refactoring.framework.launch;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.framework.WorkspaceConfiguration;
import org.alfine.refactoring.processors.RefactoringProcessor;
import org.alfine.refactoring.processors.ResultTracker;
import org.alfine.refactoring.suppliers.HotMethodRefactoringSupplier;
import org.alfine.refactoring.suppliers.RandomExtractConstantFieldSupplier;
import org.alfine.refactoring.suppliers.RandomExtractMethodSupplier;
import org.alfine.refactoring.suppliers.RandomInlineConstantFieldSupplier;
import org.alfine.refactoring.suppliers.RandomInlineMethodSupplier;
import org.alfine.refactoring.suppliers.RandomRenameSupplier;
import org.alfine.refactoring.suppliers.RefactoringDescriptor;
import org.alfine.refactoring.suppliers.RefactoringDescriptorFactory;
import org.alfine.refactoring.suppliers.RefactoringSupplier;
import org.alfine.refactoring.suppliers.SingleRefactoringSupplier;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jdt.core.JavaCore;

public class Main implements IApplication {
	@Override
	public Object start(IApplicationContext context) throws Exception {
		String[]             args      = (String [])context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		CommandLineArguments arguments = new CommandLineArguments(args);
		if (arguments.getPrepare()) {
			prepareWorkspace(arguments);
			return IApplication.EXIT_OK;
		} else {
			applyRefactoring(arguments);
			return IApplication.EXIT_OK;
		}
	}

	private void applyRefactoring(CommandLineArguments arguments) throws Exception {

		final String descriptor = arguments.getRefactoringDescriptor();
		if (descriptor == null) {
			throw new Exception("Please specify a refactoring descriptor using the appropriate command line switch.");
		}

		RefactoringDescriptor refactoringDescriptor = RefactoringDescriptorFactory.get(descriptor);

		// Assume that the workspace is already setup by a previous invocation to "prepare" the workspace.

		Workspace           workspace = new Workspace(new WorkspaceConfiguration(arguments), false);
		RefactoringSupplier supplier  = new SingleRefactoringSupplier(refactoringDescriptor);

		// `reportFolder` is an independent folder into which we write refactoring
		// output reports and keep track of which refactorings have succeeded and
		// which have failed.

		String refactoringOutputReportFolder = arguments.getRefactoringOutputReportFolder();

		Path reportFolder       = Paths.get(refactoringOutputReportFolder);
		Path successTrackerFile = reportFolder.resolve("successTrackerFile.txt");
		Path failureTrackerFile = reportFolder.resolve("failureTrackerFile.txt");

		ResultTracker resultTracker = new ResultTracker(successTrackerFile, failureTrackerFile);

		boolean success = new RefactoringProcessor(supplier, resultTracker, reportFolder).processSupply(0, 0);

		workspace.close(success);
	}

	private void prepareWorkspace(CommandLineArguments arguments) throws Exception {
		final String compliance = arguments.getCompilerComplianceVersion();
		if (compliance == null) {
			throw new Exception("Please specify compiler compliance using the appropriate command line switch.");
		}
		Hashtable<String, String> options = JavaCore.getDefaultOptions();
		JavaCore.setComplianceOptions(arguments.getCompilerComplianceVersion(), options);
		JavaCore.setOptions(options);

		Workspace workspace = new Workspace(new WorkspaceConfiguration(arguments), true);

		// The `packages.config' file is a Java properties file mapping project
		// names to package names. The user should can configure this file
		// to specify to the framework which packages are open for
		// transformation.
		//
		// The `units.config' file is a Java properties file mapping
		// project names to compilation unit names. The user should configure
		// this file to specify to the framework which units are open for
		// transformation.
		//
		// The `packages.config.helper` file written here is for showing
		// the user what package fragment options are available.
		//
		// This is only relevant for collecting opportunities.
		//
		Path packagesConfigHelperPath = workspace.getConfiguration().getSrcPath().resolve("packages.config.helper");

		workspace.writePackagesConfigHelper(packagesConfigHelperPath);

		if (WorkspaceConfiguration.hasPackagesConfig()) {
			// Cache opportunities by scanning packages open for transformation based on `package.config'.
			new RandomRenameSupplier(workspace).cacheOpportunities();
			new RandomInlineMethodSupplier(workspace).cacheOpportunities();
		    new RandomExtractMethodSupplier(workspace).cacheOpportunities();
		    new RandomInlineConstantFieldSupplier(workspace).cacheOpportunities();
			new RandomExtractConstantFieldSupplier(workspace).cacheOpportunities();
		}

		if (WorkspaceConfiguration.hasMethodsConfig()) {
			// Cache opportunities based on a list of method signatures.
			// Originally intended for hot methods, but can be applied to any available function.
			new HotMethodRefactoringSupplier(workspace).cacheOpportunities();
		}

		if (!(WorkspaceConfiguration.hasPackagesConfig() || WorkspaceConfiguration.hasMethodsConfig())) {
			throw new Exception("No opportunities cached. Please specify packages and compilation units configuration, or methods configuration, or both.");
		}
	}

	@Override
	public void stop() {
		// nothing to do
	}
}
