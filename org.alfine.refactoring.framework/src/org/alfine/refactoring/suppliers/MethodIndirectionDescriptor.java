package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

public class MethodIndirectionDescriptor extends RefactoringDescriptor {

	public static final String ID = IJavaRefactorings.INTRODUCE_INDIRECTION;

	public MethodIndirectionDescriptor() {
		this(Collections.emptyMap());
	}

	public MethodIndirectionDescriptor(Map<String, String> args) {
		this(args, Collections.emptyMap());
	}

	public MethodIndirectionDescriptor(Map<String, String> args, Map<String, String> meta) {
		super(args, meta);
	}

	@Override
	public String getRefactoringID() {
		return ID;
	}
}
