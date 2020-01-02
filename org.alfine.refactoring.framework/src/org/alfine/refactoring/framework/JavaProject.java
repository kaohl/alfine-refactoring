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
import org.eclipse.core.runtime.IPath;
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

	/** Return IProject handle of this java project. */
	private IProject getIProject() {
		return this.project;
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

	private void setRawClasspath(IClasspathEntry[] classpath) {
		try {
			javaProject.setRawClasspath(classpath, null);
		} catch (JavaModelException e1) {
			e1.printStackTrace();
		}
	}

	private void initialize(boolean fresh) {

		// Create a corresponding java project in workspace.

		String projectName = getConfig().getName();

		System.out.println("JavaProject::initialize(boolean): projectName = " + projectName);

		this.project     = getWorkspace().getProject(projectName);
		this.javaProject = getWorkspace().newJavaProject(projectName, fresh);

		// Load sources and libraries and populate classpath.

		if (fresh) {
			setRawClasspath(new IClasspathEntry[] {});
		}

		for (String d : getConfig().getDeps()) {
			if (!addDependency(d, fresh)) {
				System.err.println("Dependency not on classpath: project = `" + projectName + "`, dep = `" + d + "`");
			}
		}

		for (SrcEntry p : getConfig().getSrcs()) {
			importSource(p.getJar(), p.getDir(), p.isVariable(), fresh);
		}

		for (LibEntry p : getConfig().getLibs()) {
			importLibrary(p.getLib(), p.getSrc(), p.isExported(), fresh);
		}

		// Refresh project to make resources visible.

		refresh();

		showClasspath();

		if (fresh) {

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
	private boolean addDependency(String dep, boolean fresh) {
		this.dependencies.add(dep);

		IProject p = getWorkspace().getProject(dep);

		System.out.println("dependency path (fullpath):" + p.getFullPath());

		if (fresh) {
			addClasspathEntry(JavaCore.newProjectEntry(p.getFullPath(), true));
		}

		return getIJavaProject().isOnClasspath(p);
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

	/** Refresh project resource hierarchy to make new resources visible. */
	private void refresh() {
		try {
			getIProject().refreshLocal(IProject.DEPTH_INFINITE, new NullProgressMonitor());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/** Add source entry to project classpath. */
	private void addSource(Source source, boolean isVariable, boolean fresh) {
		this.sources.add(source);

		Path target = Paths.get(source.getTarget().getFileName().toString());

		//target = source.getSource() == null ? Paths.get(getConfig().getName()).resolve(target) : target;

		// System.out.println("target  = " + target);

		if (fresh) {
			source.importResource();
			refresh();
		}

		IFolder targetFolder = project.getFolder(target.toString());

		if (fresh) {
			addClasspathEntry(asSourceEntry(targetFolder));
		}

		if (isVariable) {

			System.out.println(
				"Add IPackageFragmentRoot: project = "
				+ project.getName()
				+ ", target = "
				+ target.toString()
			);

			IPackageFragmentRoot packageFragmentRoot = 
				getIJavaProject().getPackageFragmentRoot(targetFolder);

			getWorkspace().addVariableSourceRoot(packageFragmentRoot);
		}
	}

	/** Return an IClasspath entry representing the specified library with optional source attachment. */
	private IClasspathEntry asLibEntry(IPath lib, IPath src, boolean doExport) {

		return JavaCore.newLibraryEntry(
			lib,
			src,
			null, // sourceAttachmentRootPath (root within archive)
			doExport
		);
	}

	/** Add library to project classpath. */
	private void addLibrary(Library library, boolean fresh) {

		this.libraries.add(library);

		Path libPath = library.getBinaryPath();
		Path srcPath = library.getSourcePath();

		org.eclipse.core.runtime.Path lib = new org.eclipse.core.runtime.Path(libPath.toString());

		org.eclipse.core.runtime.Path src =
			srcPath != null
			? new org.eclipse.core.runtime.Path(srcPath.toString())
			: null;

		if (fresh) {
			addClasspathEntry(asLibEntry(lib, src, library.isExported()));
		}
	}

	/** Add the specified library to the project. */
	private void importLibrary(Path lib, Path src, boolean export, boolean fresh) {
		addLibrary(new Library(lib, src, export), fresh);
	}

	/** Import source archive from `source' using `folder' as its source root, and return
	 * 	a `Source' instance representing the archive in the specified `project'. 
	 * @param b 
	 * @param isVariable */
	private void importSource(Path source, String folder, boolean isVariable, boolean fresh) {

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

		addSource(result, isVariable, fresh);
	}

	public void exportSource(Path output) {
		for (Source s : getSources()) {
			s.exportResource(output);
		}
	}
}
