package org.alfine.refactoring.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.alfine.refactoring.framework.Project;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;

public class ASTHelper {
	/*
	 * https://www.eclipse.org/articles/Article-JavaCodeManipulation_AST/
	 * https://stackoverflow.com/questions/12755640/variabledeclarationfragment-node-resolvebindind-returns-null-in-eclipse-jdt-as
	 * https://www.programcreek.com/2014/01/how-to-resolve-bindings-when-using-eclipse-jdt-astparser/
	 */
	
	public static CompilationUnit getCompilationUnit(ICompilationUnit unit) {

		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setProject(Project.getJavaProject());
		parser.setSource(unit);

		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		
		return (CompilationUnit)parser.createAST(null);
	}
	
	public static ASTNode findASTNode(CompilationUnit unit, int start, int length) {
		return org.eclipse.jdt.core.dom.NodeFinder.perform(unit.getRoot(), start, length);
	}

	public static ASTNode findASTNode(CompilationUnit unit, ISourceRange range) {
		return findASTNode(unit, range.getOffset(), range.getLength());
	}

	public static List<ASTNode> getNodeHierarchy(ASTNode node) {
		List<ASTNode> nodes = new LinkedList<>();
		ASTNode n = node;
		while (n != null) {
			nodes.add(n);
			n = n.getParent();
		}
		return new ArrayList<ASTNode>(nodes).reversed();
	}

	public static CompilationUnit getCompilationUnit(ASTNode node) {
		return (CompilationUnit) getNodeHierarchy(node).stream().filter(n -> n instanceof CompilationUnit).findFirst().orElse(null);
	}

	public static List<TypeDeclaration> getTypeDeclarations(ASTNode node) {
		return getNodeHierarchy(node).stream().filter(n -> n instanceof TypeDeclaration).map(TypeDeclaration.class::cast).collect(Collectors.toList());
	}

	public static TypeDeclaration getTypeDeclaration(ASTNode node) {
		return getTypeDeclarations(node).getLast();
	}

	public static MethodDeclaration getMethodDeclaration(ASTNode node) {
		return (MethodDeclaration) getNodeHierarchy(node).stream().filter(n -> n instanceof MethodDeclaration).findFirst().orElse(null);
	}

	public static boolean isMethodParameter(SingleVariableDeclaration node) {
		return node != null && node.getParent() instanceof MethodDeclaration;
	}

	public static boolean isTypeTypeParameter(TypeParameter node) {
		return node != null && node.getParent() instanceof TypeDeclaration;
	}

	public static boolean isMethodTypeParameter(TypeParameter node) {
		return node != null && node.getParent() instanceof MethodDeclaration;
	}

	public static boolean isNameOfTypeParameter(SimpleName node) {
		return node != null && node.getParent() instanceof TypeParameter;
	}

	public static String getFullyQualifiedPackageName(TypeDeclaration decl) {
		return getCompilationUnit(decl).getPackage().getName().getFullyQualifiedName();
	}

	public static String getFullyQualifiedName(ASTNode node) {
		List<String> parts = new LinkedList<>();
		for (ASTNode n : getNodeHierarchy(node).stream().filter(n -> n instanceof CompilationUnit || n instanceof TypeDeclaration || n instanceof MethodDeclaration).collect(Collectors.toList())) {
			if (n instanceof CompilationUnit unit) {
				parts.add(unit.getPackage().getName().getFullyQualifiedName());
			} else if (n instanceof TypeDeclaration type) {
				parts.add(type.getName().getFullyQualifiedName());
			} else if (n instanceof MethodDeclaration method) {
				parts.add(method.getName().getFullyQualifiedName());
			}
		}
		return String.join(".", parts);
	}

	/** Return a signature on the format used by the 'methods.config' file. */
	public static String getMethodSignature(MethodDeclaration node) {
		List<String> paramTypes = new LinkedList<>();
		for (Object p : node.parameters()) {
			String param    = p.toString();
			String typeOnly = param.substring(0, param.lastIndexOf(" ")).trim();
			paramTypes.add(typeOnly);
		}
		return String.format("%s(%s)", getFullyQualifiedName(node),  String.join(", ", paramTypes));
	}

	public static List<String> getDeclarationContext(ASTNode node) {
		return Arrays.asList(getFullyQualifiedName(node).split("\\."));
	}
}
