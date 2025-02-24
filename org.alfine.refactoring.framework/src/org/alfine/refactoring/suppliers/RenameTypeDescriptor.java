package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

public class RenameTypeDescriptor extends RenameRefactoringDescriptor {

	public static final String ID = IJavaRefactorings.RENAME_TYPE;

	public RenameTypeDescriptor() {
		this(Collections.emptyMap());
	}

	public RenameTypeDescriptor(Map<String, String> args) {
		super(args);
	}

	@Override
	public String getRefactoringID() {
		return ID;
	}

	@Override
	public void configure() {
		super.configure(); // Generate name.
		put("qualified",            "true");
		put("references",          "true");
		put("similarDeclarations", "false");
		put("textual",             "false");
	}
}
