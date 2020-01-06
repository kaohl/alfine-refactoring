package org.alfine.refactoring.suppliers;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.utils.ASTHelper;
import org.alfine.refactoring.utils.Generator;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class RandomExtractConstantSupplier extends RefactoringSupplier {

	public RandomExtractConstantSupplier(Workspace workspace, Generator generator) {
		super(workspace, generator);
	}

	@Override
	protected void cacheOpportunities() {

		VectorSupply supply = doCache ? VectorSupply.EMPTY : new VectorSupply();

		visitCompilationUnits(icu -> {
			CompilationUnit cu = ASTHelper.getCompilationUnit(icu);
			cu.accept(new ExtractConstantVisitor(icu, supply));
		});

		return supply;
	}
}
