package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;

public class ExtractMethodDescriptor extends RefactoringDescriptor {

	public static final String ID = IJavaRefactorings.EXTRACT_METHOD;

	public ExtractMethodDescriptor() {
		this(Collections.emptyMap());
	}

	public ExtractMethodDescriptor(Map<String, String> args) {
		super(args);
	}

	@Override
	public String getRefactoringID() {
		return ID;
	}

	/*
	@Override
	public int histBin() {
		// Map to one less than length since length is always > 0
		// (we always extract at least one statement).
		return Integer.parseInt(get(RefactoringDescriptor.KEY_HIST_BIN)) - 1;
	}
	*/

	@Override
	protected void configure() {
		put("name", "extractedMethodByAlfine");
		put("visibility", "2"); // private
		put("replace", "true");
		put("comments", "false");
		put("exceptions", "false");
	}

	@Override
	protected void configureJavaRefactoringDescriptor(JavaRefactoringDescriptor descriptor) {
	}
}
