package org.alfine.refactoring.opportunities;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.InlineConstantDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;

public class InlineConstantFieldOpportunity extends RefactoringOpportunity {

	private ICompilationUnit unit;
	private String           selection; // "offset length"

	public InlineConstantFieldOpportunity(ICompilationUnit unit, String selection) {
		super(null);

		this.unit = unit;
		this.selection = selection;
	}

	private ICompilationUnit getICompilationUnit() {
		return this.unit;
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
			put("replace", "false");  // If set to "true", all occurrences are inlined, otherwise only the selection is inlined.

			// I'm unsure what the following arguments do but they
			// are required.

			put("remove", "false");
			put("comments", "false");
			put("exceptions", "false");
		}};

		String                    id           = IJavaRefactorings.INLINE_CONSTANT;
		RefactoringContribution   contribution = RefactoringCore.getRefactoringContribution(id);
		JavaRefactoringDescriptor defaultInit  = (JavaRefactoringDescriptor)contribution.createDescriptor();
		InlineConstantDescriptor  descriptor   = (InlineConstantDescriptor)contribution.createDescriptor(
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
