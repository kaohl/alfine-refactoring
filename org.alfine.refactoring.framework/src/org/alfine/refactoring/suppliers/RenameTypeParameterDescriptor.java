package org.alfine.refactoring.suppliers;

import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

public class RenameTypeParameterDescriptor extends RenameRefactoringDescriptor {

	public static final String ID = IJavaRefactorings.RENAME_TYPE_PARAMETER;

	public RenameTypeParameterDescriptor(Map<String, String> args) {
		super(args);
	}

	@Override
	public String getRefactoringID() {
		return ID;
	}
}
