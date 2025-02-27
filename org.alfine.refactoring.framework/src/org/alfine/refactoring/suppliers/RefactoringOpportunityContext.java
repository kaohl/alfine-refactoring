package org.alfine.refactoring.suppliers;

import java.nio.file.Path;

public abstract class RefactoringOpportunityContext {
	public RefactoringOpportunityContext() {}
	public abstract Path getContextPath();
}
