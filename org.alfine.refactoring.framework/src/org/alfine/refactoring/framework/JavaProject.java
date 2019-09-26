package org.alfine.refactoring.framework;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

import org.alfine.refactoring.framework.WorkspaceConfiguration.LibEntry;
import org.alfine.refactoring.framework.WorkspaceConfiguration.SrcEntry;
import org.alfine.refactoring.framework.resources.Library;
import org.alfine.refactoring.framework.resources.Source;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

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
	private Vector<String>      dependencies;

	private IProject            project;
	private IJavaProject        javaProject;

	public JavaProject(Workspace workspace, ProjectConfiguration config, boolean fresh) {
		this.workspace    = workspace;
		this.config        = config;
		this.sources      = new Vector<>();
		this.libraries    = new Vector<>();
		this.dependencies = new Vector<>();

		this.javaProject  = null;

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

	/** Return project source archives. */
	public Vector<Source> getSources() {
		return this.sources;
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
	public void showClasspath() {
		try {
			for (IClasspathEntry e: javaProject.getRawClasspath()) {
				System.out.println("ClasspathEntry=" + e.getPath());
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private void initialize(boolean fresh) {

		// Create a corresponding java project in workspace.

		String projectName = getConfig().getName();

		this.project     = getWorkspace().newProject(projectName);
		this.javaProject = getWorkspace().newJavaProject(projectName, fresh);

		// Load sources and libraries and populate classpath.

		try {
			javaProject.setRawClasspath(new IClasspathEntry[] {}, null);
		} catch (JavaModelException e1) {
			e1.printStackTrace();
		}

		for (String d : getConfig().getDeps()) {
			addDependency(d);
		}

		for (SrcEntry p : getConfig().getSrcs()) {
			importSource(p.getJar(), p.getDir(), p.isVariable());
		}

		for (LibEntry p : getConfig().getLibs()) {
			importLibrary(p.getLib(), p.getSrc(), p.isExported());
		}

		// Refresh project to make resources visible.

		try {
			project.refreshLocal(IProject.DEPTH_INFINITE, new NullProgressMonitor());
		} catch (CoreException e) {
			e.printStackTrace();
		}

		showClasspath();

		// Set project dependencies.
		try {
			IProjectDescription desc = project.getDescription();
			desc.setReferencedProjects(getProjectReferences());
			project.setDescription(desc, new NullProgressMonitor());
			project.clearCachedDynamicReferences();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public void printReferencingProjects() {
		for (IProject p : project.getReferencingProjects()) {
			System.out.println(p.getName() + " references " + this.getConfig().getName());
		}
	}

	public IProject[] getProjectReferences() {
		return this.dependencies.stream()
			.map(name -> {
				IWorkspace     workspace     = ResourcesPlugin.getWorkspace();
				IWorkspaceRoot workspaceRoot = workspace.getRoot();
				IProject p = workspaceRoot.getProject(name);
				return p;
			})
			.collect(Collectors.toList())
			.toArray(new IProject[0]);
	}

	public void close() {
		try {
			project.close(new NullProgressMonitor());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private void addClasspathEntry(IClasspathEntry entry) {
		try {

			IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
			IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
			System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
			newEntries[oldEntries.length] = entry;

			javaProject.setRawClasspath(newEntries, null);

		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	/** */
	private void addDependency(String dep) {
		this.dependencies.add(dep);

		IWorkspace     workspace     = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot workspaceRoot = workspace.getRoot();
		IProject p = workspaceRoot.getProject(dep);

		
		// Path      path   = getWorkspace().getLocation().resolve(Paths.get(dep));
		// IResource folder = project.getFolder(path.toString());


		
		//JavaCore.getJavaCore().
		//javaProject.isOnClasspath(element);
		//javaProject.isOnClasspath(resource);
		/*
		try {
			return javaProject.getClasspathEntryFor(folder.getFullPath());
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return null;
		*/
		
		// for (IProject p : project.getReferencingProjects()) {}
		
		//System.out.println("dependency path:" + folder.getFullPath());
		//addClasspathEntry(JavaCore.newProjectEntry(folder.getFullPath(), true));
		//System.out.println("dependency path (location):" + p.getLocation());
		//addClasspathEntry(JavaCore.newProjectEntry(p.getLocation(), true));
		System.out.println("dependency path (fullpath):" + p.getFullPath());
		addClasspathEntry(JavaCore.newProjectEntry(p.getFullPath(), true));
	}

	private IClasspathEntry asSourceEntry(IResource resource) {
		if (resource.exists()) {
			IPackageFragmentRoot root  = javaProject.getPackageFragmentRoot(resource);
			IClasspathEntry      entry = JavaCore.newSourceEntry(root.getPath());
			return entry;
		} else {
			System.err.println("Error: asSourceClasspathEntry(): Resource `" + resource.getFullPath() + "'does not exist.");
		}
		return null;
	}

	/** Add source entry to project classpath. */
	private void addSource(Source source, boolean isVariable) {
		this.sources.add(source);

		Path target = Paths.get(source.getTarget().getFileName().toString());

		//target = source.getSource() == null ? Paths.get(getConfig().getName()).resolve(target) : target;

		// System.out.println("target  = " + target);

		source.importResource();

		// Refresh project to make resources visible.

		try {
			project.refreshLocal(IProject.DEPTH_INFINITE, new NullProgressMonitor());
		} catch (CoreException e) {
			e.printStackTrace();
		}

		IFolder targetFolder = project.getFolder(target.toString());

		addClasspathEntry(asSourceEntry(targetFolder));

		if (isVariable) {

			System.out.println(
				"Add IPackageFragmentRoot: project = "
				+ project.getName()
				+ ", target = "
				+ target.toString()
			);

			IPackageFragmentRoot packageFragmentRoot = 
				javaProject.getPackageFragmentRoot(targetFolder);

			getWorkspace().addVariableSourceRoot(packageFragmentRoot);
		}
	}

	/** Return an IClasspath entry representing the specified library with optional source attachment. */
	private IClasspathEntry asLibEntry(IResource lib, IResource src, boolean doExport) {

		// TODO: Get source paths:
		// IPackageFragmentRoot root  = javaProject.getPackageFragmentRoot(...);
		// if (src != null) ...

		return JavaCore.newLibraryEntry(
			lib.getFullPath(),
			null, // src.getFullPath(),
			null, // sourceAttachmentRootPath
			doExport
		);
	}

	/** Add library to project classpath. */
	private void addLibrary(Library library) {

		this.libraries.add(library);

		addClasspathEntry(asLibEntry(
			project.getFile(library.getBinaryPath().toString()),
			project.getFile(library.getSourcePath().toString()),
			library.isExported())
		);
	}

	/** Add the specified library to the project. */
	private void importLibrary(Path lib, Path src, boolean export) {
		addLibrary(new Library(lib, src, export));
	}

	/** Import source archive from `source' using `folder' as its source root, and return
	 * 	a `Source' instance representing the archive in the specified `project'. 
	 * @param b 
	 * @param isVariable */
	private void importSource(Path source, String folder, boolean isVariable) {

		String[] ss      = source.toString().split(File.separator);
		String   filename = ss[ss.length - 1];
		Path     target  = getLocation().resolve(filename + ".dir");

		//System.out.println("getLocation() = " + getLocation());
		//System.out.println("getLocation().resolve(filename.dir) = " + target);

		Source result = null;

		if (folder.equals("/")) {

			result = new Source(source, folder, target);

		} else {

			Source parent = null;

			if (!getSharedSourceArchives().containsKey(source)) {

				Path parentTarget = null;

				parentTarget = getWorkspace().getLocation().resolve(filename + ".dir");

				System.out.println("parentTarget = " + parentTarget);

				parent       = new Source(source, "/", parentTarget);

				getSharedSourceArchives().put(source, parent);
			}

			parent = getSharedSourceArchives().get(source);

			result = new Source(parent, folder, target);
		}

		addSource(result, isVariable);
	}

	public void exportSource(Path output) {
		for (Source s : getSources()) {
			s.exportResource(output);
		}
	}
}
