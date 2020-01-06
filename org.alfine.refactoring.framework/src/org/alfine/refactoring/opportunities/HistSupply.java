package org.alfine.refactoring.opportunities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.alfine.refactoring.suppliers.RefactoringDescriptor;

public class HistSupply implements org.alfine.refactoring.opportunities.Supply {


	public static final HistSupply EMPTY = new HistSupply() {

		private final List<RefactoringDescriptor> opportunities = new ArrayList<>(0);

		@SuppressWarnings("unused")
		public void add(int length, RefactoringOpportunity opp) {
		}

		@Override
		public void shuffle(Random random) {
		}

		@Override
		public Supply merge(Supply supply) throws IllegalArgumentException {
			return this;
		}

		@Override
		public Iterator<RefactoringDescriptor> iterator(Random random) {
			return this.opportunities.iterator();
		}

		@Override
		public int size() {
			return 0;
		}
	};

	private Vector<Vector<RefactoringDescriptor>> matrix;

	public HistSupply(Vector<Vector<RefactoringDescriptor>> matrix) {
		this.matrix = matrix;
	}

	public HistSupply() {
		this.matrix = new Vector<>();
	}

	public void add(RefactoringDescriptor opp) {

		this.matrix.ensureCapacity(opp.histBin() + 1);

		Vector<RefactoringDescriptor> vec = null;
		try {
			vec = this.matrix.elementAt(opp.histBin());
		} catch (ArrayIndexOutOfBoundsException e) {
		}

		if (vec == null) {
			vec = new Vector<>(0);
			this.matrix.add(opp.histBin(), vec);
		}

		vec.add(opp);
	}

	@Override
	public void shuffle(Random random) {
		for (Vector<RefactoringDescriptor> opps : this.matrix) {
			if (opps != null) {
				Supply.shuffle(opps, random);
			}
		}
	}

	@Override
	public Supply merge(Supply supply) throws IllegalArgumentException {

		if (supply instanceof HistSupply) {

			HistSupply other = (HistSupply)supply;

			int size = Math.max(this.matrix.size(), other.matrix.size());

			Vector<Vector<RefactoringDescriptor>> resultM = null;

			resultM = new Vector<>(size);

			for (int i = 0; i < size; ++i) {

				Vector<RefactoringDescriptor> opps = new Vector<>();

				if (i < this.matrix.size() && this.matrix.elementAt(i) != null) {
					opps.addAll(this.matrix.elementAt(i));
				}

				if (i < other.matrix.size() && other.matrix.elementAt(i) != null) {
					opps.addAll(other.matrix.elementAt(i));
				}

				resultM.add(i, opps);
			}

			return new HistSupply(resultM);

		} else {
			throw new IllegalArgumentException("Supplies must be of the same type.");
		}
	}

	private static class HistSupplyIterator implements Iterator<RefactoringDescriptor> {

		private Vector<Vector<RefactoringDescriptor>> matrix;
		private Random random;

		public HistSupplyIterator(HistSupply supply, Random random) {

			Vector<Vector<RefactoringDescriptor>> matrix = new Vector<>();

			// Only keep non-null and non-empty vectors.

			for (Vector<RefactoringDescriptor> opps : supply.matrix) {
				if (opps != null && opps.size() > 0) {

					Vector<RefactoringDescriptor> itOpps = new Vector<>();

					for (RefactoringDescriptor opp : opps) {
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
		public RefactoringDescriptor next() {

			// Note: We do not prevent a data-point from being selected more than once.

			// We only include non-null and non-empty vectors so this
			// test should be sufficient to prevent crash if there are
			// no opportunities available at all.

			if (!(this.matrix.size() > 0))
				return null;

			int vecIndex  = (int)Math.floor(this.random.nextDouble() * this.matrix.size());

			Vector<RefactoringDescriptor> opps = null;
			opps = this.matrix.elementAt(vecIndex);

			int elemIndex = (int)Math.floor(this.random.nextDouble() * opps.size());

			return opps.elementAt(elemIndex);
		}
	}

	@Override
	public Iterator<RefactoringDescriptor> iterator(Random random) {
		return new HistSupplyIterator(this, random);
	}

	@Override
	public int size() {

		int count = 0;

		for (Vector<RefactoringDescriptor> opps : this.matrix) {
			count += opps.size();
		}

		return count;
	}

	@Override
	public Iterator<RefactoringDescriptor> iterator() {
		return iterator(new Random(0));
	}

}

