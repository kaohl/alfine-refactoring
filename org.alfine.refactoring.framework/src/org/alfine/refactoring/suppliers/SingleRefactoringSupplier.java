package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Iterator;

public class SingleRefactoringSupplier extends RefactoringSupplier {

	private final RefactoringDescriptor descriptor;

	public SingleRefactoringSupplier(RefactoringDescriptor descriptor) {
		this.descriptor = descriptor;
	}

	@Override
	public Iterator<RefactoringDescriptor> iterator() {
		return Collections.singleton(this.descriptor).iterator();
	}

	@Override
	public void cacheOpportunities() {}
}
