package org.alfine.refactoring.suppliers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

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

	/** Cache for refactoring descriptors.*/
	private Cache            cache;

	/** The IJavaProject to which compilation unit belongs. */
//	private IJavaProject     project;

	/** The compilation unit traversed by this visitor. */
//	private ICompilationUnit unit;

	// TODO: Do we need the IProject of the refactored project as well to fix context?

	public RenameVisitor(Cache cache, ICompilationUnit unit) {
		this.cache       = cache;
//		this.unit        = unit;
//		this.project     = unit.getJavaProject();
	}

	private void addOpportunity(RefactoringDescriptor descriptor) {
		this.cache.write(descriptor);
	}

	private Map<String, String> defaultArguments(IJavaElement element) {
		Map<String, String> args = new TreeMap<>();

		// Note: Refactoring defaults to workspace; input argument seems to specify project as well.
		//args.put("project", this.project.getHandleIdentifier()); 

		args.put("input", element.getHandleIdentifier());
		return args;
	}

	private RenameTypeDescriptor createRenameTypeDescriptor(IJavaElement element) {
		return new RenameTypeDescriptor(defaultArguments(element));
	}

	private RenameTypeParameterDescriptor createRenameTypeParameterDescriptor(IJavaElement element) {
		return new RenameTypeParameterDescriptor(defaultArguments(element));
	}

	private RenameFieldDescriptor createRenameFieldDescriptor(IJavaElement element) {
		return new RenameFieldDescriptor(defaultArguments(element));
	}

	private RenameMethodDescriptor createRenameMethodDescriptor(IJavaElement element) {
		return new RenameMethodDescriptor(defaultArguments(element));
	}

	private RenameLocalVariableDescriptor createRenameLocalVariableDescriptor(IJavaElement element) {
		return new RenameLocalVariableDescriptor(defaultArguments(element));
	}

	@SuppressWarnings("unchecked")
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

			// addOpportunity(new RenameTypeOpportunity(element, generator), decl.getStartPosition());
			addOpportunity(createRenameTypeDescriptor((IType) element));
				
			for (TypeParameter tp : (List<TypeParameter>)decl.typeParameters()) {

				element = tp.getName().resolveBinding().getJavaElement();

				System.out.println("TypeParameter (TypeDeclaration)"
						+ "\n\tname   = " + tp.getName()
						+ "\n\tstart  = " + tp.getStartPosition()
						+ "\n\tlength = " + tp.getLength());

				// addOpportunity(new RenameTypeParameterOpportunity(element, generator), tp.getStartPosition());
				addOpportunity(createRenameTypeParameterDescriptor(element));
			}
		});

		return true;
	}

	@SuppressWarnings("unchecked")
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

				//addOpportunity(new RenameFieldOpportunity(e, generator), frag.getStartPosition());
				addOpportunity(createRenameFieldDescriptor(e));
			});
		}

		return true;
	}

	@SuppressWarnings("unchecked")
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
				//addOpportunity(new RenameMethodOpportunity(element, generator), decl.getStartPosition());
				addOpportunity(createRenameMethodDescriptor(element));
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

					// addOpportunity(new RenameTypeParameterOpportunity(e, generator), tp.getStartPosition());
					addOpportunity(createRenameTypeParameterDescriptor(e));
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

			//addOpportunity(new RenameLocalVariableOpportunity(ije, generator), svd.getStartPosition());
			addOpportunity(createRenameLocalVariableDescriptor(ije));
		});

		return true;
	}

	@SuppressWarnings("unchecked")
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

				//addOpportunity(new RenameLocalVariableOpportunity(e, generator), frag.getStartPosition());
				addOpportunity(createRenameLocalVariableDescriptor(e));
			});
		}

		return true;
	}

	@SuppressWarnings("unchecked")
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

				// addOpportunity(new RenameLocalVariableOpportunity(e, generator), frag.getStartPosition());
				addOpportunity(createRenameLocalVariableDescriptor(e));

			});
		}

		return true;
	}
}
