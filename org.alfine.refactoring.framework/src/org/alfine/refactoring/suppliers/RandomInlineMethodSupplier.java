package org.alfine.refactoring.suppliers;

import java.util.Iterator;
import java.util.Random;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.utils.ASTHelper;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomInlineMethodSupplier extends RefactoringSupplier {

	public RandomInlineMethodSupplier(Workspace workspace) {
		super(workspace);
		Cache.installCachePath(InlineMethodDescriptor.ID, "inline.method.txt");
	}

	@Override
	public void cacheOpportunities() {
		visitCompilationUnits(icu -> {
			CompilationUnit cu = ASTHelper.getCompilationUnit(icu);
			cu.accept(new InlineMethodVisitor(getCache(), icu));
		});
		Logger logger = LoggerFactory.getLogger(RandomInlineMethodSupplier.class);
		logger.info("Total number of method invocations `{}`", InlineMethodVisitor.nInvocations);
	}

	@Override
	public Iterator<RefactoringDescriptor> iterator() {
		return getCache().makeSupplier((Cache cache) -> {

			final org.alfine.refactoring.suppliers.ListSupply supply =
				new org.alfine.refactoring.suppliers.ListSupply();

			cache
			.getCacheLines(InlineMethodDescriptor.ID)
			.forEach(line -> supply.add(RefactoringDescriptorFactory.get(line)));

			Random shuffle  = new Random(getShuffleSeed());
			Random select = new Random(getSelectSeed());

			supply.shuffle(shuffle);

			return supply.iterator(select);
		});
	}
}
