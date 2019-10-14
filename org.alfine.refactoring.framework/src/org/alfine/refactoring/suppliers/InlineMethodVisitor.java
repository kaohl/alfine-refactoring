package org.alfine.refactoring.suppliers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfine.refactoring.opportunities.InlineMethodOpportunity;
import org.alfine.refactoring.opportunities.RefactoringOpportunity;
import org.alfine.refactoring.suppliers.RefactoringSupplier.VectorSupply;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

class InlineMethodVisitor extends ASTVisitor {

	private ICompilationUnit unit;
	private Set<Integer>     oppStartSet; // Source start position for all found opportunities.
	private VectorSupply     supply;

	/** The number of `MethodInvocation' nodes in a traversal of the tree. */
	private long nInvocations;

	public InlineMethodVisitor(ICompilationUnit unit, VectorSupply supply) {
		this.unit         = unit;
		this.oppStartSet  = new HashSet<Integer>();
		this.supply       = supply;
		this.nInvocations = 0;
	}

	public long getNbrInvocations() {
		return this.nInvocations;
	}

	private void addOpportunity(RefactoringOpportunity opp, int start) {

		System.out.print("addOpportunity start = " + start);

		if (!oppStartSet.contains(start)) {
			supply.add(opp);
			oppStartSet.add(start);
		} else {
			System.out.print(", already present.");
		}
		System.out.printf("\n");
	}

	/** Check if `node` is a `MethodInvocation` in which case it is added as an opportunity. */
	private void tryAddNode(ASTNode node) {

		if (node instanceof MethodInvocation) {

			MethodInvocation mi = (MethodInvocation)node;
			IMethodBinding   mb = mi.resolveMethodBinding();

			if (mb == null) {

				// Source is not available...
				// (This happens for standard library and binary
				//  dependencies that does not ship with sources.)

				System.err.println("Unable to resolve method binding for invoked method.");

			} else {

				int modifierFlags = 
						org.eclipse.jdt.core.dom.Modifier.PRIVATE |
						org.eclipse.jdt.core.dom.Modifier.STATIC;

				int modifiers = mb.getModifiers();
				
				boolean isConstructor     = mb.isConstructor();
				boolean isPrivateOrStatic = (modifiers & modifierFlags) != 0;

				// Note: We only consider private or static non-constructor methods for inlining.

				if (!isConstructor && isPrivateOrStatic) {
					addOpportunity(new InlineMethodOpportunity(mb.getJavaElement(), this.unit, mi.getStartPosition(), mi.getLength()), mi.getStartPosition());
				}
			}
		}
	}

	@Override
	public boolean visit(Assignment a) {
		tryAddNode(a.getRightHandSide());
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean visit(VariableDeclarationStatement decl) {
		((List<VariableDeclarationFragment>)decl.fragments()).stream().forEach(f -> {
			tryAddNode(f.getInitializer());
		});
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean visit(VariableDeclarationExpression decl) {
		((List<VariableDeclarationFragment>)decl.fragments()).stream().forEach(f -> {
			tryAddNode(f.getInitializer());
		});
		return true;
	}
	
	@Override
	public boolean visit(SingleVariableDeclaration decl) {
		tryAddNode(decl.getInitializer());
		return true;
	}

	@Override
	public boolean visit(ExpressionStatement exprStat) {
		System.out.println("ExpressionStatement: " + exprStat);
		return true;
	}

	@Override
	public boolean visit(MethodInvocation mi) {

		
		// org.eclipse.jdt.core.dom.rewrite.ASTRewrite; // See documentation string for this entry.
		
		IMethodBinding mb = mi.resolveMethodBinding();

		int modifierFlags = 
			org.eclipse.jdt.core.dom.Modifier.PRIVATE |
			org.eclipse.jdt.core.dom.Modifier.STATIC;

		if (mb != null) {

			boolean isConstructor     = mb.isConstructor();
			boolean isPrivateOrStatic = (mb.getModifiers() & modifierFlags) != 0;

			// Note: We only consider private or static non-constructor methods for inlining.

			if (!isConstructor && isPrivateOrStatic) {
				nInvocations += 1;
			}

			// Note:
			//     The purpose is to count total number of invocations in each
			//     compilation unit so that we can determine how many that are
			//     unavailable for transformation and how many we cannot inline
			//     without first extracting the call into a temporary variable:
			//
			//     int x = 1 + f();
			// -->
			//     int tmp = f();      // We can only inline function calls that are directly assigned to a variable.
			//     int x   = 1 + tmp;
		}

		// This method gives an example of how to use the InlineMethodDescriptor!
		// https://android.wekeepcoding.com/article/20191382/How+to+execute+inline+refactoring+programmatically+using+JDT+LTK%3F

		return true;
	}
}