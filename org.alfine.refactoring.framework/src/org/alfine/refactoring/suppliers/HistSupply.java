package org.alfine.refactoring.suppliers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class HistSupply implements org.alfine.refactoring.suppliers.Supply {

	/** Lazily populated histogram. (We do not know the absolute size of the
	 *  base array when we traverse a set of source files or load refactoring
	 *  descriptors from file so we use a map and populate it lazily with
	 *  opportunity lists (bins) and opportunities (descriptors).) */
	private Map<Integer, List<RefactoringDescriptor>> matrix;

	public HistSupply() {
		this.matrix = new HashMap<>();
	}

	public void add(RefactoringDescriptor opp) {
		if (!this.matrix.containsKey(opp.histBin())) {
			this.matrix.put(opp.histBin(), new ArrayList<>());
		}
		this.matrix.get(opp.histBin()).add(opp);
	}

	@Override
	public int size() {

		int count = 0;

		for (Map.Entry<Integer, List<RefactoringDescriptor>> entry : this.matrix.entrySet()) {
			count += entry.getValue().size();
		}

		return count;
	}

	@Override
	public void shuffle(Random random) {
		this.matrix.entrySet().parallelStream()
		.forEach(entry -> {
			Supply.shuffle(entry.getValue(), random);
		});
	}

	@Override
	public Iterator<RefactoringDescriptor> iterator(Random random) {
		shuffle(random);
		return new HistSupplyIterator(this, random);
	}

	@Override
	public Iterator<RefactoringDescriptor> iterator() {
		return iterator(new Random(0));
	}

	private static class HistSupplyIterator implements Iterator<RefactoringDescriptor> {

		private final HistSupply  supply;
		private final Random      random;
		private final Map<Integer, Iterator<RefactoringDescriptor>> iterators;

		public HistSupplyIterator(HistSupply supply, Random random) {
			this.supply = supply;
			this.random = random;
			this.iterators = new TreeMap<>();

			for (Map.Entry<Integer, List<RefactoringDescriptor>> entry : this.supply.matrix.entrySet()) {
				if (!entry.getValue().isEmpty()) {
					this.iterators.put(entry.getKey(), entry.getValue().iterator());
				}
			}
		}

		@Override
		public boolean hasNext() {
			return this.supply.size() > 0;
		}

		@Override
		public RefactoringDescriptor next() {
			while (this.iterators.size() != 0) {

				// The key-set stream over `iterators` is
				// always sorted since we use a tree map.

				int nbrIters        = this.iterators.size();
				int randomIterIndex = (int)Math.floor(this.random.nextDouble() * nbrIters);
				int iterKey         = this.iterators.keySet().stream().collect(Collectors.toList()).get(randomIterIndex);

				Iterator<RefactoringDescriptor> iter = this.iterators.get(iterKey);

				try {
					return iter.next();
				} catch (NoSuchElementException e) {
					this.iterators.remove(iterKey);
				}
			}
			return null;
		}
	}
}
