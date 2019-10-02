package org.alfine.refactoring.suppliers;

import java.util.Vector;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.opportunities.RefactoringOpportunity;
import org.alfine.refactoring.utils.ASTHelper;
import org.alfine.refactoring.utils.Generator;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class RandomExtractMethodSupplier extends RefactoringSupplier {

	public RandomExtractMethodSupplier(Workspace workspace, Generator generator) {
		super(workspace, generator);
	}

	@Override
	protected Vector<RefactoringOpportunity> collectOpportunities() {

		Vector<RefactoringOpportunity> opportunities = new Vector<>();

		visitCompilationUnits(icu -> {
			CompilationUnit cu = ASTHelper.getCompilationUnit(icu);
			cu.accept(new ExtractMethodVisitor(icu, opportunities));
		});

		return opportunities;
	}
}
