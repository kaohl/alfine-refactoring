package org.alfine.refactoring.suppliers;

import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;

public class InlineConstantFieldDescriptor extends RefactoringDescriptor {

	public InlineConstantFieldDescriptor() {
	}

	public InlineConstantFieldDescriptor(Map<String, String> args) {
		super(args);
	}

	public InlineConstantFieldDescriptor(String cacheLine) {
		super(cacheLine);
	}

	@Override
	public String getRefactoringID() {
		return IJavaRefactorings.INLINE_CONSTANT;
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
