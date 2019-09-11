package org.alfine.refactoring.opportunities;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.InlineMethodDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public abstract class RefactoringOpportunity implements Comparable<RefactoringOpportunity> {

	// Unique ID identifying and sorting refactorings.

	private static int idCounter = 0;
	private        int id        = ++idCounter;

	private IJavaElement element;

	public RefactoringOpportunity(IJavaElement element) {
		this.element = element;
	}

	public int getId() {
		return this.id;
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof RefactoringOpportunity)
				&& getId() == ((RefactoringOpportunity)other).getId();
	}

	@Override
	public int compareTo(RefactoringOpportunity other) {
		return Integer.compare(getId(), other.getId());
	}

	protected IJavaElement getElement() {
		return this.element;
	}

/*	
	private void exists(String id) {
		if (RefactoringCore.getRefactoringContribution(id) != null) {
			System.out.println(id);
		}		
	}
*/

	/** Create a new JavaRefactoringDescriptor of the specified kind. (See IJavaRefactorings for available ids.) */
	protected JavaRefactoringDescriptor getDescriptor(String id) {

		RefactoringContribution contribution = RefactoringCore.getRefactoringContribution(id);

		/*
		exists(IJavaRefactorings.RENAME_COMPILATION_UNIT);
		exists(IJavaRefactorings.RENAME_METHOD);
		exists(IJavaRefactorings.RENAME_FIELD);
		exists(IJavaRefactorings.RENAME_LOCAL_VARIABLE);
		exists(IJavaRefactorings.RENAME_TYPE);
		exists(IJavaRefactorings.RENAME_TYPE_PARAMETER);
		exists(IJavaRefactorings.RENAME_ENUM_CONSTANT);
		exists(IJavaRefactorings.RENAME_PACKAGE);
		*/
		
		if (contribution == null) {
			System.err.println("No refactoring contribution!");
			throw new RuntimeException("No refactoring contribution");
		}

		JavaRefactoringDescriptor descriptor = (JavaRefactoringDescriptor)contribution.createDescriptor();

		// org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor
		

		if (descriptor == null) {
			System.err.println("No refactoring descriptor for `" + id + "'!");
			throw new RuntimeException("No refactoring descriptor for `" + id + "'");
		}

		return descriptor;
	}
	
	protected abstract JavaRefactoringDescriptor buildDescriptor();

	/** Create refactoring from descriptor. (Override this method to create a refactoring without using the descriptor.)*/
	protected Refactoring createRefactoring() {

		JavaRefactoringDescriptor descriptor = buildDescriptor();

		if (descriptor.validateDescriptor().hasFatalError()) {
			System.err.println("Missing args! Not all relevant parameters set for refactoring!\n");
			return null;
		}

		RefactoringStatus status      = new RefactoringStatus();
		Refactoring       refactoring = null;

		try {
			refactoring = descriptor.createRefactoring(status);
		} catch (CoreException e) {
			e.printStackTrace();
		}

		if (status.hasError()) {
			System.out.print("Initial refactoring status has errors: " + status);
			return null;
		}

		return refactoring;
	}

	public Refactoring getRefactoring() {
		return createRefactoring();
	}
}
