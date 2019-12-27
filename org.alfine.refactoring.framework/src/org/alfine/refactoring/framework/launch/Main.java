package org.alfine.refactoring.framework.launch;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.framework.WorkspaceConfiguration;
import org.alfine.refactoring.framework.launch.CommandLineArguments.RefactoringType;
import org.alfine.refactoring.processors.RefactoringProcessor;
import org.alfine.refactoring.suppliers.RandomExtractConstantSupplier;
import org.alfine.refactoring.suppliers.RandomExtractMethodSupplier;
import org.alfine.refactoring.suppliers.RandomInlineConstantFieldSupplier;
import org.alfine.refactoring.suppliers.RandomInlineMethodSupplier;
import org.alfine.refactoring.suppliers.RandomRenameSupplier;
import org.alfine.refactoring.suppliers.RefactoringSupplier;
import org.alfine.refactoring.utils.Generator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

/**
 * This class controls all aspects of the application's execution
 */
public class Main implements IApplication {

	public static final String LOGFILE_KEY = "alfine.refactoring.stdlog";

	@Override
	public Object start(IApplicationContext context) throws Exception {

		String[]             args      = null;
		CommandLineArguments arguments = null;

		args      = (String [])context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		arguments = new CommandLineArguments(args);

		boolean prepareWorkspace = arguments.getPrepare();      // Setup workspace and cache opportunities then exit if true.
		String  srcFolder        = arguments.getSrcFolder();    // Location of source archives to be imported.
		String  libFolder        = arguments.getLibFolder();    // Location of binary archives to be imported.
		String  outputFolder     = arguments.getOutputFolder(); // Output folder for source archives on success.
		boolean verbose          = arguments.getVerbose();      // Execute with extra console output (mostly for debugging).
		int     drop             = arguments.getDrop();         // Drop the first n refactorings in the supplier stream. 
		int     limit            = arguments.getLimit();        // Number of refactoring attempts before we give up.
		long    shuffleSeed        = arguments.getShuffleSeed();    // Seed passed to Random instance used for shuffling opportunities.
		long    selectSeed       = arguments.getSelectSeed();   // Seed passed to supply iterator used for selecting next opportunity.

		RefactoringType type     = arguments.getRefactoring(); // Refactoring type.
		long            seed     = arguments.getSeed();        // Number generator seed.
		int             offset    = arguments.getOffset();       // Number generator initial offset.
		int             length   = arguments.getLength();      // Rename symbol max length. (In case of a rename refactoring.)
		boolean         fixed     = arguments.getFixed();       // Whether length of generated symbols is fixed or random.

		Path logFilePath = Paths.get("refactoring-output.log");
		System.setProperty(Main.LOGFILE_KEY, logFilePath.toString());

		String location = Platform.getInstanceLocation().getURL().getFile();

		System.out.println("Location = " + location);

		Path locationPath = Paths.get(location);
		Path srcFolderPath = locationPath.resolve(srcFolder);
		Path libFolderPath = locationPath.resolve(libFolder);
		Path outFolderPath = locationPath.resolve(outputFolder);

		Workspace workspace = new Workspace(
			new WorkspaceConfiguration(
				locationPath,
				srcFolderPath,
				libFolderPath,
				srcFolderPath.resolve("workspace.config"),
				srcFolderPath.resolve("variable.config")
			),
			srcFolderPath,
			libFolderPath,
			outFolderPath,
			prepareWorkspace /* If true, workspace is set up and refactoring opportunities written to file. */
		);

		if (prepareWorkspace) {
			// 
			// 1. Refactoring Opportunity Type Generator (visitor) (code selector)
			// 2. Refactoring Opportunity selector and instantiation.
		}

		// TODO: Consider representing refactoring opportunities as (ID, ARGMAP)-tuples.

		Generator           generator = new Generator(seed, offset);
		RefactoringSupplier supplier  = null;

		switch (type) {
		case NONE:
			break;
		case INLINE_CONSTANT:
			supplier = new RandomInlineConstantFieldSupplier(workspace, generator);
			break;
		case EXTRACT_CONSTANT:
			supplier = new RandomExtractConstantSupplier(workspace, generator);
			break;
		case EXTRACT_METHOD:
			supplier = new RandomExtractMethodSupplier(workspace, generator);
			break;
		case INLINE_METHOD:
			supplier = new RandomInlineMethodSupplier(workspace, generator);
			break;
		case RENAME:

			if (!arguments.hasOption("length")) {
				System.out.println("Missing command-line option 'length' (-l) for rename refactorings.");
			}
			
			generator.setMaxLength(length);
			generator.setLengthFixed(fixed);    // TODO: Add as a command line option.

			supplier = new RandomRenameSupplier(workspace, generator);
			break;
		default:
			System.out.println("Unknown refactoring type.");
		}

		supplier.setShuffleSeed(shuffleSeed);
		supplier.setSelectSeed(selectSeed);

		boolean success = new RefactoringProcessor(supplier).processSupply(drop, limit);

		workspace.close(success);

		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		// nothing to do
	}
}
