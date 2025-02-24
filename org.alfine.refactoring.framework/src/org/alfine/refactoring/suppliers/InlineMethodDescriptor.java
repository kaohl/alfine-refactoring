package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;

public class InlineMethodDescriptor extends RefactoringDescriptor {

	public static final String ID = IJavaRefactorings.INLINE_METHOD;

	public InlineMethodDescriptor() {
		this(Collections.emptyMap());
	}

	public InlineMethodDescriptor(Map<String, String> args) {
		super(args);
	}

	@Override
	public String getRefactoringID() {
		return ID;
	}

	@Override
	protected void configure() {
	}

	@Override
	protected void configureJavaRefactoringDescriptor(JavaRefactoringDescriptor descriptor) {
	}

}
