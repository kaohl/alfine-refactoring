package org.alfine.refactoring.suppliers;

import java.util.Vector;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.opportunities.RefactoringOpportunity;
import org.alfine.refactoring.utils.ASTHelper;
import org.alfine.refactoring.utils.Generator;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class RandomInlineMethodSupplier extends RefactoringSupplier {

	public RandomInlineMethodSupplier(Workspace workspace, Generator generator) {
		super(workspace, generator);
	}

	@Override
	protected Vector<RefactoringOpportunity> collectOpportunities() {

		/*
		IJavaProject project = getProject();

		// Note: We must make sure that imported sources are listed on the classpath in a
		//       deterministic way so that we always get the same order of opportunities.
		//
		// Note: Package fragment roots and package fragments are returned in order of appearance
		//       on the classpath.

		Vector<RefactoringOpportunity> opportunities = new Vector<>();

		try {
			for (IPackageFragment frag : project.getPackageFragments()) {
				for (ICompilationUnit icu : frag.getCompilationUnits()) {
					CompilationUnit cu = ASTHelper.getCompilationUnit(icu);
					cu.accept(new InlineVisitor(icu, opportunities));
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		*/

		Vector<RefactoringOpportunity> opportunities = new Vector<>();

		visitCompilationUnits(icu -> {
			CompilationUnit cu = ASTHelper.getCompilationUnit(icu);
			cu.accept(new InlineVisitor(icu, opportunities));
		});

		return opportunities;
	}
}
