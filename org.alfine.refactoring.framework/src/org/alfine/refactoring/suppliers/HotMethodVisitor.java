package org.alfine.refactoring.suppliers;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

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
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
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
	private Predicate<String> isHot;
	private List<String>      trace;
	private boolean           isCapture;

	private List<TypeDeclaration> enclosingTypeDecls;

	// A list of methods accessed from hot methods in
	// the associated compilation unit.
	private List<String>      expansion;

	// Inline constant state. (TODO: Can't remember why this is needed.)
	private Set<Integer>     inlineConstantOppStartSet;   // Source start position for all found opportunities.
	private Set<Integer>     inlineMethodOppStartSet;

	// Toggle to control which opportunities are captured.
	private static final boolean isCaptureInlineConstantField  = false;
	private static final boolean isCaptureExtractConstantField = false;
	private static final boolean isCaptureInlineMethod         = false;
	private static final boolean isCaptureExtractMethod        = false;
	private static final boolean isCaptureRename               = true;

	public HotMethodVisitor(Cache cache, ICompilationUnit unit, Predicate<String> isHot) {
		this.cache        = cache;
		this.unit         = unit;
		this.isHot        = isHot;
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

	private String getFullyQualifiedName() {
		// There seems to be no straight forward way to get the
		// fully qualified name of method declaration nodes.
		// The getFullyQualifiedName() don't do that for SimpleName
		// which is what getName() returns for MethodDeclaration.
		// I tried to get the name via the binding as well, but that
		// also only gave me unqualified names.
		return String.join(".", trace);
	}

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

		List<String> paramTypes = new LinkedList<>();
		for (Object p : node.parameters()) {
			String param    = p.toString();
			String typeOnly = param.substring(0, param.lastIndexOf(" ")).trim();
			paramTypes.add(typeOnly);
		}

		String qNameAndPList = String.format("%s(%s)", getFullyQualifiedName(),  String.join(", ", paramTypes));

		if (!isHot.test(qNameAndPList)) {
			return false; // Skip body.
		}

		System.out.println(String.format("Visit HOT method declaration %s", qNameAndPList));

		// Apply renaming to type declarations in context.
		for (TypeDeclaration decl : this.enclosingTypeDecls) {
			visit_rename(decl);
		}

		visit_rename(node);
		
		this.isCapture = true; // Enable opportunity capture.
		return true;           // Enter body.
	}

	@Override
	public void endVisit(MethodDeclaration node) {
		leave();
		this.isCapture = false; // Disable opportunity capture.
	}

	
	private void addOpportunity(RefactoringDescriptor descriptor) {
		if (!this.isCapture) {
			return;
		}
		System.out.println("ADD OPPORUNITY: " + descriptor);
		this.cache.write(descriptor);
	}

	private void addExtractConstantFieldOpportunity(ExtractConstantFieldDescriptor descriptor) {
		if (!isCaptureExtractConstantField) {
			return;
		}
		addOpportunity(descriptor);
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
		addExtractConstantFieldOpportunity(createExtractConstantFieldDescriptor(literal.getStartPosition(), literal.getLength()));
		return false;
	}

	@Override
	public boolean visit(CharacterLiteral literal) {
		addExtractConstantFieldOpportunity(createExtractConstantFieldDescriptor(literal.getStartPosition(), literal.getLength()));
		return false;
	}

	@Override
	public boolean visit(NumberLiteral literal) {
		addExtractConstantFieldOpportunity(createExtractConstantFieldDescriptor(literal.getStartPosition(), literal.getLength()));
		return false;
	}

	@Override
	public boolean visit(StringLiteral literal) {
		addExtractConstantFieldOpportunity(createExtractConstantFieldDescriptor(literal.getStartPosition(), literal.getLength()));
		return false;
	}

	@Override
	public boolean visit(ArrayInitializer node) {
		addExtractConstantFieldOpportunity(createExtractConstantFieldDescriptor(node.getStartPosition(), node.getLength()));
		return true;
	}

	private void addInlineConstantOpportunity(InlineConstantFieldDescriptor descriptor, int start) {
		if (!isCaptureInlineConstantField) {
			return;
		}
		if (!inlineConstantOppStartSet.contains(start)) {
			inlineConstantOppStartSet.add(start);
			addOpportunity(descriptor);
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
			boolean isFinal  = (modifiers & org.eclipse.jdt.core.dom.Modifier.FINAL)  > 0;
			boolean isStatic = (modifiers & org.eclipse.jdt.core.dom.Modifier.STATIC) > 0;

			if (isFinal && isStatic) {
				addInlineConstantOpportunity(
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

	private void addInlineMethodOpportunity(RefactoringDescriptor descriptor, int start) {
		if (!isCaptureInlineMethod) {
			return;
		}
		if (descriptor != null && !inlineMethodOppStartSet.contains(start)) {
			inlineMethodOppStartSet.add(start);
			addOpportunity(descriptor);
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
				
				// TODO: Why limit to private static methods?

				int modifierFlags = 
					//org.eclipse.jdt.core.dom.Modifier.PRIVATE |
					org.eclipse.jdt.core.dom.Modifier.STATIC;

				int modifiers = mb.getModifiers(); // Bitwise or of modifier constants.
				
				boolean isConstructor = mb.isConstructor();
				boolean isApplicable  = (modifiers & modifierFlags) != 0;

				if (!isConstructor && isApplicable) {
					addInlineMethodOpportunity(
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

	@Override
	public boolean visit(MethodInvocation mi) {
		tryAddNode(mi);
		return true;
	}

	private void addExtractMethodOpportunity(ExtractMethodDescriptor descriptor) {
		if (!isCaptureExtractMethod) {
			return;
		}
		addOpportunity(descriptor);
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

		args.put(RefactoringDescriptor.KEY_HIST_BIN, "" + (end - start + 1));

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
					addExtractMethodOpportunity(createExtractMethodDescriptor(block, start, end));
				}
			}
		}
		 // Visit nested blocks recursively.
		return true;
	}

	private void addRenameOpportunity(RefactoringDescriptor descriptor) {
		if (!isCaptureRename) {
			return;
		}
		addOpportunity(descriptor);
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
	public boolean visit_rename(TypeDeclaration decl) {
		String name = decl.getName().getIdentifier();
		Optional.ofNullable(decl.getName().resolveBinding())
		.map  (b -> (IJavaElement)b.getJavaElement())
		.filter(e -> e instanceof IType)
		.ifPresent(element -> {
			System.out.println("TypeDeclaration"
				+ "\n\tname   = " + name
				+ "\n\tstart  = " + decl.getStartPosition()
				+ "\n\tlength = " + decl.getLength());
			addRenameOpportunity(createRenameTypeDescriptor((IType) element));
			for (TypeParameter tp : (List<TypeParameter>)decl.typeParameters()) {
				element = tp.getName().resolveBinding().getJavaElement();
				System.out.println("TypeParameter (TypeDeclaration)"
						+ "\n\tname   = " + tp.getName()
						+ "\n\tstart  = " + tp.getStartPosition()
						+ "\n\tlength = " + tp.getLength());
				addRenameOpportunity(createRenameTypeParameterDescriptor(element));
			}
		});

		return true;
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
		Optional.ofNullable(node.resolveFieldBinding())
		.map(IVariableBinding::getJavaElement)
		.ifPresent(e -> {
			addRenameOpportunity(createRenameFieldDescriptor(e));
		});
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
	public boolean visit_rename(MethodDeclaration decl) {
		Optional.ofNullable(decl.resolveBinding())
		.map  (b -> (IJavaElement)b.getJavaElement())
		.filter(e -> e instanceof IMethod)
		.ifPresent(element -> {
			IMethod method = (IMethod)element;
			String  tag    = "";
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
				addRenameOpportunity(createRenameMethodDescriptor(element));
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
					addRenameOpportunity(createRenameTypeParameterDescriptor(e));
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
			addRenameOpportunity(createRenameLocalVariableDescriptor(ije));
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
				addRenameOpportunity(createRenameLocalVariableDescriptor(e));
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
				addRenameOpportunity(createRenameLocalVariableDescriptor(e));
			});
		}
		return true;
	}
}
