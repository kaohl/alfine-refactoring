package org.alfine.refactoring.utils;

import org.alfine.refactoring.framework.Project;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class ASTHelper {
	
	/*
	 * https://www.eclipse.org/articles/Article-JavaCodeManipulation_AST/
	 * https://stackoverflow.com/questions/12755640/variabledeclarationfragment-node-resolvebindind-returns-null-in-eclipse-jdt-as
	 * https://www.programcreek.com/2014/01/how-to-resolve-bindings-when-using-eclipse-jdt-astparser/
	 */
	
	public static CompilationUnit getCompilationUnit(ICompilationUnit unit) {

		ASTParser parser = ASTParser.newParser(AST.JLS8);
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
}
