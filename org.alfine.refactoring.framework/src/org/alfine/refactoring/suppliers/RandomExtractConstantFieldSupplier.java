package org.alfine.refactoring.suppliers;

import java.util.Iterator;
import java.util.Random;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.utils.ASTHelper;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class RandomExtractConstantFieldSupplier extends RefactoringSupplier {

	public RandomExtractConstantFieldSupplier(Workspace workspace) {
		super(workspace);
		Cache.installCachePath(ExtractConstantFieldDescriptor.ID, "extract.field.txt");
	}

	@Override
	public void cacheOpportunities() {
		visitCompilationUnits(icu -> {
			CompilationUnit cu = ASTHelper.getCompilationUnit(icu);
			cu.accept(new ExtractConstantFieldVisitor(getCache(), icu));
		});
	}

	@Override
	public Iterator<RefactoringDescriptor> iterator() {
		return getCache().makeSupplier((Cache cache) -> {

			final org.alfine.refactoring.suppliers.ListSupply supply =
					new org.alfine.refactoring.suppliers.ListSupply();

			cache
			.getCacheLines(ExtractConstantFieldDescriptor.ID)
			.forEach(line -> supply.add(RefactoringDescriptorFactory.get(line)));

			Random shuffle  = new Random(getShuffleSeed());
			Random select = new Random(getSelectSeed());

			supply.shuffle(shuffle);

			return supply.iterator(select);
		});
	}
}
