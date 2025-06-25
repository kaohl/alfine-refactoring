package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

public class InlineMethodDescriptor extends RefactoringDescriptor {

	public static final String ID = IJavaRefactorings.INLINE_METHOD;

	public InlineMethodDescriptor() {
		this(Collections.emptyMap());
	}

	public InlineMethodDescriptor(Map<String, String> args) {
		this(args, Collections.emptyMap());
	}

	public InlineMethodDescriptor(Map<String, String> args, Map<String, String> meta) {
		super(args, meta);
	}

	@Override
	public String getRefactoringID() {
		return ID;
	}
}
