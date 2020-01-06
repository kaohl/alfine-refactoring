package org.alfine.refactoring.suppliers;

import java.util.Vector;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.opportunities.RefactoringOpportunity;
import org.alfine.refactoring.suppliers.RefactoringSupplier.VectorSupply;
import org.alfine.refactoring.utils.ASTHelper;
import org.alfine.refactoring.utils.Generator;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class RandomInlineConstantFieldSupplier extends RefactoringSupplier {

	public RandomInlineConstantFieldSupplier(Workspace workspace, Generator generator) {
		super(workspace, generator);
	}

	@Override
	protected void cacheOpportunities() {

		VectorSupply supply = doCache ? VectorSupply.EMPTY : new VectorSupply();

		if (doCache) {
			visitCompilationUnits(icu -> {
				CompilationUnit cu = ASTHelper.getCompilationUnit(icu);
				cu.accept(new InlineConstantFieldVisitor(icu, supply));
			});
		} else {
			// TODO: load cache from file.
		}
		return supply;
	}
}
