package org.alfine.refactoring.framework;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.alfine.refactoring.framework.resources.Source;
import org.alfine.refactoring.suppliers.Cache;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class Workspace {
	private Path location;  /* Workspace folder. */
	private Path srcPath;   /* Folder containing source archives. */
	private Path libPath;   /* Folder containing binary archives. */
	private Path outPath;   /* Destination folder for transformed source archives. */

	private Cache cache; /* Refactoring descriptor cache. */

	/* Project configuration from configuration file. */
	private Map<String, ProjectConfiguration> projectMap;
	private List<ProjectConfiguration>        projectVec;
	private Map<String, JavaProject>          projects;

	private class VariablePackageFragments {
		private String               projectName;
		private IPackageFragmentRoot root;
		private Set<String>          includedPackagesNames;
		
		public VariablePackageFragments(String projectName, IPackageFragmentRoot root, Set<String> includedPackagesNames) {
			this.projectName           = projectName;
			this.root                  = root;
			this.includedPackagesNames = includedPackagesNames;
		}
		
		public String getProjectName() {
			return this.projectName;
		}

		private Set<IPackageFragment> getPackageFragments(Predicate<IPackageFragment> filter) {
			Set<IPackageFragment> fragments = new HashSet<>();
			try {
				Arrays.asList(root.getChildren()).stream()
				.filter(c -> c instanceof IPackageFragment)
				.map(IPackageFragment.class::cast)
				.filter(filter)
				.forEach(f -> {
					fragments.add(f);
					getPackageFragmentsRec(f, fragments, filter);
				});
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
			return fragments;
		}
		
		private void getPackageFragmentsRec(IPackageFragment fragment, Set<IPackageFragment> set, Predicate<IPackageFragment> filter) {
			try {

				Arrays.asList(fragment.getChildren()).stream()
				.filter(c -> c instanceof IPackageFragment)
				.map(IPackageFragment.class::cast)
				.filter(filter)
				.forEach(f -> {
					set.add((IPackageFragment) f);
					getPackageFragmentsRec((IPackageFragment)f, set, filter);
				});
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		
		public Set<IPackageFragment> getVariablePackageFragments() {
			return getPackageFragments(fragment -> {
				return fragment instanceof IPackageFragment
					&& this.includedPackagesNames.contains(((IPackageFragment)fragment).getElementName());
			});
		}
		
		public Set<IPackageFragment> getNonVariablePackageFragments() {
			return getPackageFragments(fragment -> {
				return fragment instanceof IPackageFragment
					&& !this.includedPackagesNames.contains(((IPackageFragment)fragment).getElementName());
			});
		}
	}

	private WorkspaceConfiguration config;

	/* Source roots to be considered variable in the workspace (depending on workspace configuration). */
	private Set<VariablePackageFragments> variableSourceRootFolders;

	private List<String> compilationUnitsFilterList = new ArrayList<String>();

	public Workspace(WorkspaceConfiguration config, boolean fresh) {
		this.location                  = config.getLocation();
		this.projectVec                = config.getProjects();
		this.projectMap                = config.getProjectMap();
		this.variableSourceRootFolders = new HashSet<>();
		this.projects                  = new HashMap<>();
		this.compilationUnitsFilterList = config.getIncludedCompilationUnitsNames();

		this.srcPath   = config.getSrcPath();
		this.libPath   = config.getLibPath();
		this.outPath   = config.getOutPath();

		this.config = config;

		initialize(fresh);

		this.cache  = new Cache(config.getCachePath());
	}

	public WorkspaceConfiguration getConfiguration() {
		return this.config;
	}

	public Collection<IPackageFragment> getFragments(Predicate<IPackageFragment> filter) {
		List<IPackageFragment> fragments = new LinkedList<>();
		for (JavaProject jp : this.projects.values()) {
			try {
				fragments.addAll(Arrays.asList(jp.getIJavaProject().getPackageFragments()).stream().filter(filter).collect(Collectors.toList()));
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Found fragments " + fragments.size());
		return fragments;
	}

	public JavaProject getJavaProject(String name) {
		return this.projects.get(name);
	}

	private Map<String, List<IPackageFragment>> mapVariableFragments() {
		Map<String, List<IPackageFragment>> result = new HashMap<>();
		
		this.variableSourceRootFolders.stream().forEach(vsf -> {
			result.computeIfAbsent(vsf.getProjectName(), (key) -> {
				return new ArrayList<IPackageFragment>();});
			result.computeIfPresent(vsf.getProjectName(), (k,v) -> {
				v.addAll(vsf.getVariablePackageFragments());
				return v; });
		});

		return result;
	}

	private Map<String, List<IPackageFragment>> mapNonVariableFragments() {
		Map<String, List<IPackageFragment>> result = new HashMap<>();
		
		this.variableSourceRootFolders.stream().forEach(vsf -> {
			result.computeIfAbsent(vsf.getProjectName(), (key) -> {
				return new ArrayList<IPackageFragment>();});
			result.computeIfPresent(vsf.getProjectName(), (k,v) -> {
				v.addAll(vsf.getNonVariablePackageFragments());
				return v; });
		});
		
		return result;
	}

	/** Collect all variable fragments on a project-to-project basis and write them to file.*/
	public void writePackagesConfigHelper(Path packagesConfigHelperPath) {
		
		final Map<String, List<IPackageFragment>> variableFragments =
			mapVariableFragments();
		
		final Map<String, List<IPackageFragment>> nonVariableFragments =
				mapNonVariableFragments();
		
		try (BufferedWriter out =
				Files.newBufferedWriter(
					packagesConfigHelperPath,
					StandardCharsets.UTF_8,
					java.nio.file.StandardOpenOption.CREATE)) {
			
			Set<String> keys = new HashSet<>();
			keys.addAll(variableFragments.keySet());
			keys.addAll(nonVariableFragments.keySet());

			// Output variable and non-variable fragments so that the
			// user can move package lines between the two variables
			// `<project-name>.include` and `<project-name>.exclude`
			// in `packages.config` to vary the scope in which the
			// refactoring framework search for refactoring
			// opportunities.
			
			keys.forEach(projectName -> {
				try {
					
					final String INDENT = "    ";
					
					out.write(projectName + ".include=");
					Optional.ofNullable(variableFragments.get(projectName)).ifPresent(pFrags -> {
						pFrags.forEach(u -> {
							try {
								out.write("\\");
								out.newLine();
								out.write(INDENT + u.getElementName());
							} catch (IOException e) {
								e.printStackTrace();
							}
						});
						try {
							out.newLine();
							out.newLine();
						} catch (IOException e) {
							e.printStackTrace();
						}
					});

					out.write(projectName + ".exclude=");
					Optional.ofNullable(nonVariableFragments.get(projectName)).ifPresent(pFrags -> {
						pFrags.forEach(u -> {
							try {
								out.write("\\");
								out.newLine();
								out.write(INDENT + u.getElementName());
							} catch (IOException e) {
								e.printStackTrace();
							}
						});
						try {
							out.newLine();
							out.newLine();
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/** Return set of all registered variable source folders within the workspace. */
	public Set<IPackageFragment> getVariableSourceFragments() {
		return this.variableSourceRootFolders.stream()
				.map(vsr -> vsr.getVariablePackageFragments())
				.flatMap(set -> set.stream())
				.collect(Collectors.toSet());
	}

	/** Return set of all registered variable source folders within the workspace. */
	public Set<IPackageFragment> getNonVariableSourceFragments() {
		return this.variableSourceRootFolders.stream()
				.map(vsr -> vsr.getNonVariablePackageFragments())
				.flatMap(set -> set.stream())
				.collect(Collectors.toSet());
	}

	/** Register `root' as a variable source root from packages whose names are in
	 * `includedPackagesNames` should be open for transformation. */
	public void addVariableSourceRoot(String projectName, IPackageFragmentRoot root, Set<String> includedPackagesNames) {
		variableSourceRootFolders.add(new VariablePackageFragments(projectName, root, includedPackagesNames));
	}
	
	public void writeUnitConfigHelper(List<String> units) {
		Path path = getSrcPath().resolve("units.config.helper");
		if (!Files.exists(path)) {
			try (BufferedWriter out =
					Files.newBufferedWriter(
						path,
						StandardCharsets.UTF_8,
						java.nio.file.StandardOpenOption.CREATE)) {
				units.forEach(u -> {
					try {
						out.write(u);
						out.newLine();
					} catch (IOException e1) {
							e1.printStackTrace();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}	
		}
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

	/** Return refactoring descriptor cache. */
	public Cache getCache() {
		return this.cache;
	}

	/** Return list with projects in top-down configuration file order (declare before use!). */
	public List<ProjectConfiguration> getProjects() {
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

			for (ProjectConfiguration p : getProjects()) {
				if (!p.validate(this)) {
					validConfig = false;
				}
				// Run through all to print errors.
			}

			if (!validConfig) {
				throw new RuntimeException("Workspace configuration contains errors.");
			}
		}

		for (ProjectConfiguration p : getProjects()) {
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
			
			// I got an assertion failure when running on the benchmark machine
			// from what appears to be related to cancellation of startup clean
			// and build actions caused by the refactoring being performed and
			// finishing before the startup clean+build is completed.
			//
			// The following hopefully disable auto building at startup.
			//
			// https://github.com/eclipse-platform/eclipse.platform.ui/issues/2340
			// https://github.com/eclipse-platform/eclipse.platform.ui/pull/2441
			// https://www.eclipse.org/forums/index.php/t/94801/
			try {
				IWorkspace ws = ResourcesPlugin.getWorkspace();
				IWorkspaceDescription desc = ws.getDescription();
				desc.setAutoBuilding(false);
				desc.setMaxConcurrentBuilds(1);
				ws.setDescription(desc);
			} catch (CoreException e) {
				e.printStackTrace();
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

	public List<String> getCompilationUnitFilterSet() {
		return this.compilationUnitsFilterList;
	}
}
