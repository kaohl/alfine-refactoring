package org.alfine.refactoring.suppliers;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.alfine.refactoring.utils.ASTHelper;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
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
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

/*
	Renamings

	Methods
	1. The hot method
	2. Methods called directly from hot method

	Parameters
	1. Parameters of hot method
	2. Parameters of methods called directly from hot method

	Type parameters
	1. Type parameters of hot method
	2. Type parameters of methods called directly from hot method

    Locals
    1. Locals of hot method
    2. Locals of methods called directly from hot method

	Fields
	1. Fields referenced directly by hot method
	2. Fields referenced directly by methods called directly from hot method

	Constants
	1. Constant fields referenced directly by hot method
	2. Constant fields referenced directly by methods called directly from hot method

	Classes
	1. The enclosing classes of hot method
	2. Classes referenced from hot method (static calls or fields)
	3. Enclosing classes of methods called directly from hot method
	4. Classes referenced from methods called directly from hot method


	Extract constant
	1. Constant expressions within hot method
	2. Constant expressions within methods called directly from hot method

	Inline constant
	1. Constants referenced by hot method
	2. Constants referenced by methods called directly from hot method


	Extract method
	1. Statements from body of hot method
	2. Statements from body of methods called directly from hot method

	Inline method
	1. Inline hot method (required to be private and static)
	2. Inline methods called from hot method
	3. Inline methods called from methods called directly from hot method



    Expansions:
    - Callers of hot methods (include methods calling into hot methods; note that there could be many paths into a hot method and in worst case, only one is used by the benchmark. The same is true going out of a hot method.)
    - Called from hot methods
    - Superclass of hot method class (rename)
    - Interfaces of hot method class (rename)
 */

/*
The point of this organization is to make it possible to vary the
scope of the selection, i.e., a broad vs. narrow selection, based
on interest.

In a random selection, each <>-segment is a random choice between all available folders,
where in the last folder we find a list of descriptors from which we select a random entry.

In a targeted selection the user specify pre-made choices as a list of tuples.
We then make random selections from descriptor lists available given those constraints.
Constraint formats: (<pkg>) or (<pkg>, <class>), or (<pkg>, <class>, <method>)

If the user wants to select more refactorings than are available of a given type,
all refactorings are selected.

If the number of refactorings is more than 50% of those available, we instead make the
equivalent choice of deciding which should not be included rather than the opposite.

If we name the <pgk> folder using the full package fragment name, then we can
later filter opportunities base on package prefix.

Note(Possible Extension):
  We could generate argument files for "to-method" of extract refactorings
  to do extraction into random existing class.

	inline_constant          = <cachetop>/i-constant/<pkg>/<class>/<to-method>/descriptors.txt
	extract_constant         = <cachetop>/x-constant/<pkg>/<class>/<from-method>/descriptors.txt         // Assumes to == from

	inline_method            = <cachetop>/i-method/<pkg>/<class>/<to-method>/descriptors.txt
	extract_method           = <cachetop>/x-method/<pkg>/<class>/<from-method>/<nstmts>/descriptors.txt  // Assumes to == from

	rename_type              = <cachetop>/rename/type/descriptors.txt
	rename_fields            = <cachetop>/rename/field/<pkg>/<class>/descriptors.txt
	rename_method            = <cachetop>/rename/method/<pkg>/<class>/descriptors.txt

	rename_type_type_param   = <cachetop>/rename/type-type-param/<pkg>/<class>/descriptors.txt
	rename_method_type_param = <cachetop>/rename/method-type-param/<pkg>/<class>/<method>/descriptors.txt

	rename_local             = <cachetop>/rename/local/<pkg>/<class>/<method>/descriptors.txt
	rename_param             = <cachetop>/rename/param/<pkg>/<class>/<method>/descriptors.txt

	*** We could also move declarations around in a file (perhaps it is possible to just permute their places using AST rewrite?)

	NOTE: For extract method, we don't distinguish between different blocks in the method.
	      Instead, we output all n-sized extractions into the folder named 'n'.
	      One issue with this is that for blocks of size 1, all opportunities have equal
	      chance of being selected. However, larger blocks generate more folders, and
	      contribute to more folders, which means that the probability of extracting a
	      sequence of statements from a larger block is higher than the probability of
	      selecting a sequence of statements from a smaller block.

	      It might be better if we used two indirections with a selection on block ID
	      first, and then select on block size, and then select descriptor:

	          <cachetop>/x-method/<method context>/<block ID 0>/{<size 1>, <size 2>, ..., <size n0>}/descriptors.txt
  	          <cachetop>/x-method/<method context>/<block ID 1>/{<size 1>, <size 2>, ..., <size n1>}/descriptors.txt
  	          ...

		  This would distribute the selection fairly across all blocks.
		  However, on the individual statement level, statements in the middle of blocks
		  are part of more selections than statements closer to the edges, because there
		  are more selections covering such statements.
*/
// TODO: Instead of splitting static and instance fields and methods into different files,
//       we could add meta data to the descriptor.
//
//       "__static"    : {"true"|"false"}
//       "__visibility": {"public"|"protected"|"private"}
//       "__length"    : <int>  // Original length of renamed symbol. (Could be used to experiment with different or same size identifiers.)
//       "__init_type" : <type of initialiser; numeral, boolean, array initialiser...>
//       "__selection_size" : <size of selection of the input parameter (when defined)>
//       "__block_size": <extract|inline method block size>
//       "__n_params"   : <number of parameters of renamed, inlined, or extracted method>
//       "__n_type_params" : <number of type parameters> // >0 => generic
//
// We can use these to filter and categorize opportunities based on their attributes,
// before or after an experiment, provided that we store the descriptor with the
// refactoring patches (which we do anyway).
//

// TODO: The recursive exploration will require a depth limit and
//       it is probably a good idea to track which methods has been
//       visited to reduce redundant work.

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
	private static final boolean isCaptureRename               = true;

	public HotMethodVisitor(Cache cache, ICompilationUnit unit, MethodSet methods) {
		this.cache          = cache;
		this.unit           = unit;
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

	private void addOpportunity(RefactoringOpportunityContext context, RefactoringDescriptor descriptor) {
		if (!this.isCapture) {
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
		try {
			addExtractConstantFieldOpportunity(new ExtractConstantFieldContext(literal), createExtractConstantFieldDescriptor(literal.getStartPosition(), literal.getLength()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean visit(BooleanLiteral literal) {
		try {
			addExtractConstantFieldOpportunity(new ExtractConstantFieldContext(literal), createExtractConstantFieldDescriptor(literal.getStartPosition(), literal.getLength()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean visit(CharacterLiteral literal) {
		try {
			addExtractConstantFieldOpportunity(new ExtractConstantFieldContext(literal), createExtractConstantFieldDescriptor(literal.getStartPosition(), literal.getLength()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean visit(NumberLiteral literal) {
		try {
			addExtractConstantFieldOpportunity(new ExtractConstantFieldContext(literal), createExtractConstantFieldDescriptor(literal.getStartPosition(), literal.getLength()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean visit(StringLiteral literal) {
		try {
			addExtractConstantFieldOpportunity(new ExtractConstantFieldContext(literal), createExtractConstantFieldDescriptor(literal.getStartPosition(), literal.getLength()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean visit(ArrayInitializer node) {
		try {
			addExtractConstantFieldOpportunity(new ExtractConstantFieldContext(node), createExtractConstantFieldDescriptor(node.getStartPosition(), node.getLength()));
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

	/** Inline constant at specified location. */
	private InlineConstantFieldDescriptor createInlineConstantFieldDescriptor(int start, int length) {
		String selection = "" + start + " " + length;
		Map<String, String> args = new TreeMap<>();
		args.put("input", this.unit.getHandleIdentifier());
		args.put("element", this.unit.getHandleIdentifier());
		args.put("selection", selection);
		return new InlineConstantFieldDescriptor(args);
	}

	@Override
	public boolean visit(SimpleName name) {

		try {
			IBinding b = name.resolveBinding();
	
			if (b != null && !name.isDeclaration()) {
	
				int     modifiers = b.getModifiers();
				boolean isFinal   = (modifiers & org.eclipse.jdt.core.dom.Modifier.FINAL)  > 0;
				boolean isStatic  = (modifiers & org.eclipse.jdt.core.dom.Modifier.STATIC) > 0;
	
				if (b.getJavaElement() instanceof IField element) {
					addRenameOpportunity(new RenameFieldAccessContext(name), createRenameFieldDescriptor(element));
				}
	
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

	@Override
	public boolean visit(MethodInvocation node) {
		try {
			if (node.resolveMethodBinding() instanceof IMethodBinding binding) {
	
				int modifierFlags = org.eclipse.jdt.core.dom.Modifier.STATIC;
				int modifiers     = binding.getModifiers(); // Bitwise or of modifier constants.
	
				boolean isConstructor = binding.isConstructor();
				boolean isApplicable  = (modifiers & modifierFlags) != 0;
	
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
		args.put("__block_size", String.valueOf(end - start + 1));

		return new ExtractMethodDescriptor(args);
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
		return new RenameLocalVariableDescriptor(defaultArguments(element));
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

	public boolean visit(SingleVariableDeclaration svd) {
		try {
			if (
				svd.resolveBinding()     instanceof IVariableBinding binding  &&
				binding.getJavaElement() instanceof ILocalVariable   variable
			) {
				if (ASTHelper.isMethodParameter(svd)) {
					// Note: A parameter is still a local variable descriptor, but we change the context to be able to distinguish between parameters and locals.
					// TODO: Better to handle this as a meta attribute on the descriptor?
					addRenameOpportunity(new RenameMethodParameterContext(svd), createRenameLocalVariableDescriptor(variable));
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
}
