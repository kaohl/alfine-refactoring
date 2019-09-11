package org.alfine.refactoring.suppliers;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;

import org.alfine.refactoring.framework.Project;
import org.alfine.refactoring.opportunities.RefactoringOpportunity;
import org.alfine.refactoring.opportunities.RenameFieldOpportunity;
import org.alfine.refactoring.opportunities.RenameLocalVariableOpportunity;
import org.alfine.refactoring.opportunities.RenameMethodOpportunity;
import org.alfine.refactoring.opportunities.RenameTypeOpportunity;
import org.alfine.refactoring.opportunities.RenameTypeParameterOpportunity;
import org.alfine.refactoring.utils.Generator;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class RandomRenameSupplier extends RefactoringSupplier {

	public RandomRenameSupplier(IJavaProject project, Generator generator) {
		super(project, generator);
	}

	@Override
	protected Vector<RefactoringOpportunity> collectOpportunities() {

		
		Vector<RefactoringOpportunity> opportunities = new Vector<>();
		

		try {

			IType t = Project.getJavaProject().findType("org.extendj.ast.CompilationUnit");
			
			opportunities.add(new RenameTypeOpportunity(t, getGenerator()));

		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		
		
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

		/*
		Vector<RefactoringOpportunity> opportunities = new Vector<>();

		visitCompilationUnits(icu -> {
			CompilationUnit cu = ASTHelper.getCompilationUnit(icu);
			cu.accept(new RenameVisitor(icu, opportunities, getGenerator()));
		});
		*/
		
		
		
		
		
		
		
		
		
		
		
		
		
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
		 */
		
		return opportunities;
		
		
		
		
		
		
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
	
	private static class RenameVisitor extends ASTVisitor {
		private ICompilationUnit               unit;
		private Set<Integer>                   oppStartSet;   // Source start position for all found opportunities.
		private Vector<RefactoringOpportunity> opportunities;
		private Generator                      generator;

		public RenameVisitor(ICompilationUnit unit, Vector<RefactoringOpportunity> opportunities, Generator generator) {
			this.unit          = unit;
			this.oppStartSet   = new HashSet<Integer>();
			this.opportunities = opportunities;
			this.generator     = generator;
		}
		
		private Generator getGenerator() {
			return this.generator;
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
		public boolean visit(TypeDeclaration decl) {

			String name = decl.getName().getIdentifier();
			
			Optional.ofNullable(decl.getName().resolveBinding())
			.map  (b -> (IJavaElement)b.getJavaElement())
			.filter(e -> e instanceof IType)
			.ifPresentOrElse(element -> {
				System.out.println("TypeDeclaration"
					+ "\n\tname   = " + name
					+ "\n\tstart  = " + decl.getStartPosition()
					+ "\n\tlength = " + decl.getLength());

				addOpportunity(new RenameTypeOpportunity(element, generator), decl.getStartPosition());

					
				for (TypeParameter tp : (List<TypeParameter>)decl.typeParameters()) {

					element = tp.getName().resolveBinding().getJavaElement();

					System.out.println("TypeParameter (TypeDeclaration)"
							+ "\n\tname   = " + tp.getName()
							+ "\n\tstart  = " + tp.getStartPosition()
							+ "\n\tlength = " + tp.getLength());

					addOpportunity(new RenameTypeParameterOpportunity(element, generator), tp.getStartPosition());
				}
			}, () -> {});

			return true;

			/*
			SimpleName   simpleName  = decl.getName();
			IBinding     binding     = simpleName.resolveBinding();
			IJavaElement element     = binding.getJavaElement();
			int          elementType = binding.getJavaElement().getElementType();

			String name = simpleName.getIdentifier();

			if (elementType == IJavaElement.TYPE) {
				IType type = (IType)element;
				name = type.getFullyQualifiedName();
			}

			System.out.println("TypeDeclaration"
				+ "\n\tname   = " + name
				+ "\n\tstart  = " + decl.getStartPosition()
				+ "\n\tlength = " + decl.getLength());

			addOpportunity(new RenameTypeOpportunity(element, generator), decl.getStartPosition());

			
			for (TypeParameter tp : (List<TypeParameter>)decl.typeParameters()) {

				element = tp.getName().resolveBinding().getJavaElement();

				System.out.println("TypeParameter (TypeDeclaration)"
						+ "\n\tname   = " + tp.getName()
						+ "\n\tstart  = " + tp.getStartPosition()
						+ "\n\tlength = " + tp.getLength());

				addOpportunity(new RenameTypeParameterOpportunity(element, generator), tp.getStartPosition());
			}
			
			return true;
			*/
		}

		public boolean visit(FieldDeclaration decl) {

			System.out.println("FieldDeclaration: " + decl);

			for (VariableDeclarationFragment frag : (List<VariableDeclarationFragment>)decl.fragments()) {

				Optional.ofNullable(frag.getName().resolveBinding())
				.map  (b -> (IJavaElement)b.getJavaElement())
				.filter(e -> e instanceof IField)
				.ifPresentOrElse(e -> {
					System.out.println("VariableDeclarationFragment in FieldDeclaration"
							+ "\n\tname   = " + frag.getName()
							+ "\n\tstart  = " + frag.getStartPosition()
							+ "\n\tlength = " + frag.getLength());

					addOpportunity(new RenameFieldOpportunity(e, generator), frag.getStartPosition());
				}, () -> {
					System.err.println("IJavaElement for VariableDeclarationFragment is not instanceof ILocalVariable!");
				});
				
				/*
				IBinding     b = frag.getName().resolveBinding();
				IJavaElement e = b.getJavaElement();

				if (e != null && e instanceof IField) {

					System.out.println("VariableDeclarationFragment in FieldDeclaration"
							+ "\n\tname   = " + frag.getName()
							+ "\n\tstart  = " + frag.getStartPosition()
							+ "\n\tlength = " + frag.getLength());

					addOpportunity(new RenameFieldOpportunity(e, generator), frag.getStartPosition());

				} else {
					System.err.println("IJavaElement for VariableDeclarationFragment is not instanceof ILocalVariable!");
				}
				*/
			}

			return true;
		}

		public boolean visit(MethodDeclaration decl) {

			Optional.ofNullable(decl.resolveBinding())
			.map  (b -> (IJavaElement)b.getJavaElement())
			.filter(e -> e instanceof IMethod)
			.ifPresentOrElse(element -> {

				IMethod method  = (IMethod)element;

				String tag = "";

				boolean skip   = false;
				boolean isMain = false;
				boolean isCtor = false;
				try {
					isMain = method.isMainMethod();
					isCtor = method.isConstructor();
				} catch (Exception e) {
					
					e.printStackTrace();

					tag  = "<unknown>";
					skip = true;
				}

				tag = isMain ? " (main)"        : tag;
				tag = isCtor ? " (constructor)" : tag;

				System.out.println("MethodDeclaration" + tag
						+ "\n\tname   = " + decl.resolveBinding().getName() // We have already checked that binding is present.
						+ "\n\tstart  = " + decl.getStartPosition()
						+ "\n\tlength = " + decl.getLength());

				if (skip) {
					System.out.println("Skip MethodDeclaration: Unable to determine if method is `main' or a constructor.");
					return;
				}

				if (!isMain && ! isCtor) {
					addOpportunity(new RenameMethodOpportunity(element, generator), decl.getStartPosition());
				}

				for (TypeParameter tp : (List<TypeParameter>)decl.typeParameters()) {

					Optional.ofNullable(tp.getName().resolveBinding())
					.map  (b -> (IJavaElement)b.getJavaElement())
					.filter(e -> e instanceof ITypeParameter)
					.ifPresentOrElse(e -> {

						System.out.println("TypeParameter (MethodDeclaration)"
							+ "\n\tname   = " + tp.getName()
							+ "\n\tstart  = " + tp.getStartPosition()
							+ "\n\tlength = " + tp.getLength());

						addOpportunity(new RenameTypeParameterOpportunity(e, generator), tp.getStartPosition());
					}, () -> {});
				}
			}, () -> {});

			return true;

			/*
			IMethodBinding mb      = decl.resolveBinding();
			IJavaElement   element = mb.getJavaElement();
			IMethod        method  = (IMethod)element;
			
			String tag = "";

			try {
				tag = method.isMainMethod()  ? " (main)"        : tag;
				tag = method.isConstructor() ? " (constructor)" : tag;
			} catch (Exception e) {
				e.printStackTrace();
			}

			System.out.println("MethodDeclaration" + tag
					+ "\n\tname   = " + mb.getName()
					+ "\n\tstart  = " + decl.getStartPosition()
					+ "\n\tlength = " + decl.getLength());

			try {
				if (!method.isMainMethod() && !method.isConstructor()) {
					addOpportunity(new RenameMethodOpportunity(element, generator), decl.getStartPosition());
				}
			} catch (JavaModelException e) {
				System.err.println("Failed to check if method is 'public static void main()'");
				e.printStackTrace();
			}

			for (TypeParameter tp : (List<TypeParameter>)decl.typeParameters()) {

				element = tp.getName().resolveBinding().getJavaElement();

				System.out.println("TypeParameter (MethodDeclaration)"
						+ "\n\tname   = " + tp.getName()
						+ "\n\tstart  = " + tp.getStartPosition()
						+ "\n\tlength = " + tp.getLength());

				addOpportunity(new RenameTypeParameterOpportunity(element, generator), tp.getStartPosition());
			}
			return true;
			*/
			
			
			
			

			/*
			 * These should be captured by SingleVariableDeclaration.
			 * 
			for (Object obj : decl.parameters()) {
				// https://help.eclipse.org/neon/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Freference%2Fapi%2Forg%2Feclipse%2Fjdt%2Fcore%2Fdom%2FMethodDeclaration.html
				// https://help.eclipse.org/neon/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Freference%2Fapi%2Forg%2Feclipse%2Fjdt%2Fcore%2Fdom%2FSingleVariableDeclaration.html

				SingleVariableDeclaration svd = (SingleVariableDeclaration)obj;
				IVariableBinding svb = svd.resolveBinding();
				IJavaElement     ije = svb.getJavaElement();

				System.out.println("Parameter (MethodDeclaration)"
						+ "\n\tname   = " + svd.getName()
						+ "\n\tstart  = " + svd.getStartPosition()
						+ "\n\tlength = " + svd.getLength());

				opportunities.add(new RenameLocalVariable(ije, generator));
			}
			return true;
			*/

		}

		public boolean visit(SingleVariableDeclaration svd) {

			System.out.println("SingleVariableDeclaration: " + svd);

			Optional.ofNullable(svd.resolveBinding())
			.map  (b -> (IJavaElement)b.getJavaElement())
			.filter(e -> e instanceof ILocalVariable)
			.ifPresentOrElse(ije -> {

				IVariableBinding svb = svd.resolveBinding(); // We know it can be resolved now.

				System.out.println("SingleVariableDeclaration"
						+ "\n\tname   = " + svb.getName()
						+ "\n\tstart  = " + svd.getStartPosition()
						+ "\n\tlength = " + svd.getLength());

				addOpportunity(new RenameLocalVariableOpportunity(ije, generator), svd.getStartPosition());
			}, ()->{
				System.err.println("Unable to resolve SingleVariableDeclaration.");
			});

			return true;

			/*
			IVariableBinding svb = svd.resolveBinding();
			IJavaElement     ije = svb.getJavaElement();

			System.out.println("SingleVariableDeclaration"
					+ "\n\tname   = " + svb.getName()
					+ "\n\tstart  = " + svd.getStartPosition()
					+ "\n\tlength = " + svd.getLength());

			if (ije != null && ije instanceof ILocalVariable) {
				addOpportunity(new RenameLocalVariableOpportunity(ije, generator), svd.getStartPosition());
			} else {
				System.err.println("SingleVariableDeclaration IJavaElement `element' is not instanceof ILocalVariable!");
			}

			return true;
			*/
		}

		public boolean visit(VariableDeclarationStatement decl) {

			System.out.println("VariableDeclarationStatement: " + decl);

			for (VariableDeclarationFragment frag : (List<VariableDeclarationFragment>)decl.fragments()) {

				Optional.ofNullable(frag.getName().resolveBinding())
				.map  (b -> (IJavaElement)b.getJavaElement())
				.filter(e -> e instanceof ILocalVariable)
				.ifPresentOrElse(e -> {

					System.out.println("VariableDeclarationFragment in VariableDeclarationStatement"
							+ "\n\tname   = " + frag.getName()
							+ "\n\tstart  = " + frag.getStartPosition()
							+ "\n\tlength = " + frag.getLength());

					addOpportunity(new RenameLocalVariableOpportunity(e, generator), frag.getStartPosition());
				}, ()->{
					System.err.println("Unable to resolve VariableDeclarationFragment in VariableDeclarationStatement.");
				});
					
					
				/*
					
				IBinding     b = frag.getName().resolveBinding();
				IJavaElement e = b.getJavaElement();

				if (e != null && e instanceof ILocalVariable) {

					System.out.println("VariableDeclarationFragment in VariableDeclarationStatement"
							+ "\n\tname   = " + frag.getName()
							+ "\n\tstart  = " + frag.getStartPosition()
							+ "\n\tlength = " + frag.getLength());

					addOpportunity(new RenameLocalVariableOpportunity(e, generator), frag.getStartPosition());

				} else {
					System.err.println("IJavaElement for VariableDeclarationFragment is not instanceof ILocalVariable!");
				}
				*/
			}

			return true;
		}

		public boolean visit(VariableDeclarationExpression decl) {

			System.out.println("VariableDeclarationExpression: " + decl);

			for (VariableDeclarationFragment frag : (List<VariableDeclarationFragment>)decl.fragments()) {

				Optional.ofNullable(frag.getName().resolveBinding())
				.map  (b -> (IJavaElement)b.getJavaElement())
				.filter(e -> e instanceof ILocalVariable)
				.ifPresentOrElse(e -> {
					
					System.out.println("VariableDeclarationFragment in VariableDeclarationExpression"
						+ "\n\tname   = " + frag.getName()
						+ "\n\tstart  = " + frag.getStartPosition()
						+ "\n\tlength = " + frag.getLength());

					addOpportunity(new RenameLocalVariableOpportunity(e, generator), frag.getStartPosition());
					
				}, ()->{
					System.err.println("Unable to resolve VariableDeclarationFragment in VariableDeclarationExpression!");
				});
				
				
				/*
				IBinding     b = frag.getName().resolveBinding();
				IJavaElement e = b.getJavaElement();

				if (e != null && e instanceof ILocalVariable) {

					System.out.println("VariableDeclarationFragment in VariableDeclarationExpression"
							+ "\n\tname   = " + frag.getName()
							+ "\n\tstart  = " + frag.getStartPosition()
							+ "\n\tlength = " + frag.getLength());

					addOpportunity(new RenameLocalVariableOpportunity(e, generator), frag.getStartPosition());

				} else {
					System.err.println("IJavaElement for VariableDeclarationFragment is not instanceof ILocalVariable!");
				}
				*/
			}

			return true;
		}
		/*
		 * We must collect these in VariableDeclarationStatement otherwise the binding is not resolved...
		 * 
		public boolean visit(VariableDeclarationFragment frag) {

			IBinding     b = frag.getName().resolveBinding();
			IJavaElement e = b.getJavaElement();

			if (e != null && e instanceof ILocalVariable) {

				System.out.println("VariableDeclarationFragment"
						+ "\n\tname   = " + frag.getName()
						+ "\n\tstart  = " + frag.getStartPosition()
						+ "\n\tlength = " + frag.getLength());

				opportunities.add(new RenameLocalVariable(e, generator));

			} else {
				System.err.println("IJavaElement for VariableDeclarationFragment is not instanceof ILocalVariable!");
			}
			
			return true;
		}
		 */
	}
}
