package org.alfine.refactoring.suppliers;

import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;

public class InlineMethodDescriptor extends RefactoringDescriptor {

	public InlineMethodDescriptor() {
	}

	public InlineMethodDescriptor(Map<String, String> args) {
		super(args);
	}

	public InlineMethodDescriptor(String cacheLine) {
		super(cacheLine);
	}

	@Override
	public String getRefactoringID() {
		return IJavaRefactorings.INLINE_METHOD;
	}

	@Override
	protected void configure() {
	}

	@Override
	protected void configureJavaRefactoringDescriptor(JavaRefactoringDescriptor descriptor) {
	}

}
