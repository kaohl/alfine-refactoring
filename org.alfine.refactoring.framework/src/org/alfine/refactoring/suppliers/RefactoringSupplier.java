package org.alfine.refactoring.suppliers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.alfine.refactoring.framework.Workspace;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.Refactoring;

public abstract class RefactoringSupplier
	implements Iterable<RefactoringDescriptor> {

	private final Workspace workspace;
	private long           shuffleSeed;
	private long           selectSeed;

	public RefactoringSupplier(Workspace workspace) {
		this.workspace = workspace;
	}

	protected Workspace getWorkspace() {
		return this.workspace;
	}

	/** Access workspace cache. */
	protected Cache getCache() {
		return getWorkspace().getCache();
	}

	public void setShuffleSeed(long shuffleSeed) {
		this.shuffleSeed = shuffleSeed;
	}

	public void setSelectSeed(long selectSeed) {
		this.selectSeed = selectSeed;
	}

	protected long getSelectSeed() {
		return this.selectSeed;
	}

	protected long getShuffleSeed() {
		return this.shuffleSeed;
	}

	/** Return sorted list of source roots. (The resulting list should be deterministic.)*/
	protected List<IPackageFragment> getSortedVariableSourceFragments() {
		return getWorkspace().getVariableSourceFragments().stream()
				.sorted(Comparator.comparing(IPackageFragment::getHandleIdentifier))
				.collect(Collectors.toList());
	}

	public Supplier<Refactoring> getSupplier() {

		System.out.println("RefactoringSupplier::getSupplier()");

		Iterator<RefactoringDescriptor> iter = iterator();

		return new Supplier<Refactoring>() {

			@Override
			public Refactoring get() {
				
				System.out.println("Supplier::get()");

				RefactoringDescriptor opp = null;
				Refactoring           ref = null;

				while (iter.hasNext()) {

					System.out.println("Trying to supply a refactoring...");

					if ((opp = iter.next()) != null) {

						ref = opp.getRefactoring();

						if (ref != null) {
							// System.out.println(" succeeded. Supplying refactoring of element: " + opp.getElement());
							break;
						} else {
							System.out.println(" failed.");
						}
					}
				}

				if (ref == null) {
					System.out.println("Supplier is empty. No more refactorings to supply.");
				}

				return ref;
			}
		};
	}

	public abstract void cacheOpportunities();

	protected void visitCompilationUnits(Consumer<? super ICompilationUnit> action) {

		List<IPackageFragment> roots = getSortedVariableSourceFragments();
		
		System.out.println("visitCompilationUnits(), roots = ");

		for (IPackageFragment root : roots) {
			System.out.println("\t" + root.getHandleIdentifier());
		}

		List<ICompilationUnit> units = roots.stream()
		.sorted((x,y) -> x.getElementName().compareTo(y.getElementName()))
		.flatMap(f -> {
			try {
				return Arrays.asList(f.getCompilationUnits()).stream();
			} catch (JavaModelException e) {}
			return java.util.stream.Stream.empty();
		})
		.collect(Collectors.toList());
		
		String path = "visited-classes.txt";

		try (BufferedWriter out =
				Files.newBufferedWriter(
					Paths.get(path),
					StandardCharsets.UTF_8,
					java.nio.file.StandardOpenOption.CREATE)) {
			units.forEach(u -> {
				try {
					out.write(u.toString());
					out.newLine();
				} catch (IOException e1) {
						e1.printStackTrace();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		units.forEach(action);
	}
}
