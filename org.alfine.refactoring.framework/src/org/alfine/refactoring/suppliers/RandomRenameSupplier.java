package org.alfine.refactoring.suppliers;

import java.util.Vector;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.opportunities.RefactoringOpportunity;
import org.alfine.refactoring.utils.ASTHelper;
import org.alfine.refactoring.utils.Generator;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class RandomRenameSupplier extends RefactoringSupplier {

	public RandomRenameSupplier(Workspace workspace, Generator generator) {
		super(workspace, generator);
	}

	@Override
	protected Supply collectOpportunities() {

		VectorSupply supply = new VectorSupply();

		visitCompilationUnits(icu -> {
			CompilationUnit cu = ASTHelper.getCompilationUnit(icu);
			cu.accept(new RenameVisitor(icu, supply, getGenerator()));
		});

		return supply;
	}
}
