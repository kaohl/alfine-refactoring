package org.alfine.refactoring.suppliers;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
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

public class InlineMethodVisitor extends ASTVisitor {

	/** Cache for refactoring descriptors.*/
	private Cache            cache;

	private ICompilationUnit unit;
	private Set<Integer>     oppStartSet; // Source start position for all found opportunities.

	public static long nInvocations = 0;

	public InlineMethodVisitor(Cache cache, ICompilationUnit unit) {
		this.cache        = cache;
		this.unit         = unit;
		this.oppStartSet  = new HashSet<Integer>();
	}

	private void addOpportunity(RefactoringDescriptor descriptor, int start) {
		if (!oppStartSet.contains(start)) {
			oppStartSet.add(start);
			cache.write(descriptor);
		}
	}

	private InlineMethodDescriptor createInlineMethodDescriptor(IJavaElement element, int start, int length) {
		Map<String, String> args = new TreeMap<>();
		args.put("input", this.unit.getHandleIdentifier());
		args.put("element", element.getHandleIdentifier());
		args.put("selection", "" + start + " " + length);
		return new InlineMethodDescriptor(args);
	}

	/** Check if `node` is a `MethodInvocation` in which case it is added as an opportunity. */
	private void tryAddNode(ASTNode node) {

		if (node instanceof MethodInvocation) {

			MethodInvocation mi = (MethodInvocation)node;
			IMethodBinding   mb = mi.resolveMethodBinding();

			if (mb == null) {

				// Source is not available...
				// (This happens for standard library and binary
				//  dependencies for which source is unavailable.)

				System.err.println("Unable to resolve method binding for invoked method.");

			} else {

				int modifierFlags = 
					org.eclipse.jdt.core.dom.Modifier.PRIVATE |
					org.eclipse.jdt.core.dom.Modifier.STATIC;

				int modifiers = mb.getModifiers(); // Bitwise or of modifier constants.
				
				boolean isConstructor     = mb.isConstructor();
				boolean isPrivateOrStatic = (modifiers & modifierFlags) != 0;

				// Note: We only consider private or static non-constructor methods for inlining.

				if (!isConstructor && isPrivateOrStatic) {
					//addOpportunity(new InlineMethodOpportunity(mb.getJavaElement(), this.unit, mi.getStartPosition(), mi.getLength()), mi.getStartPosition());
					addOpportunity(
						createInlineMethodDescriptor(
							mb.getJavaElement(),
							mi.getStartPosition(),
							mi.getLength()
						),
						mi.getStartPosition()
					);
				}
			}
		}
	}

//	@Override
//	public boolean visit(Assignment a) {
//		tryAddNode(a.getRightHandSide());
//		return true;
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public boolean visit(VariableDeclarationStatement decl) {
//		((List<VariableDeclarationFragment>)decl.fragments()).stream().forEach(f -> {
//			tryAddNode(f.getInitializer());
//		});
//		return true;
//	}
//
//	@Override
//	@SuppressWarnings("unchecked")
//	public boolean visit(VariableDeclarationExpression decl) {
//		((List<VariableDeclarationFragment>)decl.fragments()).stream().forEach(f -> {
//			tryAddNode(f.getInitializer());
//		});
//		return true;
//	}
//	
//	@Override
//	public boolean visit(SingleVariableDeclaration decl) {
//		tryAddNode(decl.getInitializer());
//		return true;
//	}
//	
//	@Override
//	public boolean visit(ExpressionStatement exprStat) {
//		// Visit children by returning true.
//		
//		
//		for (int i = 1 + f(); i < 19; i++) {
//			System.out.println("i = " + i);
//		}
//		
//		return true;
//	}

	@Override
	public boolean visit(MethodInvocation mi) {
		tryAddNode(mi);
		return true;
	}
	
//	@Override
//	public boolean visit(MethodInvocation mi) {
//
//		// org.eclipse.jdt.core.dom.rewrite.ASTRewrite; // See documentation string for this entry.
//		
//		IMethodBinding mb = mi.resolveMethodBinding();
//
//		int modifierFlags = 
//			org.eclipse.jdt.core.dom.Modifier.PRIVATE |
//			org.eclipse.jdt.core.dom.Modifier.STATIC;
//
//		if (mb != null) {
//
//			boolean isConstructor     = mb.isConstructor();
//			boolean isPrivateOrStatic = (mb.getModifiers() & modifierFlags) != 0;
//
//			// Note: We only consider private or static non-constructor methods for inlining.
//
//			if (!isConstructor && isPrivateOrStatic) {
//				InlineMethodVisitor.nInvocations += 1;
//			}
//
//			// Note:
//			//     The purpose is to count total number of invocations in each
//			//     compilation unit so that we can determine how many that are
//			//     unavailable for transformation and how many we cannot inline
//			//     without first extracting the call into a temporary variable:
//			//
//			//     int x = 1 + f();
//			// -->
//			//     int tmp = f();      // We can only inline function calls that are directly assigned to a variable.
//			//     int x   = 1 + tmp;
//		}
//
//		// This method gives an example of how to use the InlineMethodDescriptor!
//		// https://android.wekeepcoding.com/article/20191382/How+to+execute+inline+refactoring+programmatically+using+JDT+LTK%3F
//
//		return true;
//	}
}