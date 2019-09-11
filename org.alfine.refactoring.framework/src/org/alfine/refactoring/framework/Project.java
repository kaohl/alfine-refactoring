package org.alfine.refactoring.framework;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.alfine.util.PUP; // Utility program for dealing with jar files in a symmetric way (unpack/re-pack).
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;


// https://help.eclipse.org/kepler/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Freference%2Fapi%2Forg%2Feclipse%2Fcore%2Fresources%2FIProjectDescription.html

// 0. enter source generation phase of selected benchmark,
// 1. create assets folder with 'src' and 'lib' directories,
// 2. generate source archives into  ${assets}/src,
// 3. generate binary archives into  ${assets}/lib,
// 4. run eclipse product (see class CommandLineArguments for available options)
//     0. create eclipse project in workspace
//     1. import resources (assets)
//     2. run transformation
//     3. export resources (assets)
// 5. Reenter build:
//     0. import transformed source
//     1. compile benchmark and dependencies
//     2. run test (Consider introducing run configurations to enable different levels of testing)
//     3. produce tested artifacts and a benchmark bundle (executable jarfile (dacapoharness))
// 6. execute benchmark.
//
// IMPORTANT: Binary dependencies may not depend on source dependencies since that may break binaries after refactoring!
//



/* TODO: We should add classpath entries in a correct way: 
 * 
 * https://help.eclipse.org/kepler/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Fguide%2Fjdt_api_classpath.htm
 *
 * Note: The java source files under a classpath source entry must have package declarations which correspond to
 *       the directory structure from the source root.
 * 
 * */

public class Project {
	private static final Project instance = new Project();
	
	private Project() {} /* Singleton */

	/**
	 *	Base class for source and binary artifacts.
	 */
	public abstract class Resource {
		protected final File      source;
		protected final File      target;
		protected final File      output;
		protected final IResource resource;
		protected final boolean   variable; /* Whether archive is to be considered for transformation. */

		public Resource(File source, File target, File output, IResource resource, boolean variable) {
			this.source   = source;
			this.target   = target;
			this.output   = output;
			this.resource = resource;
			this.variable = variable;
		}

		public boolean isVariable() {
			return this.variable;
		}

		public IPackageFragmentRoot getPackageFragmentRoot() {
			return javaProject.getPackageFragmentRoot(resource);
		}

		protected void importResource() {
			if (verbose) {
				log("Importing resource: " + "source=" + source.getAbsolutePath() + ",target=" + target.getAbsolutePath());
			}

			copyFile(source, target);
		}

		protected void exportResource() {}

		private void addToClasspath() {
			try {
				log("resource(" + resource.getFullPath() + ").exists=" + resource.exists());

				IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(resource);

				IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
				IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
				System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
				newEntries[oldEntries.length] = JavaCore.newSourceEntry(root.getPath());
				
				javaProject.setRawClasspath(newEntries, null);
				
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		
		public void copyFile(File source, File target) {
			try (BufferedInputStream  in  = new BufferedInputStream(new FileInputStream(source));
				 BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {

				int b;
				while ((b = in.read()) != -1)
					out.write(b);	

			} catch (Exception e)  {
				e.printStackTrace();
			}
		}
	}
	
	private class SourceArchive extends Resource {
		public SourceArchive(File source, File target, File output, IResource resource, boolean variable) {
			super(source, target, output, resource, variable);	
		}
		
		@Override
		public void importResource() {

			if (verbose) {
				log("Importing resource: " + "source=" + source.getAbsolutePath() + ",target=" + target.getAbsolutePath());
			}

			// Unjar resource archive in project folder.
		
			try {
				// TODO: Refactor PUP usage into two methods: one for jar and one for unjar.
				
				PUP.main(new String[] {source.getAbsolutePath(), target.getAbsolutePath()});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void exportResource() {
			
			if (verbose) {
				log("Exporting resource: " + "target=" + target.getAbsolutePath() + ",output=" + output.getAbsolutePath());
			}

			// TODO: Refactor PUP usage into two methods: one for jar and one for unjar.
			
			try {
				PUP.main(new String[] {target.getAbsolutePath(), output.getAbsolutePath()});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private class BinaryArchive extends Resource {
		public BinaryArchive(File source, File target, File output, IResource resource, boolean variable) {
			super(source, target, output, resource, variable);
		}
	}

	private static boolean verbose;
	
	private static File projectFile;   /* File representing project folder under workspace. */
	private static File workspaceFile; /* File representing workspace folder. */
	private static File srcDirFile;    /* Location of source archives. */
	private static File libDirFile;    /* Location of binary archives. */
	private static File outputFile;    /* Location to where transformed resources are exported. */

	private static List<Resource> resources; /* Imported resources; filenames. */

	private static IProject     project;     /* The created project. */
	private static IJavaProject javaProject; /* The JavaModel of the created project. */

	private static void log(String msg) {
		
		if (Project.getVerbose()) {
			System.out.println(msg);
		}
	}

	public static void setVerbose(boolean b) {
		verbose = b;
	}

	public static boolean getVerbose() {
		return Project.verbose;
	}
	
	public static IJavaProject getJavaProject() {
		return javaProject;
	}

	public static void open(String projectName, String srcFolder, String libFolder, String outputFolder) throws Exception {

		String workspaceLocation = Platform.getInstanceLocation().getURL().getFile();

		workspaceFile = new File(workspaceLocation);
		projectFile   = new File(workspaceFile, projectName);
		srcDirFile    = new File(srcFolder);
		libDirFile    = new File(libFolder);
		outputFile    = new File(outputFolder); // TODO: Rename to outputDirFile.

		log("[Project] OPEN "
			+ "\n\tworkspace = " + workspaceFile.getAbsolutePath()
			+ "\n\tproject   = " + projectFile.getAbsolutePath()
			+ "\n\tsrc       = " + srcDirFile.getAbsolutePath()
			+ "\n\tlib       = " + libDirFile.getAbsolutePath()
			+ "\n\tout       = " + outputFile.getAbsolutePath());

		if (!srcDirFile.exists()) {
			throw new IllegalArgumentException("Invalid path for option 'src'. Directory '" + srcDirFile.getAbsolutePath() + "' does not exist.");
		}
		if (!libDirFile.exists()) {
			throw new IllegalArgumentException("Invalid path for option 'lib'. Directory '" + libDirFile.getAbsolutePath() + "' does not exist.");
		}
		if (!srcDirFile.isDirectory()) {
			throw new IllegalArgumentException("Invalid path for option 'src': '" + srcDirFile.getAbsolutePath() + "' is not a directory.");
		}		
		if (!libDirFile.isDirectory()) {
			throw new IllegalArgumentException("Invalid path for option 'lib': '" + libDirFile.getAbsolutePath() + "' is not a directory.");
		}

		// Disable initialize to validate CLI-args.
		initialize();
	}

	public static void close(boolean writeSrcToOutput) {
		
		// Export imported source artifacts to output folder specified on command-line.

		if (verbose) {
			log("Exporting project (save?=" + writeSrcToOutput + ")");
		}

		if (writeSrcToOutput) {
			resources.stream().forEach(resource -> {
				resource.exportResource();
			});
		}
		
		try {
			// This does not seem to do anything...
			project.close(new NullProgressMonitor());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public static List<IPackageFragmentRoot> getAvailableRoots() {
		return resources.stream()
				.filter((Resource r) -> { return (r instanceof SourceArchive) && r.isVariable(); })
				.map((Resource r) -> { return r.getPackageFragmentRoot(); })
				.collect(Collectors.toList());
	}

	private static void initialize() throws Exception {
		IWorkspace     workspace     = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot workspaceRoot = workspace.getRoot();

		// Create a new project in the workspace: ${workspace}/<project-name>.

		project = workspaceRoot.getProject(projectFile.getName());

		if (project.exists()) {
			log("initialize(): Project exists. Deleting project.");
			project.delete(IResource.FORCE, null);
		}

		if (project.exists()) {
			throw new Exception("Failed to delete existing project.");
		}

		log("initialize(): Creating a new project.");

		project.create(new NullProgressMonitor());
		project.open(new NullProgressMonitor());

		// https://www.vogella.com/tutorials/EclipseProjectNatures/article.html
		// https://help.eclipse.org/kepler/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2FresAdv_natures.htm

		IProjectDescription description = project.getDescription();

		if (!description.hasNature(JavaCore.NATURE_ID)) {

			String[] natures = description.getNatureIds();
			String[] newNatures = new String[natures.length + 1];
			System.arraycopy(natures, 0, newNatures, 0, natures.length);
			newNatures[natures.length] = JavaCore.NATURE_ID;// "org.eclipse.jdt.core.javanature";

			IStatus status = workspace.validateNatureSet(newNatures);

			if (status.getCode() == IStatus.OK) {
				description.setNatureIds(newNatures);
				project.setDescription(description, null);
			} else {
				log("Failed to add JavaNature to project natures.");
			}
		}
		
		// Access project as a IJavaProject.

		javaProject = JavaCore.create(project);

		importResources(projectFile, srcDirFile, libDirFile, outputFile);
	}

	private static void importResources(File projectDir, File srcDir, File libDir, File outputDir) throws Exception {

		File projectLibDir = new File(projectDir, "lib"); // Location of binary archives.
		
		if (!projectLibDir.exists()) {
			projectLibDir.mkdir();
		}

		resources = new LinkedList<Resource>();
		
		if (!srcDir.exists()) {
			throw new Exception("Source archive directory does not exist.");
		}
		if (!libDir.exists()) {
			throw new Exception("Binary archive directory does not exist.");
		}
		if (!outputDir.mkdirs()) {
			throw new Exception("Could not make output directory: " + outputDir.getAbsolutePath());
		}

		File configFile = new File(srcDir, "variable.config");

		Set<String> variables = new HashSet<>();

		try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {

			br.lines()
			.map    (line -> line.trim())
			.filter  (line -> !line.equals(""))
			.forEach(name -> { variables.add(name); });

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to read configuration file: " + configFile.getCanonicalPath());
		}

		Arrays.stream(libDir.listFiles()).forEach((File file) -> {
			if (file.exists() && file.isFile()) {

				String resourceName       = file.getName();
				String targetResourceName = resourceName;

				File      source = new File(libDir, resourceName);
				File      target = new File(projectLibDir, targetResourceName);
				File      output = null; // Binaries are not transformed and are therefore not be copied around.
				IResource res    = null;

				res = project.getFile(new Path("/lib/" + targetResourceName));

				resources.add(instance.new BinaryArchive(source, target, output, res, false));
			}
		});

		Arrays.stream(srcDir.listFiles())
		.filter  (file -> !file.getName().equals(configFile.getName()))
		.forEach((File file) -> {
			if (file.exists() && file.isFile()) {
				String resourceName    = file.getName();
				String resourceNameDir = resourceName + ".dir";

				File      source = new File(srcDir, resourceName);
				File      target = new File(projectDir, resourceNameDir);
				File      output = new File(outputDir, resourceName);
				IResource res    = project.getFolder(resourceNameDir);

				target.mkdir();

				resources.add(instance.new SourceArchive(source, target, output, res, variables.contains(resourceName)));
			}
		});
				
		// Note: As far as I know, we have to reset the classpath since the
		//       paths we are trying to add are located below the project
		//       folder, which is on the classpath by default. I'm guessing
		//       we are prevented from adding these additional source folders
		//       or archives (jar, zip) as source roots to prevent the model
		//       from having nested source roots (IPackageFragmentRoot).

		// Reset classpath.

		javaProject.setRawClasspath(new IClasspathEntry[] {}, null);

		resources.forEach((Resource resource) -> {
			resource.importResource();
			resource.addToClasspath();
		});

		// Refresh project to make resources visible.

		project.refreshLocal(IProject.DEPTH_INFINITE, new NullProgressMonitor());
		
		// TODO: Order classpath entries so that binaries are placed first?
		//       (Use order as specified in a configuration file?)

		if (verbose) {
			showClasspath();
		}
	}
	
	public static void showClasspath() throws Exception {
		for (IClasspathEntry e: javaProject.getRawClasspath()) {
			System.out.println("ClasspathEntry=" + e.getPath());
		}
	}

	/*
	public static IPackageFragment[] getPackageFragments() throws JavaModelException {
		return javaProject.getPackageFragments();
	}
	*/
	/*
	public static boolean applyRefactoring(Refactoring refactoring) {
		//
		// Is this useful?
		// RefactoringASTParser p; p.parse(typeRoot, owner, resolveBindings, statementsRecovery, bindingsRecovery, pm);
	
		// We can get undo here... (But we do not want to execute in a new thread...)
		// final int style = org.eclipse.ltk.core.refactoring.CheckConditionsOperation.ALL_CONDITIONS;
		// PerformRefactoringOperation prop = new PerformRefactoringOperation(refactoring, style);
	
		System.out.println("Project.Refactor.applyRefactoring()");
				
		boolean result = false;
	
		try {
			RefactoringStatus status = refactoring.checkAllConditions(new NullProgressMonitor());

			if (status.hasError()) {
				log("Refactoring status has errors: " + status);
				throw new Exception("Refactoring status has errors.");
			}

			Change change = refactoring.createChange(new NullProgressMonitor());

			try {

				change.initializeValidationData(new NullProgressMonitor());

				if (!change.isEnabled()) {
					// log("Change is not enabled: " + change);
					throw new Exception("Change is not enabled: "  + change);
				}

				RefactoringStatus valid = change.isValid(new NullProgressMonitor());

				if (valid.hasError()) {
					// log("Change validity has errors: " + valid);
					throw new Exception("Change validity has errors: " + valid);
				}

				Object o = change.perform(new NullProgressMonitor());

				if (o == null) {
					// log("Failed to apply change!");
					throw new Exception("Failed to apply change.");
				}

				result = true;

			} finally {
				change.dispose();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	*/
		

	    /*
	    WorkingCopyOwner owner = new WorkingCopyOwner() {};
	    
	    // Create a new working copy for the compilation unit.
	    ICompilationUnit wc = unit.getWorkingCopy(owner, new NullProgressMonitor());
	    
	    System.out.println("owner=" + wc.getOwner());
	    System.out.println("is_owner=" + (owner == wc.getOwner()));
	    
	    */
	    // System.out.println("decl...=" + decl.toString());
	    
	    //ASTRewrite rewrite;
	   
	    /*
	    RenameJavaElementDescriptor descriptor = new RenameJavaElementDescriptor("");
	    descriptor.setJavaElement((IJavaElement)decl);
	    descriptor.setNewName("B");
	    descriptor.setUpdateReferences(true);
	    descriptor.setRenameGetters(true);
	    descriptor.setRenameSetters(true);


	    org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor.;
		RenameSupport support = RenameSupport.create(descriptor);
		
		
			    // RenameTypeProcessor processor = new RenameTypeProcessor((IType) decl);
	     */

	    // RenameSupport support = RenameSupport.create(unit, "B", RenameSupport.UPDATE_REFERENCES | RenameSupport.UPDATE_TEXTUAL_MATCHES);
	    		
	    		
	    		//  org.eclipse.jdt.ui.actions.RenameAction 

	    /*
	    RenameRefactoring rr = new RenameRefactoring();
	    RenameProcessor rp = new RenameProcessor();
	    
	    org.eclipse.ltk.ui.refactoring.R
	    org.eclipse.ltk.core.refactoring
	    */
	    /*
	    final String name = this.newMethodName;

	    cunit.accept(new ASTVisitor() {
	        public boolean visit(MethodInvocation methodInvocation)
	        {
	            String methodName = methodInvocation.getName().toString();
	            System.out.println(methodName);
	            if (methodName.equals(name))
	            {
	                int startPosition = methodInvocation.getStartPosition();
	                length = methodInvocation.getLength();
	                System.out.printf("startPosition %d - Length %d", startPosition, length);       
	            }
	            return false;
	        }
	    });
	    */
}
