package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

public class RenameMethodDescriptor extends RenameRefactoringDescriptor {

	public static final String ID = IJavaRefactorings.RENAME_METHOD;

	public RenameMethodDescriptor() {
		this(Collections.emptyMap());
	}

	public RenameMethodDescriptor(Map<String, String> args) {
		super(args);
	}

	@Override
	public String getRefactoringID() {
		return ID;
	}
}
