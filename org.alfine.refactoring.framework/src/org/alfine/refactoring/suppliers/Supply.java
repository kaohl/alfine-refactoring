package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

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
}
