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
		this(args, Collections.emptyMap());
	}

	public RenameTypeDescriptor(Map<String, String> args, Map<String, String> meta) {
		super(args, meta);
	}

	@Override
	public String getRefactoringID() {
		return ID;
	}

	@Override
	public void configure() {
		super.configure(); // Generate name.
		putArg("qualified",            "true");
		putArg("references",          "true");
		putArg("similarDeclarations", "false");
		putArg("textual",             "false");
	}
}
