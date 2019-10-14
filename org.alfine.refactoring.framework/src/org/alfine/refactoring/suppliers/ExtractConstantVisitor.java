package org.alfine.refactoring.suppliers;

import org.alfine.refactoring.opportunities.ExtractConstantOpportunity;
import org.alfine.refactoring.opportunities.RefactoringOpportunity;
import org.alfine.refactoring.suppliers.RefactoringSupplier.Supply;
import org.alfine.refactoring.suppliers.RefactoringSupplier.VectorSupply;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.StringLiteral;

public class ExtractConstantVisitor extends ASTVisitor  {
	private ICompilationUnit unit;
	private VectorSupply     supply;

	private static final String NAME = "_ALFINE_CONSTANT_";

	public ExtractConstantVisitor(ICompilationUnit unit, VectorSupply supply) {
		this.unit   = unit;
		this.supply = supply;
	}

	private ICompilationUnit getCompilationUnit() {
		return this.unit;
	}

	private void addOpportunity(RefactoringOpportunity opp) {
		supply.add(opp);
	}

	@Override
	public boolean visit(BooleanLiteral literal) {
		String selection = "" + literal.getStartPosition() + " " + literal.getLength();
		System.out.println("BooleanLiteral: " + literal + ", selection = " + selection);
		addOpportunity(new ExtractConstantOpportunity(getCompilationUnit(), NAME, selection));
		return false;
	}

	@Override
	public boolean visit(CharacterLiteral literal) {
		String selection = "" + literal.getStartPosition() + " " + literal.getLength();
		System.out.println("CharacterLiteral: " + literal + ", selection = " + selection);
		addOpportunity(new ExtractConstantOpportunity(getCompilationUnit(), NAME, selection));
		return false;
	}

	@Override
	public boolean visit(NumberLiteral literal) {
		String selection = "" + literal.getStartPosition() + " " + literal.getLength();
		System.out.println("NumberLiteral: " + literal + ", selection = " + selection);
		addOpportunity(new ExtractConstantOpportunity(getCompilationUnit(), NAME, selection));
		return false;
	}

	@Override
	public boolean visit(StringLiteral literal) {

		// The difference between extracting a constant into a field with private, protected or public visibility
		// is that the API for the class is extended unless the field is private. As opposed to public and protected
		// constants, constants with private visibility can in theory be eliminated since they cannot be referenced
		// from outside of the corresponding compilation unit.

		// We should want to extract constants in all cases. However, there is a risk that we will get an
		// over-representation of constants extracted from other constants since it is common-practice to
		// extract constants into variables as opposed to using literal values directly in procedures. An
		// exception to this observation is probably string literals passed to io-routines.
		// static int X  = _X;
		// static int _X = 0;

		/*
		if (literal.getParent() instanceof VariableDeclarationFragment) {

			VariableDeclarationFragment vdf = (VariableDeclarationFragment)literal.getParent();
			IVariableBinding            b   = vdf.resolveBinding();

			if (b != null && b.getDeclaringMethod() != null) {
				// Since there is a surrounding method declaration we know that the literal
				// does not occur as an initializer to a field.
				//
				// TODO: Consider if we need to make this distinction.
			}
		}
		*/

		String selection = "" + literal.getStartPosition() + " " + literal.getLength();
		System.out.println("StringLiteral: " + literal + ", selection = " + selection);
		addOpportunity(new ExtractConstantOpportunity(getCompilationUnit(), NAME, selection));
		return false;
	}

}
