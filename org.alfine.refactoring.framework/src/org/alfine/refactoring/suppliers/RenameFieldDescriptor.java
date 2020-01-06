package org.alfine.refactoring.suppliers;

import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

public class RenameFieldDescriptor extends RenameRefactoringDescriptor {

	public RenameFieldDescriptor() {
	}

	public RenameFieldDescriptor(Map<String, String> args) {
		super(args);
	}

	public RenameFieldDescriptor(String cacheLine) {
		super(cacheLine);
	}

	@Override
	public String getRefactoringID() {
		return IJavaRefactorings.RENAME_FIELD;
	}
}
