package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;

public class ExtractConstantFieldDescriptor extends RefactoringDescriptor {

	public static final String ID = IJavaRefactorings.EXTRACT_CONSTANT;

	/** The constant is extracted into a field with this name. */
	private static final String NAME = "_ALFINE_CONSTANT_";

	public ExtractConstantFieldDescriptor() {
		this(Collections.emptyMap());
	}

	public ExtractConstantFieldDescriptor(Map<String, String> args) {
		super(args);
	}

	@Override
	public String getRefactoringID() {
		return ID;
	}

	@Override
	protected void configure() {
		put("name", NAME);
		put("replace", "false"); // Not sure what this argument does.
		put("qualify", "false"); // If "true" the constant is replace with a fully qualified name as opposed to its `SimpleName`.
	}

	@Override
	protected void configureJavaRefactoringDescriptor(JavaRefactoringDescriptor descriptor) {
	}
	
}
