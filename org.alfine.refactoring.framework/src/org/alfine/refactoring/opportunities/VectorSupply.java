package org.alfine.refactoring.opportunities;

import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import org.alfine.refactoring.suppliers.RefactoringDescriptor;

public class VectorSupply implements Supply {
	
	private Vector<RefactoringDescriptor> opportunities;

	public VectorSupply() {
		this.opportunities = new Vector<>();
	}

	@Override
	public void shuffle(Random random) {
		Supply.shuffle(this.opportunities, random);
	}

	@Override
	public Iterator<RefactoringDescriptor> iterator() {
		return this.opportunities.iterator();
	}

	@Override
	public Iterator<RefactoringDescriptor> iterator(Random random) {
		shuffle(random);
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

	public void add(RefactoringDescriptor opp) {
		this.opportunities.add(opp);
	}
}