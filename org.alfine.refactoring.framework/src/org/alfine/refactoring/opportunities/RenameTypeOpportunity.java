package org.alfine.refactoring.opportunities;

import org.alfine.refactoring.utils.Generator;
import org.eclipse.jdt.core.IJavaElement;

public class RenameTypeOpportunity extends RenameOpportunity {

	public RenameTypeOpportunity(IJavaElement element, Generator generator) {
		super(element, generator);
	}

	/*
	@Override
	protected JavaRefactoringDescriptor buildDescriptor() {

		RenameJavaElementDescriptor descriptor = null;

		descriptor = (RenameJavaElementDescriptor)getDescriptor(IJavaRefactorings.RENAME_TYPE);
		descriptor.setJavaElement(getElement());

		String oldName = getElement().getElementName();
		String newName = null;
		
		while ((newName = getGenerator().genTypeName()).equals(oldName));
		
		descriptor.setNewName(newName);

		descriptor.setUpdateQualifiedNames(true);
		descriptor.setUpdateReferences(true);
		descriptor.setDeprecateDelegate(false);
		descriptor.setRenameGetters(false);
		descriptor.setRenameSetters(false);
		descriptor.setKeepOriginal(false);
		descriptor.setUpdateHierarchy(false);
		descriptor.setUpdateSimilarDeclarations(false);

		return descriptor;
	}
	*/
}
