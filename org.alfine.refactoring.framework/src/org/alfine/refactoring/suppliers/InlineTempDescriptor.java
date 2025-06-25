package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

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
}
