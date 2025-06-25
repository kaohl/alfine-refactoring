package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

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
}
