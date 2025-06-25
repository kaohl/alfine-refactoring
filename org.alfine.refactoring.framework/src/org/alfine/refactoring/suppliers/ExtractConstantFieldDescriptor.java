package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

public class ExtractConstantFieldDescriptor extends RefactoringDescriptor {

	public static final String ID = IJavaRefactorings.EXTRACT_CONSTANT;

	public ExtractConstantFieldDescriptor() {
		this(Collections.emptyMap());
	}

	public ExtractConstantFieldDescriptor(Map<String, String> args) {
		this(args, Collections.emptyMap());
	}

	public ExtractConstantFieldDescriptor(Map<String, String> args, Map<String, String> meta) {
		super(args, meta);
	}

	@Override
	public String getRefactoringID() {
		return ID;
	}
}
