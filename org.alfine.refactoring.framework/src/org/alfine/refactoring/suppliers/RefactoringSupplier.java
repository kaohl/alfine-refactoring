package org.alfine.refactoring.suppliers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.alfine.refactoring.framework.Project;
import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.opportunities.RefactoringOpportunity;
import org.alfine.refactoring.utils.Generator;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.Refactoring;

public abstract class RefactoringSupplier {

	/* TODO: Consider removing subclasses and instead use visitor as parameter. */

	private final Workspace workspace;
	private final Generator generator;

	public RefactoringSupplier(Workspace workspace, Generator generator) {
		this.workspace = workspace;
		this.generator = generator;
	}

	protected Workspace getWorkspace() {
		return this.workspace;
	}

	protected Generator getGenerator() {
		return this.generator;
	}

	protected List<IPackageFragmentRoot> getSortedVariableSourceRoots() {
		return getWorkspace().getVariableSourceRoots()
				.stream()
				.sorted()
				.collect(Collectors.toList());
	}

	private static <T extends Comparable<T>> void shuffle(Vector<T> list, Random random) {
		
		// TODO: This initial sort is not needed since they already are in the order in which they were crerated.
		// We must make sure that they are always created in the same order or use a naming-scheme which enforce
		// an absolute order on all opportunitites. We should use some sort of extended fully qualified name which
		// also uniquely names local variables. We should make use of elements´ start position.

		list.sort(null);                 // Force an initial absolute order (The order in which they are created.)
		Collections.shuffle(list, random); // Shuffle using number generator for reproducibility.
	}

	private Supplier<Refactoring> makeSupplierFrom(Vector<RefactoringOpportunity> opportunities) {

		// We should not have to use the same random generator everywhere as long as
		// we produce deterministic results.

		System.out.println("RefactoringSupplier::makeSupplierFrom()");
		
		shuffle(opportunities, new Random(0)); // TODO: This seed should be configurable: option '--shuffle <seed>'

		for (RefactoringOpportunity opp : opportunities) {
			System.out.println("shuffled opportunity: " + opp);
		}

		Iterator<RefactoringOpportunity> iter = opportunities.iterator();

		return new Supplier<Refactoring>() {

			@Override
			public Refactoring get() {
				
				System.out.println("Supplier::get()");

				RefactoringOpportunity opp = null;
				Refactoring            ref = null;

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

	public Supplier<Refactoring> getSupplier() {
		return makeSupplierFrom(collectOpportunities());
	}

	protected void visitCompilationUnits(Consumer<? super ICompilationUnit> action) {

		List<IPackageFragmentRoot> roots = getSortedVariableSourceRoots();
		
		roots.stream()
		.sorted((x,y) -> x.getElementName().compareTo(y.getElementName()))
		.flatMap(r -> {
			try {
				return Arrays.asList(r.getChildren()).stream();
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
			return java.util.stream.Stream.empty();
		})
		.filter (e -> e.getElementType() == IJavaElement.PACKAGE_FRAGMENT)
		.map   (e -> (IPackageFragment)e)
		.sorted((x,y) -> x.getElementName().compareTo(y.getElementName()))
		.flatMap(f -> {
			try {
				return Arrays.asList(f.getCompilationUnits()).stream();
			} catch (JavaModelException e) {}
			return java.util.stream.Stream.empty();
		})
		.forEach(action);
	}

	protected abstract Vector<RefactoringOpportunity> collectOpportunities();
}
