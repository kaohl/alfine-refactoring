package org.alfine.refactoring.suppliers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.alfine.refactoring.utils.ASTHelper;
import org.eclipse.jdt.core.dom.ASTNode;

public class RenameFieldAccessContext extends RefactoringOpportunityContext {
	private final List<String> declContext;

	public RenameFieldAccessContext(ASTNode node) {
		this.declContext = ASTHelper.getDeclarationContext(node);
	}

	@Override
	public Path getContextPath() {
		return Paths.get("rename/field/", String.join("/", this.declContext.subList(0, this.declContext.size() - 1)));
	}
}
