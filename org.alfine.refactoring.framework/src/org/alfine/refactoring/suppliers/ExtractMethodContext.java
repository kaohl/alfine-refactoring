package org.alfine.refactoring.suppliers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.alfine.refactoring.utils.ASTHelper;
import org.eclipse.jdt.core.dom.ASTNode;

public class ExtractMethodContext extends RefactoringOpportunityContext {
	private final List<String> declContext;
	private final int          nstmts;

	public ExtractMethodContext(ASTNode node, int nstmts) {
		this.declContext = ASTHelper.getDeclarationContext(node);
		this.nstmts      = nstmts;
	}

	@Override
	public Path getContextPath() {
		return Paths.get("x-method", String.join("/", this.declContext), String.valueOf(this.nstmts));
	}
}
