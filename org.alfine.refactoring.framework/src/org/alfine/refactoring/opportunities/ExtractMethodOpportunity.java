package org.alfine.refactoring.opportunities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.ExtractMethodDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;

public class ExtractMethodOpportunity extends RefactoringOpportunity {

	private ICompilationUnit unit;

	// Start and length of statement block within `Block` to be extracted.
	private int              blockStart;
	private int              blockLength;

	public ExtractMethodOpportunity(ICompilationUnit unit, Block block, int startIndex, int endIndex) {
		super(null);

		@SuppressWarnings("unchecked")
		List<Statement> stmtList = block.statements().subList(startIndex, endIndex + 1);

		Statement first = stmtList.get(0);
		Statement last = stmtList.get(stmtList.size() - 1);

		this.unit        = unit;
		this.blockStart  = first.getStartPosition();
		this.blockLength = last.getStartPosition() - blockStart + last.getLength();
	}

	/** Create a refactoring opportunity from string represenation (cache line format). */
	public static ExtractMethodOpportunity makeOpportunityFromString(String cacheLine) {
		ExtractMethodOpportunity
	}

	private ICompilationUnit getICompilationUnit() {
		return this.unit;
	}

	private int getBlockStart() {
		return this.blockStart;
	}

	private int getBlockLength() {
		return this.blockLength;
	}

	@Override
	public String getCacheLine() {
		return new StringBuilder()
			.append(getBlockStart())
			.append(" ")
			.append(getBlockLength())
			.append(" ")
			.append(getICompilationUnit().getHandleIdentifier())
			.toString();
	}

	@Override
	protected JavaRefactoringDescriptor buildDescriptor() {

		// Note: Here is a list of limitations of the extract method refactoring:
		//       https://help.eclipse.org/kepler/index.jsp?topic=%2Forg.eclipse.jdt.doc.user%2Fconcepts%2Fconcept-refactoring.htm

		// Note: The easiest way to get a list of required arguments for a descriptor
		//       where the documentation is lacking is to generate a refactoring script
		//       of the refactoring type you are working with and then look in the script
		//       for argument names and values.
		//           To generate a script, start eclipse workbench and go to menu:
		//       Refactor-> Create Script ..., after applying a similar refactoring to
		//       what you are looking for in an arbitrary workspace. You will then be able
		//       to save that refactoring as a script (.xml file) and inspect arguments.
		//
		// Note: It is only possible to create a refactoring script from a refactoring
		//       that has already been applied.

		@SuppressWarnings("serial")
		Map<String, String> arguments = new HashMap<String, String>() {{
			put("input", getICompilationUnit().getHandleIdentifier());
			put("element", getICompilationUnit().getHandleIdentifier());
			put("selection", "" + getBlockStart() + " " + getBlockLength());
			put("name", "extractedMethodByAlfine");
			put("visibility", "2"); // private

			// I'm unsure what the following arguments do but they
			// are required.

			put("replace", "true");
			put("comments", "false");
			put("exceptions", "false");

		}};

		String                    id           = IJavaRefactorings.EXTRACT_METHOD;
		RefactoringContribution   contribution = RefactoringCore.getRefactoringContribution(id);
		JavaRefactoringDescriptor defaultInit  = (JavaRefactoringDescriptor)contribution.createDescriptor();
		ExtractMethodDescriptor   descriptor   = (ExtractMethodDescriptor)contribution.createDescriptor(
			id,
			defaultInit.getProject(),
			defaultInit.getDescription(),
			defaultInit.getComment(),
			arguments,
			defaultInit.getFlags()
		);

		return descriptor;
	}
}
