package org.alfine.refactoring.suppliers;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;

public class ExtractMethodVisitor extends ASTVisitor {
	private Cache            cache;
	private ICompilationUnit unit;

	public ExtractMethodVisitor(Cache cache, ICompilationUnit unit) {
		this.unit  = unit;
		this.cache = cache;
	}

	private void addOpportunity(RefactoringDescriptor descriptor) {
		this.cache.write(descriptor);
	}

	@SuppressWarnings("unchecked")
	private ExtractMethodDescriptor createExtractMethodDescriptor(Block block, int start, int end) {

		List<Statement> stmtList = null;

		stmtList = block.statements().subList(start, end + 1);

		Statement first = stmtList.get(0);
		Statement last = stmtList.get(stmtList.size() - 1);

		int blockStart  = first.getStartPosition();
		int blockLength = last.getStartPosition() - blockStart + last.getLength();

		Map<String, String> args = new TreeMap<>();

		args.put("input", this.unit.getHandleIdentifier());
		args.put("element", this.unit.getHandleIdentifier());
		args.put("selection", "" + blockStart + " " + blockLength);

		// This "argument" is only for mapping the opportunity
		// to the correct location in the histogram supply.

		args.put(ExtractMethodDescriptor.KEY_NBR_STMTS, "" + (end - start + 1));

		return new ExtractMethodDescriptor(args);
	}

	@Override
	public boolean visit(Block block) {
		// Note: This does not cover (non-block) single statement bodies of
		//       control flow statements. But let's ignore that for now.

		int nbrStmts = block.statements().size();

		if (nbrStmts > 0) {
			for (int start = 0; start < nbrStmts; ++start) {
				for (int end = start; end < nbrStmts; ++end) {
					// int length = end - start + 1;
					// addOpportunity(length - 1, new ExtractMethodOpportunity(getCompilationUnit(), block, start, end));
					addOpportunity(createExtractMethodDescriptor(block, start, end));
				}
			}
		}
		 // Visit recursive blocks.
		return true;
	}
}
