package org.alfine.refactoring.suppliers;

import java.util.Iterator;
import java.util.Random;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.utils.ASTHelper;
import org.alfine.refactoring.utils.Generator;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class RandomRenameSupplier extends RefactoringSupplier {

	/* This field is accessed from rename refactoring descriptors. */
	private static Generator generator;

	/** Return publicly available name generator. */
	public static Generator getGenerator() {
		return RandomRenameSupplier.generator;
	}

	/** Create a `RandomRenameSupplier` using the specified
	 * 	seed and offset for its global name generator. */
	public RandomRenameSupplier(Workspace workspace, long seed, int offset) {
		super(workspace);

		if (RandomRenameSupplier.generator == null) {
			RandomRenameSupplier.generator = new Generator(seed, offset);
		}

		Cache.installCachePath(new RenameTypeDescriptor().getRefactoringID(),          "rename.type.txt");
		Cache.installCachePath(new RenameMethodDescriptor().getRefactoringID(),        "rename.method.txt");
		Cache.installCachePath(new RenameFieldDescriptor().getRefactoringID(),         "rename.field.txt");
		Cache.installCachePath(new RenameLocalVariableDescriptor().getRefactoringID(), "rename.local.variable.txt");
		Cache.installCachePath(new RenameTypeParameterDescriptor().getRefactoringID(), "rename.type.parameter.txt");
	}

	/** Equivalent to RandomRenameSupplier(workspace, 0, 0). */
	public RandomRenameSupplier(Workspace workspace) {
		this(workspace, 0, 0);
	}

	/** Set max length of generated names. */
	public void setMaxLength(int length) {
		this.generator.setMaxLength(length);
	}

	/** Fix length of generated names to set length. */
	public void setLengthFixed(boolean fixed) {
		this.generator.setLengthFixed(fixed);
	}

	@Override
	public void cacheOpportunities() {
		visitCompilationUnits(icu -> {
			CompilationUnit cu = ASTHelper.getCompilationUnit(icu);
			cu.accept(new RenameVisitor(getCache(), icu));
		});
	}

	@Override
	public Iterator<RefactoringDescriptor> iterator() {
		return getCache().makeSupplier((Cache cache) -> {

			final org.alfine.refactoring.suppliers.ListSupply supply =
				new org.alfine.refactoring.suppliers.ListSupply();

			cache
			.getCacheLines(new RenameTypeDescriptor().getRefactoringID())
			.forEach(line -> supply.add(new RenameTypeDescriptor(line)));

			cache
			.getCacheLines(new RenameMethodDescriptor().getRefactoringID())
			.forEach(line -> supply.add(new RenameMethodDescriptor(line)));

			cache
			.getCacheLines(new RenameFieldDescriptor().getRefactoringID())
			.forEach(line -> supply.add(new RenameFieldDescriptor(line)));

			cache
			.getCacheLines(new RenameLocalVariableDescriptor().getRefactoringID())
			.forEach(line -> supply.add(new RenameLocalVariableDescriptor(line)));

			cache
			.getCacheLines(new RenameTypeParameterDescriptor().getRefactoringID())
			.forEach(line -> supply.add(new RenameTypeParameterDescriptor(line)));

			Random shuffle  = new Random(getShuffleSeed());
			Random select = new Random(getSelectSeed());

			supply.shuffle(shuffle);

			return supply.iterator(select);
		});
	}
}
