package org.alfine.refactoring.suppliers;

import java.util.Iterator;
import java.util.Random;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.opportunities.Cache;
import org.alfine.refactoring.utils.ASTHelper;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class RandomExtractMethodSupplier extends RefactoringSupplier {

	public RandomExtractMethodSupplier(Workspace workspace) {
		super(workspace);
		Cache.installCachePath(new ExtractMethodDescriptor().getRefactoringID(), "extract.method.txt");
	}

	@Override
	public void cacheOpportunities() {
		visitCompilationUnits(icu -> {
			CompilationUnit cu = ASTHelper.getCompilationUnit(icu);
			cu.accept(new ExtractMethodVisitor(getCache(), icu));
		});
	}

	@Override
	public Iterator<RefactoringDescriptor> iterator() {
		return getCache().makeSupplier((Cache cache) -> {

			final org.alfine.refactoring.opportunities.HistSupply supply =
				new org.alfine.refactoring.opportunities.HistSupply();

			cache
			.getCacheLines(new ExtractMethodDescriptor().getRefactoringID())
			.forEach(line -> supply.add(new ExtractMethodDescriptor(line)));

			Random shuffle  = new Random(getShuffleSeed());
			Random select = new Random(getSelectSeed());

			supply.shuffle(shuffle);

			return supply.iterator(select);
		});
	}
}
