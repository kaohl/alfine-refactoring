package org.alfine.refactoring.opportunities;

import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import org.alfine.refactoring.suppliers.RefactoringDescriptor;

public interface Supply {

	public static <T extends Comparable<T>> void shuffle(Vector<T> list, Random random) {
		Collections.shuffle(list, random); // Shuffle using pseudo number-generator for reproducibility.
	}

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
