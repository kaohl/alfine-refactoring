package org.alfine.refactoring.suppliers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ListSupply implements Supply {
	
	private List<RefactoringDescriptor> opportunities;

	public ListSupply() {
		this.opportunities = new ArrayList<>();
	}

	public void add(RefactoringDescriptor opp) {
		this.opportunities.add(opp);
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
	public int size() {
		return this.opportunities.size();
	}
}