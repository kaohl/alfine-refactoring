package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

public class RenameFieldDescriptor extends RenameRefactoringDescriptor {

	public static final String ID = IJavaRefactorings.RENAME_FIELD;

	public RenameFieldDescriptor() {
		this(Collections.emptyMap());
	}

	public RenameFieldDescriptor(Map<String, String> args) {
		super(args);
	}

	@Override
	public String getRefactoringID() {
		return ID;
	}
}
