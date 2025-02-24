package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;

public class InlineConstantFieldDescriptor extends RefactoringDescriptor {

	public static final String ID = IJavaRefactorings.INLINE_CONSTANT;

	public InlineConstantFieldDescriptor() {
		this(Collections.emptyMap());
	}

	public InlineConstantFieldDescriptor(Map<String, String> args) {
		super(args);
	}

	@Override
	public String getRefactoringID() {
		return ID;
	}

	@Override
	protected void configure() {
		put("replace",    "false"); // Only inline the selected occurrence.
		put("remove",     "false");
		put("comments",   "false");
		put("exceptions", "false");
	}

	@Override
	protected void configureJavaRefactoringDescriptor(JavaRefactoringDescriptor descriptor) {
	}
}
