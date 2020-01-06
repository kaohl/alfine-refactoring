package org.alfine.refactoring.suppliers;

import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

public class RenameTypeDescriptor extends RenameRefactoringDescriptor {

	public RenameTypeDescriptor() {
	}

	public RenameTypeDescriptor(Map<String, String> args) {
		super(args);
	}

	public RenameTypeDescriptor(String cacheLine) {
		super(cacheLine);
	}

	@Override
	public String getRefactoringID() {
		return IJavaRefactorings.RENAME_TYPE;
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
