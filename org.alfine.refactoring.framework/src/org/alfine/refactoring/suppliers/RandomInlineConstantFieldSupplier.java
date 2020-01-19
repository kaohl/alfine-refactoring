package org.alfine.refactoring.suppliers;

import java.util.Iterator;
import java.util.Random;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.utils.ASTHelper;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class RandomInlineConstantFieldSupplier extends RefactoringSupplier {

	public RandomInlineConstantFieldSupplier(Workspace workspace) {
		super(workspace);
		Cache.installCachePath(new InlineConstantFieldDescriptor().getRefactoringID(), "inline.field.txt");
	}

	@Override
	public void cacheOpportunities() {
		visitCompilationUnits(icu -> {
			CompilationUnit cu = ASTHelper.getCompilationUnit(icu);
			cu.accept(new InlineConstantFieldVisitor(getCache(), icu));
		});
	}

	@Override
	public Iterator<RefactoringDescriptor> iterator() {
		return getCache().makeSupplier((Cache cache) -> {

			final org.alfine.refactoring.suppliers.ListSupply supply =
					new org.alfine.refactoring.suppliers.ListSupply();

			cache
			.getCacheLines(new InlineConstantFieldDescriptor().getRefactoringID())
			.forEach(line -> supply.add(new InlineConstantFieldDescriptor(line)));

			Random shuffle  = new Random(getShuffleSeed());
			Random select = new Random(getSelectSeed());

			supply.shuffle(shuffle);

			return supply.iterator(select);
		});
	}
}
