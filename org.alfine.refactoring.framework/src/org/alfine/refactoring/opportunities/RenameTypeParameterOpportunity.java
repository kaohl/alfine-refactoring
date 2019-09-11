package org.alfine.refactoring.opportunities;

import org.alfine.refactoring.utils.Generator;
import org.eclipse.jdt.core.IJavaElement;

public class RenameTypeParameterOpportunity extends RenameOpportunity {

	public RenameTypeParameterOpportunity(IJavaElement element, Generator generator) {
		super(element, generator);
	}

	/*
	@Override
	protected JavaRefactoringDescriptor buildDescriptor() {

		RenameJavaElementDescriptor descriptor = null;

		descriptor = (RenameJavaElementDescriptor)getDescriptor(IJavaRefactorings.RENAME_TYPE_PARAMETER);
		descriptor.setJavaElement(getElement());

		String oldName = getElement().getElementName();
		String newName = null;
		
		while ((newName = getGenerator().genTypeName()).equals(oldName));
		
		descriptor.setNewName(newName);

		descriptor.setUpdateQualifiedNames(false); // Autofrob only set this to true for types and package fragments.
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
