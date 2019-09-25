package org.alfine.refactoring.suppliers;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.alfine.refactoring.opportunities.InlineMethodOpportunity;
import org.alfine.refactoring.opportunities.RefactoringOpportunity;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

class InlineVisitor extends ASTVisitor {

	private ICompilationUnit               unit;
	private Set<Integer>                   oppStartSet;   // Source start position for all found opportunities.
	private Vector<RefactoringOpportunity> opportunities;

	public InlineVisitor(ICompilationUnit unit, Vector<RefactoringOpportunity> opportunities) {
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

	/*
	@Override
	public boolean visit(MethodDeclaration decl) {
		IMethodBinding b = decl.resolveBinding();

		return true;
	}
	*/
	
	@Override
	public boolean visit(MethodInvocation mi) {
		

		// This method gives an example of how to use the InlineMethodDescriptor!
		// https://android.wekeepcoding.com/article/20191382/How+to+execute+inline+refactoring+programmatically+using+JDT+LTK%3F
		
		
		

		System.err.println("MethodInvocation: " + mi);

		IMethodBinding mb = mi.resolveMethodBinding();
		
		if (mb == null) {

			// Source is not available...

			System.err.println("Unable to resolve method binding for invoked method.");
		} else {
			addOpportunity(new InlineMethodOpportunity(mb.getJavaElement(), this.unit, mi.getStartPosition(), mi.getLength()), mi.getStartPosition());
		}

		return true;			
	}
}