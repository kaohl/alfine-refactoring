package org.alfine.refactoring.suppliers;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;

public abstract class RenameRefactoringDescriptor extends RefactoringDescriptor {

	public RenameRefactoringDescriptor(Map<String, String> args) {
		this(args, Collections.emptyMap());
	}

	public RenameRefactoringDescriptor(Map<String, String> args, Map<String, String> meta) {
		super(args, meta);
	}

	/** Generate a random name argument for a rename descriptor. */
	public void generateName() {
		
		String name = null;

		switch (getRefactoringID()) {
	
		case IJavaRefactorings.RENAME_TYPE:
			name = RandomRenameSupplier.getGenerator().genName(IJavaElement.TYPE);
			break;

		case IJavaRefactorings.RENAME_METHOD:
			name = RandomRenameSupplier.getGenerator().genName(IJavaElement.METHOD);
			break;

		case IJavaRefactorings.RENAME_FIELD:
			name = RandomRenameSupplier.getGenerator().genName(IJavaElement.FIELD);
			break;

		case IJavaRefactorings.RENAME_LOCAL_VARIABLE:
			name = RandomRenameSupplier.getGenerator().genName(IJavaElement.LOCAL_VARIABLE);
			break;

		case IJavaRefactorings.RENAME_TYPE_PARAMETER:
			name = RandomRenameSupplier.getGenerator().genName(IJavaElement.TYPE_PARAMETER);
			break;

		default:
			throw new RuntimeException("Expected rename descriptor but found id = " + getRefactoringID());
		}

		putArg("name", name);
	}

	@Override
	public void configure() {
		// generateName();
	}

	@Override
	public void configureJavaRefactoringDescriptor(JavaRefactoringDescriptor descriptor) {
		RenameJavaElementDescriptor d = (RenameJavaElementDescriptor)descriptor;

		d.setUpdateReferences(true);
		d.setDeprecateDelegate(false);
		d.setRenameGetters(false);
		d.setRenameSetters(false);
		d.setKeepOriginal(false);
		d.setUpdateHierarchy(false);
		d.setUpdateSimilarDeclarations(false);
	}
}
