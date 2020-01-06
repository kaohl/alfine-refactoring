package org.alfine.refactoring.suppliers;

import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

public class RenameTypeParameterDescriptor extends RenameRefactoringDescriptor {

	public RenameTypeParameterDescriptor() {
	}

	public RenameTypeParameterDescriptor(Map<String, String> args) {
		super(args);
	}

	public RenameTypeParameterDescriptor(String cacheLine) {
		super(cacheLine);
	}

	@Override
	public String getRefactoringID() {
		return IJavaRefactorings.RENAME_TYPE_PARAMETER;
	}
}
