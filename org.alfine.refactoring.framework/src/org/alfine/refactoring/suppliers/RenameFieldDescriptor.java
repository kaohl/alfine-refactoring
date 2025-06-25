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
		this(args, Collections.emptyMap());
	}

	public RenameFieldDescriptor(Map<String, String> args, Map<String, String> meta) {
		super(args, meta);
	}

	@Override
	public String getRefactoringID() {
		return ID;
	}
}
