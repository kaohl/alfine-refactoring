package org.alfine.refactoring.suppliers;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.alfine.refactoring.opportunities.ExtractMethodOpportunity;
import org.alfine.refactoring.opportunities.RefactoringOpportunity;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class ExtractMethodVisitor extends ASTVisitor {
	private ICompilationUnit               unit;
	private Vector<RefactoringOpportunity> opportunities;

	public ExtractMethodVisitor(ICompilationUnit unit, Vector<RefactoringOpportunity> opportunities) {
		this.unit          = unit;
		this.opportunities = opportunities;
	}

	private ICompilationUnit getCompilationUnit() {
		return this.unit;
	}

	private void addOpportunity(RefactoringOpportunity opp) {
		opportunities.add(opp);
	}

	@Override
	public boolean visit(Block block) {
		// Note: This does not cover (non-block) single statement bodies of
		//       control flow statements. But let's ignore that for now.

		int nbrStmts = block.statements().size();

		if (nbrStmts > 0) {
			for (int start = 0; start < nbrStmts; ++start) {
				for (int end = start; end < nbrStmts; ++end) {
					addOpportunity(new ExtractMethodOpportunity(getCompilationUnit(), block, start, end));
					// System.out.println("range = " + start + ", " + end);
				}
			}
		}
		return false;
	}
}
