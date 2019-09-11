package org.alfine.refactoring.suppliers;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.alfine.refactoring.opportunities.InlineMethodOpportunity;
import org.alfine.refactoring.opportunities.RefactoringOpportunity;
import org.alfine.refactoring.utils.ASTHelper;
import org.alfine.refactoring.utils.Generator;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class RandomInlineMethodSupplier extends RefactoringSupplier {

	public RandomInlineMethodSupplier(IJavaProject project, Generator generator) {
		super(project, generator);
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
	
}
