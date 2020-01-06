package org.alfine.refactoring.suppliers;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.alfine.refactoring.opportunities.RefactoringOpportunity;
import org.alfine.refactoring.opportunities.RenameFieldOpportunity;
import org.alfine.refactoring.opportunities.RenameLocalVariableOpportunity;
import org.alfine.refactoring.opportunities.RenameMethodOpportunity;
import org.alfine.refactoring.opportunities.RenameTypeOpportunity;
import org.alfine.refactoring.opportunities.RenameTypeParameterOpportunity;
import org.alfine.refactoring.suppliers.RefactoringSupplier.VectorSupply;
import org.alfine.refactoring.utils.Generator;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class RenameVisitor extends ASTVisitor {
	private ICompilationUnit unit;
	private Set<Integer>     oppStartSet;   // Source start position for all found opportunities.
	private VectorSupply     supply;
	private Generator        generator;

	public RenameVisitor(ICompilationUnit unit, VectorSupply supply, Generator generator) {
		this.unit        = unit;
		this.oppStartSet = new HashSet<Integer>();
		this.supply      = supply;
		this.generator   = generator;
	}

	private Generator getGenerator() {
		return this.generator;
	}

	private void addOpportunity(RefactoringOpportunity opp, int start) {

		System.out.print("addOpportunity start = " + start);

		if (!oppStartSet.contains(start)) {
			supply.add(opp);
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
		.ifPresent(element -> {
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
		});

		return true;
	}

	public boolean visit(FieldDeclaration decl) {

		System.out.println("FieldDeclaration: " + decl);

		for (VariableDeclarationFragment frag : (List<VariableDeclarationFragment>)decl.fragments()) {

			Optional.ofNullable(frag.getName().resolveBinding())
			.map  (b -> (IJavaElement)b.getJavaElement())
			.filter(e -> e instanceof IField)
			.ifPresent(e -> {
				System.out.println("VariableDeclarationFragment in FieldDeclaration"
						+ "\n\tname   = " + frag.getName()
						+ "\n\tstart  = " + frag.getStartPosition()
						+ "\n\tlength = " + frag.getLength());

				addOpportunity(new RenameFieldOpportunity(e, generator), frag.getStartPosition());
			});
		}

		return true;
	}

	public boolean visit(MethodDeclaration decl) {

		Optional.ofNullable(decl.resolveBinding())
		.map  (b -> (IJavaElement)b.getJavaElement())
		.filter(e -> e instanceof IMethod)
		.ifPresent(element -> {

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
				.ifPresent(e -> {

					System.out.println("TypeParameter (MethodDeclaration)"
						+ "\n\tname   = " + tp.getName()
						+ "\n\tstart  = " + tp.getStartPosition()
						+ "\n\tlength = " + tp.getLength());

					addOpportunity(new RenameTypeParameterOpportunity(e, generator), tp.getStartPosition());
				});
			}
		});

		return true;
	}

	public boolean visit(SingleVariableDeclaration svd) {

		System.out.println("SingleVariableDeclaration: " + svd);

		Optional.ofNullable(svd.resolveBinding())
		.map  (b -> (IJavaElement)b.getJavaElement())
		.filter(e -> e instanceof ILocalVariable)
		.ifPresent(ije -> {

			IVariableBinding svb = svd.resolveBinding(); // We know it can be resolved now.

			System.out.println("SingleVariableDeclaration"
					+ "\n\tname   = " + svb.getName()
					+ "\n\tstart  = " + svd.getStartPosition()
					+ "\n\tlength = " + svd.getLength());

			addOpportunity(new RenameLocalVariableOpportunity(ije, generator), svd.getStartPosition());
		});

		return true;
	}

	public boolean visit(VariableDeclarationStatement decl) {

		System.out.println("VariableDeclarationStatement: " + decl);

		for (VariableDeclarationFragment frag : (List<VariableDeclarationFragment>)decl.fragments()) {

			Optional.ofNullable(frag.getName().resolveBinding())
			.map  (b -> (IJavaElement)b.getJavaElement())
			.filter(e -> e instanceof ILocalVariable)
			.ifPresent(e -> {

				System.out.println("VariableDeclarationFragment in VariableDeclarationStatement"
						+ "\n\tname   = " + frag.getName()
						+ "\n\tstart  = " + frag.getStartPosition()
						+ "\n\tlength = " + frag.getLength());

				addOpportunity(new RenameLocalVariableOpportunity(e, generator), frag.getStartPosition());
			});
		}

		return true;
	}

	public boolean visit(VariableDeclarationExpression decl) {

		System.out.println("VariableDeclarationExpression: " + decl);

		for (VariableDeclarationFragment frag : (List<VariableDeclarationFragment>)decl.fragments()) {

			Optional.ofNullable(frag.getName().resolveBinding())
			.map  (b -> (IJavaElement)b.getJavaElement())
			.filter(e -> e instanceof ILocalVariable)
			.ifPresent(e -> {
				
				System.out.println("VariableDeclarationFragment in VariableDeclarationExpression"
					+ "\n\tname   = " + frag.getName()
					+ "\n\tstart  = " + frag.getStartPosition()
					+ "\n\tlength = " + frag.getLength());

				addOpportunity(new RenameLocalVariableOpportunity(e, generator), frag.getStartPosition());
				
			});
		}

		return true;
	}
}
