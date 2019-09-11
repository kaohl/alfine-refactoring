package org.alfine.refactoring.opportunities;

import org.alfine.refactoring.utils.ASTHelper;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;
import org.eclipse.ltk.core.refactoring.Refactoring;

public class InlineMethodOpportunity extends RefactoringOpportunity {

	private ICompilationUnit unit;
	private int              start;
	private int              length;

	public InlineMethodOpportunity(IJavaElement element, ICompilationUnit unit, int start, int length) {
		super(element);

		this.unit   = unit;
		this.start  = start;
		this.length = length;
	}

	private ICompilationUnit getICompilationUnit() {
		return this.unit;
	}

	private int getStart() {
		return this.start;
	}

	private int getLength() {
		return this.length;
	}

	@Override
	protected JavaRefactoringDescriptor buildDescriptor() {
		
		
		// This method gives an example of how to use the InlineMethodDescriptor!
		// https://android.wekeepcoding.com/article/20191382/How+to+execute+inline+refactoring+programmatically+using+JDT+LTK%3F
		
		
		
		
		
		
		
		
		

		/*
		 * Can't find out how to set information on the descriptor.
		 * There are no methods available for that, that I can see.
		 * I did not find any good examples or instructions online
		 * for how to use the inline descriptor.
		 *
		 * Where is the source code for UI components?
		 * 
		 * Fully qualified name: org.eclipse.jdt.core.refactoring.descriptors.InlineMethodDescriptor

		InlineMethodDescriptor descriptor = (InlineMethodDescriptor)getDescriptor(IJavaRefactorings.INLINE_METHOD);

		 */
		
		/*
		Source code hinting about how to configure descriptors:

		https://git.eclipse.org/c/jdt/eclipse.jdt.ui.git/diff/org.eclipse.jdt.ui/core%20refactoring/org/eclipse/jdt/internal/corext/refactoring/JavaRefactoringDescriptorUtil.java?id=eb988be06d10ed5bf37b1e1e6a7b22450e7aa6ec
		https://git.eclipse.org/c/jdt/eclipse.jdt.ui.git/diff/org.eclipse.jdt.ui/core%20refactoring/org/eclipse/jdt/internal/corext/refactoring/JavaRefactoringArguments.java?id=eb988be06d10ed5bf37b1e1e6a7b22450e7aa6ec

		This link (org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorUtil):
		    https://git.eclipse.org/c/jdt/eclipse.jdt.ui.git/diff/org.eclipse.jdt.ui/core%20refactoring/org/eclipse/jdt/internal/corext/refactoring/JavaRefactoringDescriptorUtil.java?id=eb988be06d10ed5bf37b1e1e6a7b22450e7aa6ec
		shows some of the available descriptor arguments.

		Where do we find the remaining arguments?
		 */

		
		return null;
	}
	
	@Override
	protected Refactoring createRefactoring() {

		// We override parent method to avoid using the descriptor, because I can't find
		// how to set arguments to InlineMethodDescriptor (see buildDescriptor above.).

		ICompilationUnit icunit = getICompilationUnit();
		CompilationUnit  cunit  = ASTHelper.getCompilationUnit(icunit);

		return InlineMethodRefactoring.create(icunit, cunit, getStart(), getLength());
	}
}
