package org.alfine.refactoring.suppliers;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jdt.core.dom.ASTNode;

public class RenameTypeContext extends RefactoringOpportunityContext {

	public RenameTypeContext(ASTNode node) {
	}

	@Override
	public Path getContextPath() {
		return Paths.get("rename/type");
	}
}
