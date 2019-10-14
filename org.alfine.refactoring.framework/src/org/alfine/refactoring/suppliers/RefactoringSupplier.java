package org.alfine.refactoring.suppliers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

	//private Supplier<Refactoring> makeSupplierFrom(Vector<RefactoringOpportunity> opportunities) {
	private Supplier<Refactoring> makeSupplierFrom(Supply supply) {
		// We should not have to use the same random generator everywhere as long as
		// we produce deterministic results.

		System.out.println("RefactoringSupplier::makeSupplierFrom()");

		supply.shuffle(new Random(0)); // TODO: This seed should be configurable: option '--shuffle <seed>'

		/*
		for (RefactoringOpportunity opp : opportunities) {
			System.out.println("shuffled opportunity: " + opp);
		}
		*/

		Iterator<RefactoringOpportunity> iter = supply.iterator(new Random(0)); // TODO: This seed should be configurable.

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

		System.out.println("visitCompilationUnits(), roots = ");

		for (IPackageFragmentRoot root : roots) {
			System.out.println("\t" + root.getHandleIdentifier());
		}

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

	protected interface Supply {

		public void shuffle(Random random);

		/**
		 *  Merge two supplies of the same type.
		 * 
		 * @param supply,
		 *     supply to be merged into `this` supply.
		 * @throws IllegalArgumentException
		 *     if specified supply is not of the same type as `this`.
		 */
		public Supply merge(Supply supply) throws IllegalArgumentException;

		public Iterator<RefactoringOpportunity> iterator(Random random);

		/**
		 * @return total number of opportunities in supply.
		 */
		public int size();
	}

	protected static class VectorSupply implements Supply {

		private Vector<RefactoringOpportunity> opportunities;

		public VectorSupply() {
			this.opportunities = new Vector<>();
		}

		@Override
		public void shuffle(Random random) {
			RefactoringSupplier.shuffle(opportunities, random);
		}

		@Override
		public Iterator<RefactoringOpportunity> iterator(Random random) {
			return this.opportunities.iterator();
		}

		@Override
		public Supply merge(Supply supply) throws IllegalArgumentException {
			if (supply instanceof VectorSupply) {
				VectorSupply other = (VectorSupply)supply;
				this.opportunities.addAll(other.opportunities);
			} else {
				throw new IllegalArgumentException("Supplies must be of the same type.");
			}
			return this;
		}

		@Override
		public int size() {
			return this.opportunities.size();
		}

		public void add(RefactoringOpportunity opp) {
			this.opportunities.add(opp);
		}
	}

	protected static class MatrixSupply implements Supply {

		private Vector<Vector<RefactoringOpportunity>> matrix;

		public MatrixSupply(Vector<Vector<RefactoringOpportunity>> matrix) {
			this.matrix = matrix;
		}

		public MatrixSupply() {
			this.matrix = new Vector<>();
		}

		public void add(int length, RefactoringOpportunity opp) {
			this.matrix.ensureCapacity(length - 1);

			Vector<RefactoringOpportunity> vec = null;
			vec = this.matrix.elementAt(length - 1);

			if (vec == null) {
				vec = new Vector<>(0);
				this.matrix.add(length - 1, vec);
			}

			vec.add(opp);
		}

		@Override
		public void shuffle(Random random) {
			for (Vector<RefactoringOpportunity> opps : this.matrix) {
				if (opps != null) {
					RefactoringSupplier.shuffle(opps, random);
				}
			}
		}

		@Override
		public Supply merge(Supply supply) throws IllegalArgumentException {

			if (supply instanceof MatrixSupply) {

				MatrixSupply other = (MatrixSupply)supply;

				int size = Math.max(this.matrix.size(), other.matrix.size());

				Vector<Vector<RefactoringOpportunity>> resultM = null;

				resultM = new Vector<>(size);

				for (int i = 0; i < size; ++i) {

					Vector<RefactoringOpportunity> opps = new Vector<>();

					if (i < this.matrix.size() && this.matrix.elementAt(i) != null) {
						opps.addAll(this.matrix.elementAt(i));
					}

					if (i < other.matrix.size() && other.matrix.elementAt(i) != null) {
						opps.addAll(other.matrix.elementAt(i));
					}

					resultM.add(i, opps);
				}

				return new MatrixSupply(resultM);

			} else {
				throw new IllegalArgumentException("Supplies must be of the same type.");
			}
		}

		private static class MatrixSupplyIterator implements Iterator<RefactoringOpportunity> {

			private Vector<Vector<RefactoringOpportunity>> matrix;
			private Random random;

			public MatrixSupplyIterator(MatrixSupply supply, Random random) {

				Vector<Vector<RefactoringOpportunity>> matrix = new Vector<>();

				// Only keep non-null and non-empty vectors.

				for (Vector<RefactoringOpportunity> opps : supply.matrix) {
					if (opps != null && opps.size() > 0) {

						Vector<RefactoringOpportunity> itOpps = new Vector<>();

						for (RefactoringOpportunity opp : opps) {
							itOpps.add(opp);
						}

						matrix.add(itOpps);
					}
				}

				this.random = random;
				this.matrix = matrix;
			}

			@Override
			public boolean hasNext() {
				return this.matrix.size() > 0;
			}

			@Override
			public RefactoringOpportunity next() {
				Vector<RefactoringOpportunity> opps = null;
				opps = this.matrix.elementAt(this.random.nextInt() % this.matrix.size());
				return opps.elementAt(this.random.nextInt() % opps.size());
			}
		}
		
		@Override
		public Iterator<RefactoringOpportunity> iterator(Random random) {
			return new MatrixSupplyIterator(this, random);
		}

		@Override
		public int size() {

			int count = 0;

			for (Vector<RefactoringOpportunity> opps : this.matrix) {
				count += opps.size();
			}

			return count;
		}
	}

	protected abstract Supply collectOpportunities();
	// protected abstract Vector<RefactoringOpportunity> collectOpportunities();
}
