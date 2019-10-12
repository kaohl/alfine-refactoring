package org.alfine.refactoring.suppliers;

import java.util.Vector;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.opportunities.RefactoringOpportunity;
import org.alfine.refactoring.utils.ASTHelper;
import org.alfine.refactoring.utils.Generator;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class RandomRenameSupplier extends RefactoringSupplier {

	public RandomRenameSupplier(Workspace workspace, Generator generator) {
		super(workspace, generator);
	}

	@Override
	protected Vector<RefactoringOpportunity> collectOpportunities() {

		
		/*
		Vector<RefactoringOpportunity> opportunities = new Vector<>();
		

		try {

			
			IType t = getProject().findType("org.lib.L0");

			opportunities.add(new RenameTypeOpportunity(t, getGenerator()));

		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		*/
		
		
		// Note: Resources should be loaded and added in build order.
		
		// Note: We must order our imports and sort fragment roots, fragments,
		//       and compilation units so that the order in which compilation
		//       units are visited is deterministic. We must also make sure
		//       that the order in which refactoring opportunities are generated
		//       from visiting a compilation unit is deterministic.
		//
		// Note: We may also have to configure classpath to make sure that
		//       resources appear in an acceptable order. Perhaps we should
		//       list the build order (dependency chain) in a file and have
		//       the refactoring framework load resources top-down and mark
		//       entries that are variable.
		//

		Vector<RefactoringOpportunity> opportunities = new Vector<>();

		visitCompilationUnits(icu -> {
			CompilationUnit cu = ASTHelper.getCompilationUnit(icu);
			cu.accept(new RenameVisitor(icu, opportunities, getGenerator()));
		});
		return opportunities;
		
		
		/*
		List<IPackageFragmentRoot> roots = Project.getAvailableRoots();
				
		roots.stream()
		.sorted((x,y) -> x.getElementName().compareTo(y.getElementName()))
		.flatMap(r -> {
			try {
				return Arrays.asList(r.getChildren()).stream();
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
			return java.util.stream.Stream.empty();
		})
		.filter (e -> e.getElementType() == IJavaElement.PACKAGE_FRAGMENT)
		.map   (e -> (IPackageFragment)e)
		.sorted((x,y) -> x.getElementName().compareTo(y.getElementName()))
		.flatMap(f -> {
			try {
				return Arrays.asList(f.getCompilationUnits()).stream();
			} catch (JavaModelException e) {}
			return java.util.stream.Stream.empty();
		})
		.forEach(icu -> {
			CompilationUnit cu = ASTHelper.getCompilationUnit(icu);
			cu.accept(new RenameVisitor(icu, opportunities, getGenerator()));
		});
		return opportunities;
		 */
		
		
		
		
		
		
		/*
		IJavaProject project = getProject(); // TODO: Replace with getAvailableRoots()!
		
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
					cu.accept(new RenameVisitor(icu, opportunities, getGenerator()));
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		return opportunities;
		*/
	}

	/*
	private static String
	getRenameKind(IJavaElement e)
	{
		switch (e.getElementType()) {
		
		case IJavaElement.COMPILATION_UNIT:
			return IJavaRefactorings.RENAME_COMPILATION_UNIT;

		case IJavaElement.FIELD:
			return IJavaRefactorings.RENAME_FIELD;

		case IJavaElement.LOCAL_VARIABLE:
			return IJavaRefactorings.RENAME_LOCAL_VARIABLE;

		case IJavaElement.METHOD:
			return IJavaRefactorings.RENAME_METHOD;

		case IJavaElement.TYPE:
			return IJavaRefactorings.RENAME_TYPE;

		case IJavaElement.TYPE_PARAMETER:
			return IJavaRefactorings.RENAME_TYPE_PARAMETER;

		case IJavaElement.PACKAGE_FRAGMENT:
			return IJavaRefactorings.RENAME_PACKAGE;

		case IJavaElement.PACKAGE_FRAGMENT_ROOT:
			return IJavaRefactorings.RENAME_PACKAGE;

		default: throw new RuntimeException("Unexpected object `" + e.getClass() + "' to rename");
		}
	}
	 */
}
