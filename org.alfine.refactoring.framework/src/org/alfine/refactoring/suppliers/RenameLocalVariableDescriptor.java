package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

public class RenameLocalVariableDescriptor extends RenameRefactoringDescriptor {

	public static final String ID = IJavaRefactorings.RENAME_LOCAL_VARIABLE;

	public RenameLocalVariableDescriptor() {
		this(Collections.emptyMap());
	}

	public RenameLocalVariableDescriptor(Map<String, String> args) {
		super(args);
	}

	@Override
	public String getRefactoringID() {
		return ID;
	}
}
