package org.alfine.refactoring.suppliers;

import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

public class RenameLocalVariableDescriptor extends RenameRefactoringDescriptor {

	public RenameLocalVariableDescriptor() {
	}

	public RenameLocalVariableDescriptor(Map<String, String> args) {
		super(args);
	}

	public RenameLocalVariableDescriptor(String cacheLine) {
		super(cacheLine);
	}

	@Override
	public String getRefactoringID() {
		return IJavaRefactorings.RENAME_LOCAL_VARIABLE;
	}
}
