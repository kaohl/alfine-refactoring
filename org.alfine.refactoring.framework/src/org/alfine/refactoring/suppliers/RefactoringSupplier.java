package org.alfine.refactoring.suppliers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.alfine.refactoring.framework.Workspace;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

public abstract class RefactoringSupplier
	implements Iterable<RefactoringDescriptor> {

	private final Workspace workspace;
	private long           shuffleSeed;
	private long           selectSeed;

	public RefactoringSupplier() {
		this.workspace = null;
	}
	
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

//		TODO: Make sure cache and refactoring supplier does not sort opportunities; just pick an index from the file.
		
		Iterator<RefactoringDescriptor> iter = iterator();

		return new Supplier<Refactoring>() {

			@Override
			public Refactoring get() {
				
				System.out.println("Supplier::get()");

				RefactoringDescriptor opp = null;
				Refactoring           ref = null;
				
//				while (iter.hasNext() &&
//						((opp = iter.next()) != null || iter.hasNext()) &&
//						((opp == null) || (ref = opp.getRefactoring()) == null));
				
				while (ref == null && iter.hasNext()) {

					System.out.println("Trying to supply a refactoring...");
					
					if ((opp = iter.next()) != null) {
						ref = opp.getRefactoring();
//						if (ref != null) {
//							// System.out.println(" succeeded. Supplying refactoring of element: " + opp.getElement());
//							break;
//						} else {
//							System.out.println(" failed.");
//						}
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
//		.filter(u -> {
//			// Temporary filter to look at the only class in JaCoP that contains "private static" methods...
//          // YOU MUST ALSO UPDATE THE `packages.config` file correspondingly.
//			String path = u.getPath().removeFirstSegments(2).toString();
//			boolean result = "org/jacop/constraints/netflow/Arithmetic.java".equals(path);
//			System.out.println("path = " + path + ", result = " + result);
//
//			return result;
//		})
		.collect(Collectors.toList());
		
		getWorkspace().writeUnitConfigHelper(units.stream().map(RefactoringSupplier::getPackageNameFromUnit).collect(Collectors.toList()));
		
		units = units.stream().filter(getCompilationUnitFilter()).collect(Collectors.toList());
		
		// We don't need this printout now that we have `unit.config.helper`.
		/*
		String path = "visited-compilation-units.txt";

		try (BufferedWriter out =
				Files.newBufferedWriter(
					Paths.get(path),
					StandardCharsets.UTF_8,
					java.nio.file.StandardOpenOption.CREATE)) {
			units.forEach(u -> {
				try {
					out.write("" + u.getPath());
					out.newLine();
				} catch (IOException e1) {
						e1.printStackTrace();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		*/
		units.forEach(action);
	}
	
	private static String getPackageNameFromUnit(ICompilationUnit unit) {
		return unit.getPath().removeFirstSegments(2).toString();
	}
	
	private Predicate<? super ICompilationUnit> getCompilationUnitFilter() {
		Set<String> acceptedUnits = new HashSet<String>(getWorkspace().getCompilationUnitFilterSet());
		if (acceptedUnits != null && acceptedUnits.size() > 0) {
			return new Predicate<ICompilationUnit>() {
				@Override
				public boolean test(ICompilationUnit u) {
					String path = RefactoringSupplier.getPackageNameFromUnit(u);
					boolean result = acceptedUnits.contains(path);
					System.out.println("unit filter path = " + path + ", accept = " + result);
					return result;
				}
			};
		} else {
			System.out.println("Using default unit filter (accept all).");
			return new Predicate<ICompilationUnit>() {
				@Override
				public boolean test(ICompilationUnit u) {
					return true;
				}
			};
		}
	}
}
