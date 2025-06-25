package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

public class ExtractTempDescriptor extends RefactoringDescriptor {

	public static final String ID = IJavaRefactorings.EXTRACT_LOCAL_VARIABLE;

	public ExtractTempDescriptor() {
		this(Collections.emptyMap());
	}

	public ExtractTempDescriptor(Map<String, String> args) {
		this(args, Collections.emptyMap());
	}

	public ExtractTempDescriptor(Map<String, String> args, Map<String, String> meta) {
		super(args, meta);
	}

	@Override
	public String getRefactoringID() {
		return ID;
	}
}
