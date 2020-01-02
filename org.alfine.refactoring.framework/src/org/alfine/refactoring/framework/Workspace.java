package org.alfine.refactoring.framework;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

public class Workspace {

	private Path location; /* Workspace folder. */
	private Path srcPath;  /* Folder containing source archives. */
	private Path libPath;  /* Folder containing binary archives. */
	private Path outPath;  /* Destination folder for transformed source archives. */

	/* Project configuration from configuration file. */
	private Map<String, ProjectConfiguration> projectMap;
	private Vector<ProjectConfiguration>      projectVec;
	private Map<String, JavaProject>         projects;

	/* Source roots to be considered variable in the workspace. */
	private Set<IPackageFragmentRoot> variableSourceRootFolders;
	
	public Workspace(WorkspaceConfiguration config, Path srcPath, Path libPath, Path outPath, boolean fresh) {
		this.location                  = config.getLocation();
		this.projectVec                = config.getProjects();
		this.projectMap                = config.getProjectMap();
		this.variableSourceRootFolders = new HashSet<>();
		this.projects                  = new HashMap<>();

		this.srcPath = srcPath;
		this.libPath = libPath;
		this.outPath = outPath;

		initialize(fresh);
	}

	/** Return set of all registered variable source root folders within the workspace. */
	public Set<IPackageFragmentRoot> getVariableSourceRoots() {
		return this.variableSourceRootFolders;
	}

	/** Register `folder' as a variable source root. */
	public void addVariableSourceRoot(IPackageFragmentRoot root) {
		variableSourceRootFolders.add(root);
	}

	/** Return workspace folder. */
	public Path getLocation() {
		return this.location;
	}

	/** Return `Path' to folder with source archives. */
	public Path getSrcPath() {
		return this.srcPath;
	}

	/** Return `Path' to folder with binary archives. */
	public Path getLibPath() {
		return this.libPath;
	}

	/** Return `Path' to source output folder. */
	public Path getOutPath() {
		return this.outPath;
	}

	/** Return vector with projects in top-down configuration file order (declare before use!). */
	public Vector<ProjectConfiguration> getProjectVec() {
		return this.projectVec;
	}

	/** Return the specified project or null if it does not exist. */
	public ProjectConfiguration getProjectConfiguration(String name) {
		return this.projectMap.get(name);
	}

	/** Initializes the workspace by loading configured projects and resources. */
	private void initialize(boolean fresh) {

		System.out.println("Initializing workspace ...");

		if (fresh) {			

			boolean validConfig = true;

			for (ProjectConfiguration p : getProjectVec()) {
				if (!p.validate(this)) {
					validConfig = false;
				}
				// Run through all to print errors.
			}

			if (!validConfig) {
				throw new RuntimeException("Workspace configuration contains errors.");
			}
		}

		for (ProjectConfiguration p : getProjectVec()) {
			if (p == null) {
				throw new RuntimeException("Workspace::initialize(): ProjectConfiguration is null!");
			}
			projects.put(p.getName(), new JavaProject(this, p, fresh));
		}

		if (fresh) {
			for (String name : projects.keySet()) {
				JavaProject p = projects.get(name);
				p.printReferencingProjects();
			}
		}

		System.out.println("Workspace initialized.");
	}

	/** Return true if a project with the specified name exists. */
	public boolean hasProject(String name) {
		return this.projectMap.containsKey(name);
	}

	/** Return true if a file with the specified name exists in the configured source folder. */
	public boolean isSrcAvailable(String jar) {
		return Files.exists(srcPath.resolve(jar));
	}

	/** Return true if a file with the specified name exists in the configured source folder. */
	public boolean isSrcAvailable(String dir, String jar) {
		// TODO: Check that archive contains `dir'.
		return Files.exists(srcPath.resolve(jar));
	}

	/** Return true if a file with the specified name exists in the configured library folder. */
	public boolean isLibAvailable(String jar) {
		return Files.exists(libPath.resolve(jar));
	}

	/** Export project source code. This method is called from `close()'. */
	public void exportSource() {

		Path output = getOutPath();

		System.out.println("Exporting to " + output.toString());

		if (!Files.exists(output)) {
			try {
				Files.createDirectories(output);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		for (String key : projects.keySet()) {
			projects.get(key).exportSource(output);
		}

		/* Finalize export of shared source archives. */

		Map<Path, Source> sharedArchives = null;

		sharedArchives = JavaProject.getSharedSourceArchives();

		for (Path p : sharedArchives.keySet()) {
			sharedArchives.get(p).exportResource(output);
		}
	}

	/** Return handle to the specified project. */
	public IProject getProject(String name) {
		IWorkspace     workspace     = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot workspaceRoot = workspace.getRoot();
		return workspaceRoot.getProject(name);
	}

	/** Open existing java project. */
	private IJavaProject openJavaProject(String name) {

		IProject project = getProject(name);

		try {

			project.open(new NullProgressMonitor());

			if (!project.getDescription().hasNature(JavaCore.NATURE_ID)) {
				throw new RuntimeException("Bad java project configuration. Expected java nature.");
			}

		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException("An error occured. See previous stack trace.");
		}

		return JavaCore.create(project);
	}

	/** Open the java project specified by `name`. If `fresh` is true, the project is created first. */
	public IJavaProject newJavaProject(String name, boolean fresh) {

		if (!fresh) {
			return openJavaProject(name);
		}

		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		// Create a new project in the workspace: ${workspace}/<project-name>.

		// String workspaceLocation = Platform.getInstanceLocation().getURL().getFile();

		// Path projectLocation = Paths.get(workspaceLocation, name);

		IProject project = getProject(name);

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
		return JavaCore.create(project);
	}

	public void close(boolean exportToOutput) {
		
		// Export imported source artifacts to output folder specified on command-line.

		System.out.println("Exporting project (save?=" + exportToOutput + ")");

		if (exportToOutput) {
			exportSource();
		}
	}
}
