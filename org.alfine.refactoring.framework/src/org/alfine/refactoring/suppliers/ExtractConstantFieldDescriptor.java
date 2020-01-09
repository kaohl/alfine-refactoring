package org.alfine.refactoring.suppliers;

import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;

public class ExtractConstantFieldDescriptor extends RefactoringDescriptor {

	/** The constant is extracted into a field with this name. */
	private static final String NAME = "_ALFINE_CONSTANT_";

	public ExtractConstantFieldDescriptor() {
	}

	public ExtractConstantFieldDescriptor(Map<String, String> args) {
		super(args);
	}

	public ExtractConstantFieldDescriptor(String cacheLine) {
		super(cacheLine);
	}

	@Override
	public String getRefactoringID() {
		return IJavaRefactorings.EXTRACT_CONSTANT;
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
