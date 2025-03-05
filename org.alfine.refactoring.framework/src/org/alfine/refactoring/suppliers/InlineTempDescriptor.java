package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;

public class InlineTempDescriptor extends RefactoringDescriptor {

	public static final String ID = IJavaRefactorings.INLINE_LOCAL_VARIABLE;

	public InlineTempDescriptor() {
		this(Collections.emptyMap());
	}

	public InlineTempDescriptor(Map<String, String> args) {
		this(args, Collections.emptyMap());
	}

	public InlineTempDescriptor(Map<String, String> args, Map<String, String> meta) {
		super(args, meta);
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
