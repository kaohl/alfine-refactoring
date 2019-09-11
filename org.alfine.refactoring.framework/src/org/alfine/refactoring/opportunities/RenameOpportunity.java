package org.alfine.refactoring.opportunities;

import org.alfine.refactoring.utils.Generator;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;

public abstract class RenameOpportunity extends RefactoringOpportunity {
	
	private final Generator generator;
	
	public RenameOpportunity(IJavaElement element, Generator generator) {
		super(element);
		this.generator = generator;
	}

	protected Generator getGenerator() {
		return this.generator;
	}


	/** (This method was copied from Autofrob: `autofrob.transform.RenameTransformation.getRenameKind(IJavaElement)'.) */
	private static String getRenameRefactoringDescriptorID(IJavaElement e) {

		switch (e.getElementType()) {

		case IJavaElement.COMPILATION_UNIT:
			return IJavaRefactorings.RENAME_COMPILATION_UNIT;

		case IJavaElement.FIELD:
			return IJavaRefactorings.RENAME_FIELD;

		case IJavaElement.LOCAL_VARIABLE:
			return IJavaRefactorings.RENAME_LOCAL_VARIABLE;

		case IJavaElement.METHOD:
			return IJavaRefactorings.RENAME_METHOD;

		case IJavaElement.TYPE:
			return IJavaRefactorings.RENAME_TYPE;

		case IJavaElement.TYPE_PARAMETER:
			return IJavaRefactorings.RENAME_TYPE_PARAMETER;

		case IJavaElement.PACKAGE_FRAGMENT:
			return IJavaRefactorings.RENAME_PACKAGE;

		case IJavaElement.PACKAGE_FRAGMENT_ROOT:
			return IJavaRefactorings.RENAME_PACKAGE;

		default: throw new RuntimeException("Unexpected object `" + e.getClass() + "' to rename");
		}
	}

	@Override
	protected JavaRefactoringDescriptor getDescriptor(String id) {
		return super.getDescriptor(getRenameRefactoringDescriptorID(getElement()));
	}

	@Override
	protected JavaRefactoringDescriptor buildDescriptor() {

		RenameJavaElementDescriptor descriptor = null;

		descriptor = (RenameJavaElementDescriptor)getDescriptor(""); // Descriptor ID is resolved in `getDescriptor'.
		descriptor.setJavaElement(getElement());

		String oldName = getElement().getElementName();
		String newName = null;

		while ((newName = getGenerator().genName(getElement().getElementType())).equals(oldName));
		
		System.out.println("RenameOpportunity::buildDescriptor(): generated new name: " + newName);
		
		descriptor.setNewName(newName);
		
		descriptor.setUpdateQualifiedNames(
			getElement().getElementType() == IJavaElement.TYPE ||
			getElement().getElementType() == IJavaElement.PACKAGE_FRAGMENT
		);

		descriptor.setUpdateReferences(true);
		descriptor.setDeprecateDelegate(false);
		descriptor.setRenameGetters(false);
		descriptor.setRenameSetters(false);
		descriptor.setKeepOriginal(false);
		descriptor.setUpdateHierarchy(false);
		descriptor.setUpdateSimilarDeclarations(false);

		return descriptor;
	}
}
