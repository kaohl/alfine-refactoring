package org.alfine.refactoring.framework;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.alfine.refactoring.framework.WorkspaceConfiguration.LibEntry;
import org.alfine.refactoring.framework.WorkspaceConfiguration.SrcEntry;
import org.alfine.refactoring.framework.resources.Library;
import org.alfine.refactoring.framework.resources.Source;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class JavaProject {

	/* Source archives shared between projects (containing multiple source roots). */

	private static Map<Path, Source> parents = new HashMap<>();

	public static Map<Path, Source> getSharedSourceArchives() {
		return JavaProject.parents;
	}

	private Workspace           workspace;
	private ProjectConfiguration config;
	private Vector<Source>      sources;
	private Vector<Library>     libraries;
	private IJavaProject        javaProject;

	public JavaProject(Workspace workspace, ProjectConfiguration config, boolean fresh) {
		this.workspace = workspace;
		this.config     = config;
		this.sources   = new Vector<>();

		initialize(fresh);
	}

	/** Return `ProjectConfiguration'. */
	private ProjectConfiguration getConfig() {
		return this.config;
	}

	/** Return associated workspace. */
	public Workspace getWorkspace() {
		return this.workspace;
	}

	/** Return project folder in workspace. */
	public Path getLocation() {
		return getWorkspace().getLocation().resolve(getConfig().getName());
	}


	/** Return the associated IJavaProjct instance. */
	public IJavaProject getIJavaProject() {
		return this.javaProject;
	}

	/** Print raw classpath of associated IJavaProject. */
	public void showClasspath() throws Exception {
		for (IClasspathEntry e: javaProject.getRawClasspath()) {
			System.out.println("ClasspathEntry=" + e.getPath());
		}
	}

	private void initialize(boolean fresh) {

		// Create a corresponding java project in workspace.

		this.javaProject = createJavaProject(getConfig().getName(), fresh);


		// TODO: Load sources and libraries and populate the classpath.

		for (SrcEntry p : getConfig().getSrcs()) {
			importSource(p.getJar(), p.getDir());
		}

		for (LibEntry p : getConfig().getLibs()) {
			importLibrary(p.getLib(), p.getSrc(), p.isExported());
		}


/*
		Path inputSrcDir = getWorkspace().getSrcPath();
		Path inputLibDir = getWorkspace().getLibPath();

		Files.list(inputLibDir).forEach((Path file) -> {
			if (Files.exists(file) && Files.isRegularFile(file)) { // (File file).isFile()

				String resourceName       = file.getFileName().toString();
				String targetResourceName = resourceName;

				Path      source = file;
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
		*/
	}

	private void addSource(Source source) {
		this.sources.add(source);
	}

	private void addLibrary(Library library) {
		this.libraries.add(library);
	}

	/** Add the specified library to the project. */
	private void importLibrary(Path lib, Path src, boolean export) {
		addLibrary(new Library(lib, src, export));
	}

	/** Import source archive from `source' using `folder' as its source root, and return
	 * 	a `Source' instance representing the archive in the specified `project'. */
	private void importSource(Path source, String folder) {

		String[] ss      = source.toString().split(File.separator);
		String   filename = ss[ss.length -1];
		Path     target  = getLocation().resolve(filename + ".dir");

		Source result = null;

		if (folder.equals("/")) {

			result = new Source(source, folder, target);

		} else {

			Source parent = null;

			if (!getSharedSourceArchives().containsKey(source)) {

				Path parentTarget = null;

				parentTarget = getWorkspace().getLocation().resolve(filename + ".dir");
				parent       = new Source(source, "/", parentTarget);

				getSharedSourceArchives().put(source, parent);
			}

			parent = getSharedSourceArchives().get(source);

			result = new Source(parent, folder, target);
		}

		// Note: We do not add parents directly to projects; parents go into the shared collection!

		addSource(result);
	}

	public void exportSource(Path output) {
		
	}


	private static IJavaProject createJavaProject(String name, boolean fresh) {

		IWorkspace     workspace     = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot workspaceRoot = workspace.getRoot();

		// Create a new project in the workspace: ${workspace}/<project-name>.

		// String workspaceLocation = Platform.getInstanceLocation().getURL().getFile();

		// Path projectLocation = Paths.get(workspaceLocation, name);

		IProject project = workspaceRoot.getProject(name);

		if (fresh && project.exists()) {
			System.out.println("initialize(): Project exists. Deleting project.");
			try {
				project.delete(IResource.FORCE, null);
			} catch (CoreException e) {
				e.printStackTrace();
				throw new RuntimeException("Failed to delete project: " + name);
			}
			if (project.exists()) {
				throw new RuntimeException("Failed to delete existing project: " + name);
			}
		}

		System.out.println("initialize(): Creating a new project.");

		IProjectDescription description = null;

		try {

			project.create(new NullProgressMonitor());
			project.open(new NullProgressMonitor());

			description = project.getDescription();

		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException("An error occured. See previous stack trace.");
		}

		// https://www.vogella.com/tutorials/EclipseProjectNatures/article.html
		// https://help.eclipse.org/kepler/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2FresAdv_natures.htm

		if (!description.hasNature(JavaCore.NATURE_ID)) {

			String[] natures = description.getNatureIds();
			String[] newNatures = new String[natures.length + 1];
			System.arraycopy(natures, 0, newNatures, 0, natures.length);
			newNatures[natures.length] = JavaCore.NATURE_ID;// "org.eclipse.jdt.core.javanature";

			IStatus status = workspace.validateNatureSet(newNatures);

			if (status.getCode() == IStatus.OK) {
				description.setNatureIds(newNatures);
				try {
					project.setDescription(description, null);
				} catch (CoreException e) {
					e.printStackTrace();
					throw new RuntimeException("An error occured. See previous stack trace.");
				}
			} else {
				System.err.println("Failed to add JavaNature to project natures.");
				throw new RuntimeException("Failed to validate project nature set!");
			}
		}
		
		// Access project as a IJavaProject.

		IJavaProject javaProject = JavaCore.create(project);

		//importResources(projectFile, srcDirFile, libDirFile, outputFile);

		return javaProject;
	}
	
	
	
	/** This should be placed in the SourceEntry class which represent the classpath entry. */
/*
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
		
			public Stream<Resource> getVariableResources() {
		return resources.stream().filter(r -> { return (r instanceof SourceArchive) && r.isVariable(); });
	}


		public IPackageFragmentRoot getPackageFragmentRoot() {
			return javaProject.getPackageFragmentRoot(resource);
		}

	
*/

}
