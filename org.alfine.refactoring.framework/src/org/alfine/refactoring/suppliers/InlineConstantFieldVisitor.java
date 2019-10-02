package org.alfine.refactoring.suppliers;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.alfine.refactoring.opportunities.InlineConstantFieldOpportunity;
import org.alfine.refactoring.opportunities.RefactoringOpportunity;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;

public class InlineConstantFieldVisitor extends ASTVisitor {

	private ICompilationUnit               unit;
	private Set<Integer>                   oppStartSet;   // Source start position for all found opportunities.
	private Vector<RefactoringOpportunity> opportunities;

	public InlineConstantFieldVisitor(ICompilationUnit icu, Vector<RefactoringOpportunity> opportunities) {
		this.unit          = icu;
		this.opportunities = opportunities;
		this.oppStartSet   = new HashSet<Integer>();
	}

	private ICompilationUnit getICompilationUnit() {
		return this.unit;
	}

	private void addOpportunity(InlineConstantFieldOpportunity opp, int start) {

		System.out.print("addOpportunity start = " + start);

		if (!oppStartSet.contains(start)) {
			opportunities.add(opp);
			oppStartSet.add(start);
		} else {
			System.out.print(", already present.");
		}
		System.out.printf("\n");
	}

	@Override
	public boolean visit(SimpleName name) {

		IBinding b = name.resolveBinding();

		if (b != null && !name.isDeclaration()) {

			int     modifiers = b.getModifiers();
			boolean isFinal  = (modifiers & org.eclipse.jdt.core.dom.Modifier.FINAL)  > 0;
			boolean isStatic = (modifiers & org.eclipse.jdt.core.dom.Modifier.STATIC) > 0;

			if (isFinal && isStatic) {
				String selection = "" + name.getStartPosition() + " " + name.getLength();

				System.out.println("Add SimpleName: " + name + ", selection = " + selection);

				addOpportunity(
					new InlineConstantFieldOpportunity(getICompilationUnit(), selection),
					name.getStartPosition()
				);
			}
		}
		return true;
	}
}
