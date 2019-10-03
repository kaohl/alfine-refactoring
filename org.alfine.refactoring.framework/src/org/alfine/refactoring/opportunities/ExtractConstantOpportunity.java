package org.alfine.refactoring.opportunities;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.ExtractConstantDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;

public class ExtractConstantOpportunity  extends RefactoringOpportunity {

	private ICompilationUnit unit;
	private String           name;
	private String           selection; // "offset length"

	public ExtractConstantOpportunity(ICompilationUnit unit, String name, String selection) {
		super(null);

		this.unit = unit;
		this.name = name;
		this.selection = selection;
	}

	private ICompilationUnit getICompilationUnit() {
		return this.unit;
	}

	private String getName() {
		return this.name;
	}
	private String getSelection() {
		return this.selection;
	}

	@Override
	protected JavaRefactoringDescriptor buildDescriptor() {

		@SuppressWarnings("serial")
		Map<String, String> arguments = new HashMap<String, String>() {{
			put("input", getICompilationUnit().getHandleIdentifier());
			put("element", getICompilationUnit().getHandleIdentifier());
			put("selection", getSelection());
			put("name", getName());
			put("replace", "false"); // Not sure what this argument does.
			put("qualify", "false"); // If "true" the constant is replace with a fully qualified name as opposed to its `SimpleName`.
		}};

		String                    id           = IJavaRefactorings.EXTRACT_CONSTANT;
		RefactoringContribution   contribution = RefactoringCore.getRefactoringContribution(id);
		JavaRefactoringDescriptor defaultInit  = (JavaRefactoringDescriptor)contribution.createDescriptor();
		ExtractConstantDescriptor  descriptor  = (ExtractConstantDescriptor)contribution.createDescriptor(
			id,
			defaultInit.getProject(),
			defaultInit.getDescription(),
			defaultInit.getComment(),
			arguments,
			defaultInit.getFlags()
		);

		return descriptor;
	}
}
