package org.alfine.refactoring.suppliers;

import org.alfine.refactoring.opportunities.ExtractMethodOpportunity;
import org.alfine.refactoring.opportunities.RefactoringOpportunity;
import org.alfine.refactoring.suppliers.RefactoringSupplier.MatrixSupply;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;

public class ExtractMethodVisitor extends ASTVisitor {
	private ICompilationUnit unit;
	private MatrixSupply     supply;

	public ExtractMethodVisitor(ICompilationUnit unit, MatrixSupply supply) {
		this.unit   = unit;
		this.supply = supply;
	}

	private ICompilationUnit getCompilationUnit() {
		return this.unit;
	}

	private void addOpportunity(int length, RefactoringOpportunity opp) {
		supply.add(length, opp); // Note: `length` is reduced to `length` - 1 internally.
	}

	@Override
	public boolean visit(Block block) {
		// Note: This does not cover (non-block) single statement bodies of
		//       control flow statements. But let's ignore that for now.

		int nbrStmts = block.statements().size();

		if (nbrStmts > 0) {
			for (int start = 0; start < nbrStmts; ++start) {
				for (int end = start; end < nbrStmts; ++end) {
					int length = end - start + 1;
					addOpportunity(length, new ExtractMethodOpportunity(getCompilationUnit(), block, start, end));
					// System.out.println("range = " + start + ", " + end + ", length = " + length);
				}
			}
		}
		return false;
	}
}
