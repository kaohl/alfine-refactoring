package org.alfine.refactoring.suppliers;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.alfine.refactoring.opportunities.RefactoringOpportunity;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class ExtractMethodVisitor extends ASTVisitor {
	private ICompilationUnit               unit;
	private Set<Integer>                   oppStartSet;   // Source start position for all found opportunities.
	private Vector<RefactoringOpportunity> opportunities;

	public ExtractMethodVisitor(ICompilationUnit unit, Vector<RefactoringOpportunity> opportunities) {
		this.unit          = unit;
		this.oppStartSet   = new HashSet<Integer>();
		this.opportunities = opportunities;
	}

	private void addOpportunity(RefactoringOpportunity opp, int start) {

		System.out.print("addOpportunity start = " + start);

		if (!oppStartSet.contains(start)) {
			opportunities.add(opp);
			oppStartSet.add(start);
		} else {
			System.out.print(", already present.");
		}
		System.out.println("");
	}

	@Override
	public boolean visit(MethodDeclaration decl) {
		IMethodBinding b = decl.resolveBinding();

		// Known constraints
		// 1. The first and last statement in the selection must belong to the same block.
		
		return false;
	}

	public int f() {
		int sum = 0;
		for (int i = 0; i < 10; ++i) {
			sum += i;
			System.out.println("sum is " + sum);
		}
		return sum;
	}
}
