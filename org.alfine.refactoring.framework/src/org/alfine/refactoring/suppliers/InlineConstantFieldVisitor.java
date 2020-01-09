package org.alfine.refactoring.suppliers;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.alfine.refactoring.opportunities.Cache;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;

public class InlineConstantFieldVisitor extends ASTVisitor {

	// It is important to note that we only consider inlining a single
	// reference to a constant field and not all references, nor constant
	// propagation in general, which would be a form of accumulated inlining.

	private Cache            cache;
	private ICompilationUnit unit;
	private Set<Integer>     oppStartSet;   // Source start position for all found opportunities.

	public InlineConstantFieldVisitor(Cache cache, ICompilationUnit icu) {
		this.cache       = cache;
		this.unit        = icu;
		this.oppStartSet = new HashSet<Integer>();
	}

	/** Inline constant at specified location. */
	private InlineConstantFieldDescriptor createInlineConstantFieldDescriptor(int start, int length) {
		String selection = "" + start + " " + length;
		Map<String, String> args = new TreeMap<>();
		args.put("input", this.unit.getHandleIdentifier());
		args.put("element", this.unit.getHandleIdentifier());
		args.put("selection", selection);
		return new InlineConstantFieldDescriptor(args);
	}

	private void addOpportunity(InlineConstantFieldDescriptor descriptor, int start) {
		if (!oppStartSet.contains(start)) {
			oppStartSet.add(start);
			this.cache.write(descriptor);
		}
	}

	@Override
	public boolean visit(SimpleName name) {

		IBinding b = name.resolveBinding();

		if (b != null && !name.isDeclaration()) {

			int     modifiers = b.getModifiers();
			boolean isFinal  = (modifiers & org.eclipse.jdt.core.dom.Modifier.FINAL)  > 0;
			boolean isStatic = (modifiers & org.eclipse.jdt.core.dom.Modifier.STATIC) > 0;

			if (isFinal && isStatic) {
				/*
				addOpportunity(
					new InlineConstantFieldOpportunity(getICompilationUnit(), selection),
					name.getStartPosition()
				);
				*/
				addOpportunity(createInlineConstantFieldDescriptor(name.getStartPosition(), name.getLength()), name.getStartPosition());
			}
		}
		return true;
	}
}
