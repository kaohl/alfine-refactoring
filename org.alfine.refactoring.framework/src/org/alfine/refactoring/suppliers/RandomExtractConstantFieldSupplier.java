package org.alfine.refactoring.suppliers;

import java.util.Iterator;
import java.util.Random;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.opportunities.Cache;
import org.alfine.refactoring.utils.ASTHelper;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class RandomExtractConstantFieldSupplier extends RefactoringSupplier {

	public RandomExtractConstantFieldSupplier(Workspace workspace) {
		super(workspace);
		Cache.installCachePath(new InlineConstantFieldDescriptor().getRefactoringID(), "extract.field.txt");
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

			final org.alfine.refactoring.opportunities.VectorSupply supply =
					new org.alfine.refactoring.opportunities.VectorSupply();

			cache
			.getCacheLines(new ExtractConstantFieldDescriptor().getRefactoringID())
			.forEach(line -> supply.add(new ExtractConstantFieldDescriptor(line)));

			Random shuffle  = new Random(getShuffleSeed());
			Random select = new Random(getSelectSeed());

			supply.shuffle(shuffle);

			return supply.iterator(select);
		});
	}
}
