package org.alfine.refactoring.suppliers;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.alfine.refactoring.utils.ASTHelper;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.internal.core.SourceField;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

/*
 * Computes refactoring opportunities for hot methods and
 * generates an optional expansion of methods related to
 * hot methods, that could be explored using the same
 * visitor to fan out from the initial context.
 */
public class HotMethodVisitor  extends ASTVisitor {
	/** Cache for refactoring descriptors.*/
	private Cache               cache;
	private ICompilationUnit    unit;
	private CompilationUnit     cu;
	private MethodSet           methods;
	private boolean             isCapture;
	private final List<Boolean> isCaptureStack;

	// A list of methods accessed from hot methods in
	// the associated compilation unit.
	private List<String>      expansion;

	// Track start offsets to avoid duplicatations.
	// Not 100% sure this is relevant anymore.
	private Set<Integer>     inlineConstantOppStartSet;   // Source start position for all found opportunities.
	private Set<Integer>     inlineMethodOppStartSet;

	// Toggle to control which opportunities are captured.
	private static final boolean isCaptureInlineConstantField  = true;
	private static final boolean isCaptureExtractConstantField = true;
	private static final boolean isCaptureInlineMethod         = true;
	private static final boolean isCaptureExtractMethod        = true;
	private static final boolean isCaptureInlineTemp           = true;
	private static final boolean isCaptureExtractTemp          = true;
	private static final boolean isCaptureMethodIndirection    = true;
	private static final boolean isCaptureRename               = true;

	public HotMethodVisitor(Cache cache, ICompilationUnit unit, CompilationUnit cu, MethodSet methods) {
		this.cache          = cache;
		this.unit           = unit;
		this.cu             = cu;
		this.methods        = methods;
		this.expansion      = new LinkedList<>();
		this.isCapture      = false;
		this.isCaptureStack = new LinkedList<>();

		this.inlineConstantOppStartSet  = new HashSet<Integer>();
		this.inlineMethodOppStartSet    = new HashSet<Integer>();
	}

	public List<String> getExpansion() {
		return this.expansion;
	}

	@Override
	public boolean visit(PackageDeclaration node) {
		return true;
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		// See endVisit(TypeDeclaration).
		return true;
	}

	@Override
	public void endVisit(TypeDeclaration node) {
		try {
			final boolean wasCapture = this.isCapture;
			this.isCapture = true;
			// Add hot context opportunities.
			if (this.hotContext.contains(ASTHelper.getFullyQualifiedName(node))) {
				if (
					node.getName().resolveBinding() instanceof IBinding binding &&
					binding.getJavaElement()        instanceof IType    element
				) {
					addRenameOpportunity(new RenameTypeContext(node), createRenameTypeDescriptor(element));
				}
				for (Object o : node.typeParameters()) {
					if (o instanceof TypeParameter tp) {
						tp.accept(this);
					}
				}
			}
			this.isCapture = wasCapture;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private final Set<String> hotContext = new HashSet<>();

	@Override
	public void preVisit(ASTNode node) {
		try {
			if (node instanceof MethodDeclaration md) {
				final String qNameAndPList = ASTHelper.getMethodSignature(md);
				this.isCaptureStack.add(this.methods.hasMethod(qNameAndPList)); // Enable capture if hot.
				this.isCapture = this.isCaptureStack.getLast();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void postVisit(ASTNode node) {
		try {
			if (node instanceof MethodDeclaration) {
				this.isCaptureStack.removeLast();
				this.isCapture = this.isCaptureStack.size() > 0 && this.isCaptureStack.getLast();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		try {
			// Apply renaming to type declarations in context.
			if (this.isCapture) {
				final String qNameAndPList = ASTHelper.getMethodSignature(node);
				System.out.println(String.format("Visit HOT method declaration %s", qNameAndPList));
				for (ASTNode n : ASTHelper.getNodeHierarchy(node)) {
					if (n instanceof TypeDeclaration td) {
						this.hotContext.add(ASTHelper.getFullyQualifiedName(td));
					} else if (n instanceof MethodDeclaration md) {
						this.hotContext.add(ASTHelper.getFullyQualifiedName(md));
					} else if (n instanceof TypeDeclarationStatement tds) {
						// TODO: getDeclaration() returns AbstractTypeDeclaration of which TypeDeclaration is one possibility.
						if (tds.getDeclaration() instanceof TypeDeclaration td) {
							this.hotContext.add(ASTHelper.getFullyQualifiedName(td));
						}
					}
				}
				visit_rename(node);
				visit_add_indirection(node);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public void endVisit(MethodDeclaration node) {
		try {
			final boolean wasCapture = this.isCapture;
			this.isCapture = true;
			// Add hot context opportunities.
			if (this.hotContext.contains(ASTHelper.getFullyQualifiedName(node))) {
				visit_rename(node);
				for (Object o : node.typeParameters()) {
					if (o instanceof TypeParameter tp) {
						tp.accept(this);
					}
				}
			}
			this.isCapture = wasCapture;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void visit_add_indirection(MethodDeclaration decl) {
		try {
			if (
				decl.resolveBinding() instanceof IMethodBinding binding
			) {
				addMethodIndirectionOpportunity(new MethodIndirectionContext(decl), createMethodIndirectionDescriptor(binding));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void addOpportunity(RefactoringOpportunityContext context, RefactoringDescriptor descriptor) {
		if (!this.isCapture || descriptor == null) {
			return;
		}
		this.cache.write(context, descriptor);
	}

	private void addExtractConstantFieldOpportunity(ExtractConstantFieldContext context, ExtractConstantFieldDescriptor descriptor) {
		if (!isCaptureExtractConstantField) {
			return;
		}
		addOpportunity(context, descriptor);
	}

	private ExtractConstantFieldDescriptor createExtractConstantFieldDescriptor(int start, int length) {
		String selection = "" + start + " " + length;
		Map<String, String> args = new TreeMap<>();
		args.put("input", this.unit.getHandleIdentifier());
		args.put("element", this.unit.getHandleIdentifier());
		args.put("selection", selection);
		return new ExtractConstantFieldDescriptor(args);
	}

	@Override
	public boolean visit(NullLiteral literal) {
		// ATTENTION: This is not a valid refactoring. (Leave comment to prevent future mistakes.)
		//		try {
		//			addExtractConstantFieldOpportunity(new ExtractConstantFieldContext(literal), createExtractConstantFieldDescriptor(literal.getStartPosition(), literal.getLength()));
		//		} catch (Exception e) {
		//			e.printStackTrace();
		//		}
		return false;
	}

	@Override
	public boolean visit(BooleanLiteral literal) {
		try {
			addExtractConstantFieldOpportunity(new ExtractConstantFieldContext(literal), createExtractConstantFieldDescriptor(literal.getStartPosition(), literal.getLength()));
			addExtractTempOpportunity(new ExtractTempContext(literal), createExtractTempDescriptor(literal.getStartPosition(), literal.getLength()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean visit(CharacterLiteral literal) {
		try {
			addExtractConstantFieldOpportunity(new ExtractConstantFieldContext(literal), createExtractConstantFieldDescriptor(literal.getStartPosition(), literal.getLength()));
			addExtractTempOpportunity(new ExtractTempContext(literal), createExtractTempDescriptor(literal.getStartPosition(), literal.getLength()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean visit(NumberLiteral literal) {
		try {
			addExtractConstantFieldOpportunity(new ExtractConstantFieldContext(literal), createExtractConstantFieldDescriptor(literal.getStartPosition(), literal.getLength()));
			addExtractTempOpportunity(new ExtractTempContext(literal), createExtractTempDescriptor(literal.getStartPosition(), literal.getLength()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean visit(StringLiteral literal) {
		try {
			addExtractConstantFieldOpportunity(new ExtractConstantFieldContext(literal), createExtractConstantFieldDescriptor(literal.getStartPosition(), literal.getLength()));
			addExtractTempOpportunity(new ExtractTempContext(literal), createExtractTempDescriptor(literal.getStartPosition(), literal.getLength()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean visit(ArrayInitializer literal) {
		try {
			addExtractConstantFieldOpportunity(new ExtractConstantFieldContext(literal), createExtractConstantFieldDescriptor(literal.getStartPosition(), literal.getLength()));
			addExtractTempOpportunity(new ExtractTempContext(literal), createExtractTempDescriptor(literal.getStartPosition(), literal.getLength()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	private void addInlineConstantOpportunity(InlineConstantFieldContext context, InlineConstantFieldDescriptor descriptor, int start) {
		if (!isCaptureInlineConstantField) {
			return;
		}
		if (!inlineConstantOppStartSet.contains(start)) {
			inlineConstantOppStartSet.add(start);
			addOpportunity(context, descriptor);
		}
	}

	private void addInlineTempOpportunity(InlineTempContext context, InlineTempDescriptor descriptor) {
		if (!isCaptureInlineTemp) {
			return;
		}
		addOpportunity(context, descriptor);
	}

	/** Inline constant at specified location. */
	private InlineConstantFieldDescriptor createInlineConstantFieldDescriptor(int start, int length) {
		String selection = "" + start + " " + length;
		Map<String, String> args = new TreeMap<>();
		args.put("input", this.unit.getHandleIdentifier());
		args.put("element", this.unit.getHandleIdentifier());
		args.put("selection", selection);
		return new InlineConstantFieldDescriptor(args);
	}

	private InlineTempDescriptor createInlineTempDescriptor(int start, int length) {
		Map<String, String> args = new TreeMap<>();
		args.put("input", this.unit.getHandleIdentifier());
		args.put("selection", "" + start + " " + length);
		return new InlineTempDescriptor(args);
	}

	@Override
	public boolean visit(SimpleName name) {

		try {
			IBinding b = name.resolveBinding();
	
			if (b != null && !name.isDeclaration()) {

				// Wrap in try-catch to also check the orthogonal case below.
				try {
					if (this.cu instanceof CompilationUnit cu) {
						ASTNode declNode = cu.findDeclaringNode(b);
						if (declNode instanceof VariableDeclaration || declNode instanceof VariableDeclarationStatement) {
							addInlineTempOpportunity(new InlineTempContext(name), createInlineTempDescriptor(name.getStartPosition(), name.getLength()));
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				int     modifiers = b.getModifiers();
				boolean isFinal   = (modifiers & org.eclipse.jdt.core.dom.Modifier.FINAL)  > 0;
				boolean isStatic  = (modifiers & org.eclipse.jdt.core.dom.Modifier.STATIC) > 0;

				if (b.getJavaElement() instanceof SourceField element && isSourceAvailable(element)) {
					addRenameOpportunity(new RenameFieldAccessContext(name), createRenameFieldDescriptor(element));

					if (isFinal && isStatic) {
						addInlineConstantOpportunity(
							new InlineConstantFieldContext(name),
							createInlineConstantFieldDescriptor(
								name.getStartPosition(),
								name.getLength()
							),
							name.getStartPosition()
						);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	private void addInlineMethodOpportunity(InlineMethodContext context, RefactoringDescriptor descriptor, int start) {
		if (!isCaptureInlineMethod) {
			return;
		}
		if (descriptor != null && !inlineMethodOppStartSet.contains(start)) {
			inlineMethodOppStartSet.add(start);
			addOpportunity(context, descriptor);
		}
	}

	private InlineMethodDescriptor createInlineMethodDescriptor(IJavaElement element, int start, int length) {
		if (element.getHandleIdentifier().contains("rt.jar")) {
			return null;
		} else {
			Map<String, String> args = new TreeMap<>();
			args.put("input", this.unit.getHandleIdentifier());
			args.put("element", element.getHandleIdentifier());
			args.put("selection", "" + start + " " + length);
			return new InlineMethodDescriptor(args);
		}
	}

	private MethodIndirectionDescriptor createMethodIndirectionDescriptor(IMethodBinding binding) {
		if (
			binding.getJavaElement()  instanceof IMethod method &&
			method.getDeclaringType() instanceof IType   type   &&
			!type.isReadOnly()
		) {
			Map<String, String> args = new TreeMap<>();
			args.put("input", method.getHandleIdentifier());
			args.put("element1", type.getHandleIdentifier());
			return new MethodIndirectionDescriptor(args);
		}
		return null;
	}

	private void addMethodIndirectionOpportunity(MethodIndirectionContext context, MethodIndirectionDescriptor descriptor) {
		if (!isCaptureMethodIndirection) {
			return;
		}
		addOpportunity(context, descriptor);
	}

	@Override
	public boolean visit(MethodInvocation node) {
		try {
			if (
				node.resolveMethodBinding() instanceof IMethodBinding binding &&
				binding.getJavaElement()    instanceof IJavaElement   element
			) {
				// Both extract temp and method indirection should work for references to both source and binary elements.
				try {
					addExtractTempOpportunity(new ExtractTempContext(node), createExtractTempDescriptor(node.getStartPosition(), node.getLength()));
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					addMethodIndirectionOpportunity(new MethodIndirectionContext(node), createMethodIndirectionDescriptor(binding));
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					int modifierFlags = org.eclipse.jdt.core.dom.Modifier.STATIC;
					int modifiers     = binding.getModifiers(); // Bitwise or of modifier constants.
		
					boolean isConstructor = binding.isConstructor();
					boolean isApplicable  = (modifiers & modifierFlags) != 0;
		
					if (isSourceAvailable(element) && !element.isReadOnly()) {
						if (!isConstructor && isApplicable) {
							addInlineMethodOpportunity(
								new InlineMethodContext(node),
								createInlineMethodDescriptor(
									binding.getJavaElement(),
									node.getStartPosition(),
									node.getLength()
								),
								node.getStartPosition()
							);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				// Source is not available...
				// (This happens for standard library and binary
				//  dependencies for which source is unavailable.)
				System.err.println("Unable to resolve method binding for method invocation: " + String.valueOf(node));			
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	private boolean isSourceAvailable(IJavaElement element) {
		return JavaElementUtil.isSourceAvailable((ISourceReference) element);
	}

	private void addExtractMethodOpportunity(ExtractMethodContext context, ExtractMethodDescriptor descriptor) {
		if (!isCaptureExtractMethod) {
			return;
		}
		addOpportunity(context, descriptor);
	}

	@SuppressWarnings("unchecked")
	private ExtractMethodDescriptor createExtractMethodDescriptor(Block block, int start, int end) {

		List<Statement> stmtList = null;

		stmtList = block.statements().subList(start, end + 1);

		Statement first = stmtList.get(0);
		Statement last = stmtList.get(stmtList.size() - 1);

		int blockStart  = first.getStartPosition();
		int blockLength = last.getStartPosition() - blockStart + last.getLength();

		Map<String, String> args = new TreeMap<>();
		args.put("input", this.unit.getHandleIdentifier());
		args.put("element", this.unit.getHandleIdentifier());
		args.put("selection", "" + blockStart + " " + blockLength);

		Map<String, String> meta = new TreeMap<>();
		meta.put("block_id"  , String.valueOf(block.getStartPosition()));
		meta.put("block_idx" , String.valueOf(start));
		meta.put("block_size", String.valueOf(end - start + 1));

		return new ExtractMethodDescriptor(args, meta);
	}

	@Override
	public boolean visit(Block block) {
		try {
			// Note: This does not cover (non-block) single statement bodies of
			//       control flow statements. But let's ignore that for now.
	
			int nbrStmts = block.statements().size();
	
			if (nbrStmts > 0) {
				for (int start = 0; start < nbrStmts; ++start) {
					for (int end = start; end < nbrStmts; ++end) {
						addExtractMethodOpportunity(new ExtractMethodContext(block, end - start + 1), createExtractMethodDescriptor(block, start, end));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		 // Visit nested blocks recursively.
		return true;
	}

	private void addRenameOpportunity(RefactoringOpportunityContext context, RefactoringDescriptor descriptor) {
		if (!isCaptureRename) {
			return;
		}
		addOpportunity(context, descriptor);
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
		Map<String, String> args = defaultArguments(element);
		Map<String, String> meta = new TreeMap<>();
		meta.put("is_param", "false");
		return new RenameLocalVariableDescriptor(args, meta);
	}

	private RenameLocalVariableDescriptor createRenameMethodParamDescriptor(IJavaElement element) {
		Map<String, String> args = defaultArguments(element);
		Map<String, String> meta = new TreeMap<>();
		meta.put("is_param", "true");
		return new RenameLocalVariableDescriptor(args, meta);
	}

	@Override
	public boolean visit(FieldAccess node) {
		try {
			if (
				node.resolveFieldBinding() instanceof IVariableBinding binding &&
				binding.getJavaElement()   instanceof IJavaElement     element
			) {
				addRenameOpportunity(new RenameFieldAccessContext(node), createRenameFieldDescriptor(element));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

//	@SuppressWarnings("unchecked")
//	public boolean visit(FieldDeclaration decl) {
//		System.out.println("FieldDeclaration: " + decl);
//		for (VariableDeclarationFragment frag : (List<VariableDeclarationFragment>)decl.fragments()) {
//			Optional.ofNullable(frag.getName().resolveBinding())
//			.map  (b -> (IJavaElement)b.getJavaElement())
//			.filter(e -> e instanceof IField)
//			.ifPresent(e -> {
//				System.out.println("VariableDeclarationFragment in FieldDeclaration"
//						+ "\n\tname   = " + frag.getName()
//						+ "\n\tstart  = " + frag.getStartPosition()
//						+ "\n\tlength = " + frag.getLength());
//				addRenameOpportunity(createRenameFieldDescriptor(e));
//			});
//		}
//		return true;
//	}

	public void visit_rename(MethodDeclaration decl) {
		try {
			if (
				decl.resolveBinding()     instanceof IMethodBinding binding &&
				binding.getJavaElement()  instanceof IMethod        method
			) {
				boolean skip   = false;
				boolean isMain = false;
				boolean isCtor = false;
				try {
					isMain = method.isMainMethod();
					isCtor = method.isConstructor();
				} catch (Exception e) {
					e.printStackTrace();
					skip = true;
				}
				if (skip) {
					System.out.println("Skip MethodDeclaration: Unable to determine if method is `main' or a constructor.");
					return;
				}
				if (!isMain && ! isCtor) {
					addRenameOpportunity(new RenameMethodContext(decl), createRenameMethodDescriptor((IJavaElement)method));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean visit(VariableDeclarationFragment node) {
		try {
			if (
				node.resolveBinding()    instanceof IVariableBinding binding  &&
				binding.getJavaElement() instanceof ILocalVariable   variable
			) {
				addRenameOpportunity(new RenameLocalVariableContext(node), createRenameLocalVariableDescriptor(variable));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	public boolean visit(SingleVariableDeclaration svd) {
		try {
			if (
				svd.resolveBinding()     instanceof IVariableBinding binding  &&
				binding.getJavaElement() instanceof ILocalVariable   variable
			) {
				if (ASTHelper.isMethodParameter(svd)) {
					addRenameOpportunity(new RenameMethodParameterContext(svd), createRenameMethodParamDescriptor(variable));
				} else {
					addRenameOpportunity(new RenameLocalVariableContext(svd), createRenameLocalVariableDescriptor(variable));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean visit(VariableDeclarationStatement decl) {
		try {
			RenameLocalVariableContext context = new RenameLocalVariableContext(decl);
			for (VariableDeclarationFragment frag : (List<VariableDeclarationFragment>)decl.fragments()) {
				if (
					frag.getName().resolveBinding() instanceof IBinding       binding &&
					binding.getJavaElement()        instanceof ILocalVariable element
				) {
					addRenameOpportunity(context, createRenameLocalVariableDescriptor(element));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean visit(VariableDeclarationExpression decl) {
		try {
			RenameLocalVariableContext context = new RenameLocalVariableContext(decl);
			for (VariableDeclarationFragment frag : (List<VariableDeclarationFragment>)decl.fragments()) {
				if (
					frag.getName().resolveBinding() instanceof IBinding       binding &&
					binding.getJavaElement()        instanceof ILocalVariable element
				) {
					addRenameOpportunity(context, createRenameLocalVariableDescriptor(element));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public boolean visit(TypeParameter typeParameter) {
		try {
			if (
				typeParameter.getName().resolveBinding() instanceof IBinding       binding &&
				binding.getJavaElement()                 instanceof ITypeParameter element
			) {
				if (ASTHelper.isMethodTypeParameter(typeParameter)) {
					addRenameOpportunity(new RenameMethodTypeParameterContext(typeParameter), createRenameTypeParameterDescriptor(element));
				} else if (ASTHelper.isTypeTypeParameter(typeParameter)) {
					addRenameOpportunity(new RenameTypeTypeParameterContext(typeParameter), createRenameTypeParameterDescriptor(element));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	private void addExtractTempOpportunity(ExtractTempContext context, ExtractTempDescriptor descriptor) {
		if (!isCaptureExtractTemp) {
			return;
		}
		addOpportunity(context, descriptor);
	}

	private ExtractTempDescriptor createExtractTempDescriptor(int start, int length) {
		Map<String, String> args = new TreeMap<>();
		args.put("input", this.unit.getHandleIdentifier());
		args.put("selection", start + " " + length);
		return new ExtractTempDescriptor(args);
	}

	@Override
	public boolean visit(InfixExpression node) {
		try {
			addExtractTempOpportunity(new ExtractTempContext(node), createExtractTempDescriptor(node.getStartPosition(), node.getLength()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
}
