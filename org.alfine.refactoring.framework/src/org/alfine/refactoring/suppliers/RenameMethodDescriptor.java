package org.alfine.refactoring.suppliers;

import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

public class RenameMethodDescriptor extends RenameRefactoringDescriptor {

	public RenameMethodDescriptor() {
	}

	public RenameMethodDescriptor(Map<String, String> args) {
		super(args);
	}

	public RenameMethodDescriptor(String cacheLine) {
		super(cacheLine);
	}

	@Override
	public String getRefactoringID() {
		return IJavaRefactorings.RENAME_METHOD;
	}
}
