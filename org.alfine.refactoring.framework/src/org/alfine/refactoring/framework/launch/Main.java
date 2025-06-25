package org.alfine.refactoring.framework.launch;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.framework.WorkspaceConfiguration;
import org.alfine.refactoring.processors.Processor;
import org.alfine.refactoring.suppliers.HotMethodRefactoringFinder;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jdt.core.JavaCore;

public class Main implements IApplication {
	@Override
	public Object start(IApplicationContext context) throws Exception {
		
		try {
			IWorkspace ws = ResourcesPlugin.getWorkspace();
			IWorkspaceDescription desc = ws.getDescription();
			desc.setAutoBuilding(false);
			desc.setMaxConcurrentBuilds(1);
			ws.setDescription(desc);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
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

	public static boolean applyRefactoring(CommandLineArguments arguments) throws Exception {

		final String descriptor = arguments.getRefactoringDescriptor();
		if (descriptor == null) {
			throw new Exception("Please specify a refactoring descriptor using the appropriate command line switch.");
		}

		System.out.println("Using descriptor = " + descriptor);

		// Assume that the workspace is already setup by a previous invocation to "prepare" the workspace.

		Workspace workspace    = new Workspace(new WorkspaceConfiguration(arguments), false);
		Path      location     = Paths.get(Platform.getInstanceLocation().getURL().getFile());
		Path      reportFolder = location.resolve("report");

		boolean success = Processor.refactor(descriptor, reportFolder);

		workspace.close(success);

		return success;
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

		if (WorkspaceConfiguration.hasMethodsConfig()) {
			// Cache opportunities based on a list of method signatures.
			// Originally intended for hot methods, but can be applied to any available function.
			new HotMethodRefactoringFinder(workspace).cacheOpportunities();
		} else {
			throw new Exception("No opportunities cached. Please specify methods configuration.");
		}
	}

	@Override
	public void stop() {
		// nothing to do
	}
}
