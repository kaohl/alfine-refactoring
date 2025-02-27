package org.alfine.refactoring.suppliers;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.alfine.refactoring.utils.ASTHelper;
import org.eclipse.jdt.core.ICompilationUnit;
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
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
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

	rename_type_type_param   = <cachetop>/rename/class-type-param/<pkg>/<class>/descriptors.txt
	rename_method_type_param = <cachetop>/rename/method-type-param/<pkg>/<class>/<method>/descriptors.txt

	rename_local             = <cachetop>/rename/local/<pkg>/<class>/<method>/descriptors.txt
	rename_param             = <cachetop>/rename/param/<pkg>/<class>/<method>/descriptors.txt
*/

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
	private Cache             cache;
	private ICompilationUnit  unit;
	private MethodSet         methods;
	private List<String>      trace;
	private boolean           isCapture;

	private List<TypeDeclaration> enclosingTypeDecls;

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
		this.cache        = cache;
		this.unit         = unit;
		this.methods      = methods;
		this.trace        = new LinkedList<>();
		this.expansion    = new LinkedList<>();
		this.isCapture    = false;

		this.enclosingTypeDecls = new LinkedList<>();

		this.inlineConstantOppStartSet  = new HashSet<Integer>();
		this.inlineMethodOppStartSet    = new HashSet<Integer>();
	}

	public List<String> getExpansion() {
		return this.expansion;
	}

//	private String getFullyQualifiedName() {
//		// There seems to be no straight forward way to get the
//		// fully qualified name of method declaration nodes.
//		// The getFullyQualifiedName() don't do that for SimpleName
//		// which is what getName() returns for MethodDeclaration.
//		// I tried to get the name via the binding as well, but that
//		// also only gave me unqualified names.
//		return String.join(".", trace);
//	}

	private void enter(String name) {
		trace.add(name);
		//System.out.println(String.format("TRACE %s", String.join(".", trace)));
	}

	private void leave() {
		trace.remove(trace.size() - 1);
	}

	@Override
	public boolean visit(PackageDeclaration node) {
		enter(node.getName().getFullyQualifiedName());
		return true;
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		enter(node.getName().getFullyQualifiedName());
		this.enclosingTypeDecls.add(node);
		return true;
	}

	@Override
	public void endVisit(TypeDeclaration node) {
		leave();
		this.enclosingTypeDecls.remove(this.enclosingTypeDecls.size() - 1);
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		enter(node.getName().getFullyQualifiedName());

		final String qNameAndPList = ASTHelper.getMethodSignature(node);

		if (!this.methods.hasMethod(qNameAndPList)) {
			return false; // Skip body.
		}

		System.out.println(String.format("Visit HOT method declaration %s", qNameAndPList));

		this.isCapture = true; // Enable opportunity capture.

		// Apply renaming to type declarations in context.
		for (TypeDeclaration decl : this.enclosingTypeDecls) {
			visit_rename(decl);
		}
		visit_rename(node);
		return true;           // Enter body.
	}

	@Override
	public void endVisit(MethodDeclaration node) {
		leave();
		this.isCapture = false; // Disable opportunity capture.
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
	public boolean visit(BooleanLiteral literal) {
		addExtractConstantFieldOpportunity(new ExtractConstantFieldContext(literal), createExtractConstantFieldDescriptor(literal.getStartPosition(), literal.getLength()));
		return false;
	}

	@Override
	public boolean visit(CharacterLiteral literal) {
		addExtractConstantFieldOpportunity(new ExtractConstantFieldContext(literal), createExtractConstantFieldDescriptor(literal.getStartPosition(), literal.getLength()));
		return false;
	}

	@Override
	public boolean visit(NumberLiteral literal) {
		addExtractConstantFieldOpportunity(new ExtractConstantFieldContext(literal), createExtractConstantFieldDescriptor(literal.getStartPosition(), literal.getLength()));
		return false;
	}

	@Override
	public boolean visit(StringLiteral literal) {
		addExtractConstantFieldOpportunity(new ExtractConstantFieldContext(literal), createExtractConstantFieldDescriptor(literal.getStartPosition(), literal.getLength()));
		return false;
	}

	@Override
	public boolean visit(ArrayInitializer node) {
		addExtractConstantFieldOpportunity(new ExtractConstantFieldContext(node), createExtractConstantFieldDescriptor(node.getStartPosition(), node.getLength()));
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

		IBinding b = name.resolveBinding();

		if (b != null && !name.isDeclaration()) {

			int     modifiers = b.getModifiers();
			boolean isFinal   = (modifiers & org.eclipse.jdt.core.dom.Modifier.FINAL)  > 0;
			boolean isStatic  = (modifiers & org.eclipse.jdt.core.dom.Modifier.STATIC) > 0;

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
		if (node.resolveMethodBinding() instanceof IMethodBinding binding) {

			int modifierFlags = 
				//org.eclipse.jdt.core.dom.Modifier.PRIVATE |
				org.eclipse.jdt.core.dom.Modifier.STATIC;

			int modifiers = binding.getModifiers(); // Bitwise or of modifier constants.

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

		// This "argument" is only for mapping the opportunity
		// to the correct location in the histogram supply.

		// args.put(RefactoringDescriptor.KEY_HIST_BIN, "" + (end - start + 1)); // TODO: Remove. We now have a file system layout that communicate number of statements.

		return new ExtractMethodDescriptor(args);
	}

	@Override
	public boolean visit(Block block) {
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

	private Set<String> typeRenamings = new HashSet<>();

	@SuppressWarnings("unchecked")
	public void visit_rename(TypeDeclaration decl) {
		if (
			decl.getName().resolveBinding() instanceof IBinding binding &&
			binding.getJavaElement()        instanceof IType    element
		) {
			String name = ASTHelper.getFullyQualifiedName(decl);
			if (typeRenamings.contains(name)) {
				// Don't add multiple times.
				// Happens when there are multiple hot methods in the same type.
				return;
			}
			typeRenamings.add(name);
			addRenameOpportunity(new RenameTypeContext(decl), createRenameTypeDescriptor((IType) element));
		}
	}

//	@Override
//	public boolean visit(QualifiedName node) {
//		Optional.ofNullable(node.resolveBinding())
//		.map(IBinding::getJavaElement)
//		.filter(e -> e instanceof IField)
//		.ifPresent(e -> {
//			addRenameOpportunity(createRenameFieldDescriptor(e));
//		});
//		return true;
//	}

	@Override
	public boolean visit(FieldAccess node) {
		if (
			node.resolveFieldBinding() instanceof IVariableBinding binding &&
			binding.getJavaElement()   instanceof IJavaElement     element
		) {
			addRenameOpportunity(new RenameFieldAccessContext(node), createRenameFieldDescriptor(element));
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

	@SuppressWarnings("unchecked")
	public void visit_rename(MethodDeclaration decl) {
		if (decl.resolveBinding() instanceof IMethodBinding binding &&
			binding.getJavaElement()  instanceof IMethod    method
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
	}

	public boolean visit(SingleVariableDeclaration svd) {
		if (
			svd.resolveBinding()     instanceof IVariableBinding binding  &&
			binding.getJavaElement() instanceof ILocalVariable   variable
		) {
			if (ASTHelper.isMethodParameter(svd)) {
				// Note: A parameter is still a local variable descriptor, but we change the context to be able to distinguish between parameters and locals.
				addRenameOpportunity(new RenameMethodParameterContext(svd), createRenameLocalVariableDescriptor(variable));
			} else {
				// Is this a real case?
				new Exception("Unhandled case").printStackTrace();
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean visit(VariableDeclarationStatement decl) {
		RenameLocalVariableContext context = new RenameLocalVariableContext(decl);
		for (VariableDeclarationFragment frag : (List<VariableDeclarationFragment>)decl.fragments()) {
			if (
				frag.getName().resolveBinding() instanceof IBinding       binding &&
				binding.getJavaElement()        instanceof ILocalVariable element
			) {
				addRenameOpportunity(context, createRenameLocalVariableDescriptor(element));
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean visit(VariableDeclarationExpression decl) {
		RenameLocalVariableContext context = new RenameLocalVariableContext(decl);
		for (VariableDeclarationFragment frag : (List<VariableDeclarationFragment>)decl.fragments()) {
			if (
				frag.getName().resolveBinding() instanceof IBinding       binding &&
				binding.getJavaElement()        instanceof ILocalVariable element
			) {
				addRenameOpportunity(context, createRenameLocalVariableDescriptor(element));
			}
		}
		return true;
	}

	@Override
	public boolean visit(TypeParameter typeParameter) {
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
		return true;
	}
}
