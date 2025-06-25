package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

public class ExtractMethodDescriptor extends RefactoringDescriptor {

	public static final String ID = IJavaRefactorings.EXTRACT_METHOD;

	public ExtractMethodDescriptor() {
		this(Collections.emptyMap());
	}

	public ExtractMethodDescriptor(Map<String, String> args) {
		this(args, Collections.emptyMap());
	}

	public ExtractMethodDescriptor(Map<String, String> args, Map<String, String> meta) {
		super(args, meta);
	}

	@Override
	public String getRefactoringID() {
		return ID;
	}
}
