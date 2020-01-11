package org.alfine.refactoring.opportunities;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.alfine.refactoring.suppliers.Cache;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContext;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

public abstract class RefactoringOpportunity implements Comparable<RefactoringOpportunity> {

	// Unique ID identifying and sorting refactorings.

	private static int idCounter = 0;
	private        int id        = ++idCounter;

	private IJavaElement element;

	/** This constructor is meant to be used in
	 *  conjunction with `restoreFromCacheLine()`.*/
	public RefactoringOpportunity() {
		this.element = null;
	}

	public RefactoringOpportunity(IJavaElement element) {
		this.element = element;
	}

	public int getId() {
		return this.id;
	}

	/** Return cache file path for deriving type.
	 *  (This method should be overridden by derived types.) */
	public Path getCachePath() {
		return Paths.get(Cache.KEY_CACHE_DEFAULT);
	}

	/** Create and return cache file entry.
	 *  (This method should be overridden by derived types.)
	 *  TODO: This method should be abstract when all derived types have
	 *        been refactored and produce valid cache-lines.) */
	public abstract String getCacheLine();

	/** Cache this opportunity in preconfigured location. */
	public void cache() {
		Cache.write(getCachePath(), getCacheLine());
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


		// TODO: We must call the other version of `createDescriptor()' to be able to set refactoring arguments!
		//       This method `getDescriptor()' must also use a provided argument map for the descriptor which is
		//       to be constructed during source traversal by "Visitors" and passed as parameter to opportunities.
		//       Therefore, an opportunity is a tuple: (id : String, args : Map<String, String>)
		// Example: (IJavaRefactoring.INLINE_METHOD, new HashMap<String, String>())

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

		RefactoringStatus status = descriptor.validateDescriptor();

		for (RefactoringStatusEntry entry : status.getEntries()) {
			System.out.println("RefactoringStatusEntry from `validateDescriptor()':\n" + entry.toString());
		}

		if (status.hasFatalError()) {
			System.err.println("Invalid descriptor (FATAL).\n" + status);
			return null;
		}

		Refactoring refactoring = null;

		try {
			RefactoringContext ctx    = null;

			status = new RefactoringStatus();

			ctx = descriptor.createRefactoringContext(status);

			for (RefactoringStatusEntry entry : status.getEntries()) {
				System.out.println("RefactoringStatusEntry:\n" + entry.toString());
			}

			if (status.hasError()) {
				System.out.println("Status has errors. Refactoring can not be created.");
				return null;
			}

			refactoring = ctx.getRefactoring();

		} catch (CoreException e) {
			e.printStackTrace();
		}

		return refactoring;
	}

	public Refactoring getRefactoring() {
		return createRefactoring();
	}
}
