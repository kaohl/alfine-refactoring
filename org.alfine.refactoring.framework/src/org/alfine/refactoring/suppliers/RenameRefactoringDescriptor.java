package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Map;

public abstract class RenameRefactoringDescriptor extends RefactoringDescriptor {

	public RenameRefactoringDescriptor(Map<String, String> args) {
		this(args, Collections.emptyMap());
	}

	public RenameRefactoringDescriptor(Map<String, String> args, Map<String, String> meta) {
		super(args, meta);
	}
}
