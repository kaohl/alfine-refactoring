package org.alfine.refactoring.suppliers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.alfine.refactoring.utils.ASTHelper;
import org.eclipse.jdt.core.dom.ASTNode;

public class RenameLocalVariableContext extends RefactoringOpportunityContext {
	private final List<String> declContext;

	public RenameLocalVariableContext(ASTNode node) {
		this.declContext = ASTHelper.getDeclarationContext(node);
	}

	@Override
	public Path getContextPath() {
		return Paths.get("rename/local/", String.join("/", this.declContext));
	}
}
