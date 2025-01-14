package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public interface Supply {

	public static <T extends Comparable<T>> void shuffle(List<T> list, Random random) {
		Collections.shuffle(list, random); // Shuffle using pseudo-random number generator for reproducibility.
	}

	public void shuffle(Random random);

	/** Return iterator over opportunities after shuffling using
	 * 	the specified pseudo-random number generator. */
	public Iterator<RefactoringDescriptor> iterator(Random random);

	/** Return iterator over opportunities (no shuffling will occur). */
	public Iterator<RefactoringDescriptor> iterator();

	/**
	 * @return total number of opportunities in supply.
	 */
	public int size();
	
	
	public static class SupplyIterator implements Iterator<RefactoringDescriptor> {

		private final Random random;
		private List<List<RefactoringDescriptor>> bins;

		public SupplyIterator(List<List<RefactoringDescriptor>> bins, Random random) {
			this.bins = bins;
			this.random = random;
		}

		@Override
		public boolean hasNext() {
			return bins.stream().filter(bin -> !bin.isEmpty()).count() > 0;
		}

		@Override
		public RefactoringDescriptor next() {
			while (this.hasNext()) {
				List<List<RefactoringDescriptor>> nonEmptyBins =
					this.bins.stream().filter(list -> list.size() > 0).collect(Collectors.toList());
				int randomBinIndex = (int)Math.floor(this.random.nextDouble() * nonEmptyBins.size());
				List<RefactoringDescriptor> randomList = this.bins.get(randomBinIndex);
				return randomList.remove((int)Math.floor(this.random.nextDouble() * randomList.size()));
			}
			return null;
		}
	}
}
