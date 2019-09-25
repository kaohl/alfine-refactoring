package org.alfine.refactoring.processors;

import java.util.function.Supplier;

import org.alfine.refactoring.suppliers.RefactoringSupplier;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

public class RefactoringProcessor {

	private final RefactoringSupplier refactoringSupplier;

	public RefactoringProcessor(RefactoringSupplier supplier) {
		this.refactoringSupplier = supplier;
	}

	public boolean processSupply(int drop, int limit) {

		// Apply one refactoring at a time until one is successful.

		boolean               success     = false;
		Supplier<Refactoring> supplier    = null;
		Refactoring           refactoring = null;

		System.out.println("Processing supply...\n\t drop = " + drop + ",\n\t limit = " + limit + "");

		supplier = refactoringSupplier.getSupplier();

		int i = drop;

		while ((i > 0) && (refactoring = supplier.get()) != null) {
			--i;
			
			System.out.println("dropping refactoring: " + refactoring);
		}

		if (!(limit > 0)) {
			System.out.println("Limit (`--limit') is set to zero. No attempts will be made.");
		}

		for (int attempt = 0; (!success && (attempt < limit)); ++attempt) {
			
			System.out.println("Refactoring attempt " + attempt);
			
			// Example use: drop n limit 1 to select and try the (n + 1)th refactoring only.

			if ((refactoring = supplier.get()) != null) {
				success = applyRefactoring(refactoring);
			} else {

				int nbrAvailable = drop + attempt - i;

				System.out.println(
					"Out of refactorings ("
					+ "attempt=" + attempt
					+ ", drop=" + drop
					+ ", limit=" + limit
					+ "; number available was " + nbrAvailable + ".");

				if (drop > 0) {
					System.out.println("You could try reducing the value of the 'drop' option.");
				}
				
				break;
			}
		}
		
		if (success) {
			System.out.println("One or more refactorings were applied.");
		} else {
			System.out.println("No refactorings were applied.");
		}
		
		return success;
	}
	
	public static boolean applyRefactoring(Refactoring refactoring) {
		//
		// Is this useful?
		// RefactoringASTParser p; p.parse(typeRoot, owner, resolveBindings, statementsRecovery, bindingsRecovery, pm);
	
		// We can get undo here... (But we do not want to execute in a new thread...)
		// final int style = org.eclipse.ltk.core.refactoring.CheckConditionsOperation.ALL_CONDITIONS;
		// PerformRefactoringOperation prop = new PerformRefactoringOperation(refactoring, style);

		System.out.println("RefactoringProcessor::applyRefactoring()");


		
		try {

			RefactoringStatus status = refactoring.checkAllConditions(new NullProgressMonitor());

			if (status.hasEntries()) {
				int i = 0;
				for (RefactoringStatusEntry e : status.getEntries()) {
					System.out.printf("RefactoringStatusEntry[%d] = %s\n", i++, e);
				}
			}

			
			// RefactoringStatus status = refactoring.checkAllConditions(new NullProgressMonitor());

			Change change = refactoring.createChange(new NullProgressMonitor());

			System.out.println("ChangeDescriptor: " + change.getDescriptor().toString());

			try {

				Object o = change.perform(new NullProgressMonitor());

				if (o == null) {
					return false; // Failed to apply change.
				}
				
			} finally {
				change.dispose();				
			}

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
		
		
		
		
		
		
		/*
		
		
		
		
		boolean result = false;
	
		try {
			
			RefactoringStatus status = refactoring.checkAllConditions(new NullProgressMonitor());

			if (status.hasError()) {
				throw new Exception("Refactoring status has errors: " + status);
			}

			Change change = refactoring.createChange(new NullProgressMonitor());

			try {

				change.initializeValidationData(new NullProgressMonitor());

				if (!change.isEnabled()) {
					// log("Change is not enabled: " + change);
					throw new Exception("Change is not enabled: "  + change);
				}

				RefactoringStatus valid = change.isValid(new NullProgressMonitor());

				if (valid.hasError()) {
					// log("Change validity has errors: " + valid);
					throw new Exception("Change validity has errors: " + valid);
				}

				Object o = change.perform(new NullProgressMonitor());

				if (o == null) {
					// log("Failed to apply change!");
					throw new Exception("Failed to apply change.");
				}

				result = true;

			} finally {
				change.dispose();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
		 */
	}

}
