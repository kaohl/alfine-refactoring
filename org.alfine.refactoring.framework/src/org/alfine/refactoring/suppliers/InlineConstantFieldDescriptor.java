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
		this(args, Collections.emptyMap());
	}

	public InlineConstantFieldDescriptor(Map<String, String> args, Map<String, String> meta) {
		super(args, meta);
	}

	@Override
	public String getRefactoringID() {
		return ID;
	}

	@Override
	protected void configure() {
		putArg("replace",    "false"); // Only inline the selected occurrence.
		putArg("remove",     "false");
		putArg("comments",   "false");
		putArg("exceptions", "false");
	}

	@Override
	protected void configureJavaRefactoringDescriptor(JavaRefactoringDescriptor descriptor) {
	}
}
