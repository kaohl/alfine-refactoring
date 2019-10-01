package org.alfine.refactoring.suppliers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Vector;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.framework.launch.Main;
import org.alfine.refactoring.opportunities.RefactoringOpportunity;
import org.alfine.refactoring.utils.ASTHelper;
import org.alfine.refactoring.utils.Generator;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class RandomInlineMethodSupplier extends RefactoringSupplier {

	public RandomInlineMethodSupplier(Workspace workspace, Generator generator) {
		super(workspace, generator);
	}

	@Override
	protected Vector<RefactoringOpportunity> collectOpportunities() {

		/*
		IJavaProject project = getProject();

		// Note: We must make sure that imported sources are listed on the classpath in a
		//       deterministic way so that we always get the same order of opportunities.
		//
		// Note: Package fragment roots and package fragments are returned in order of appearance
		//       on the classpath.

		Vector<RefactoringOpportunity> opportunities = new Vector<>();

		try {
			for (IPackageFragment frag : project.getPackageFragments()) {
				for (ICompilationUnit icu : frag.getCompilationUnits()) {
					CompilationUnit cu = ASTHelper.getCompilationUnit(icu);
					cu.accept(new InlineVisitor(icu, opportunities));
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		*/

		Vector<Long> nbrInvocations = new Vector<>();

		Vector<RefactoringOpportunity> opportunities = new Vector<>();

		visitCompilationUnits(icu -> {
			CompilationUnit cu = ASTHelper.getCompilationUnit(icu);
			InlineMethodVisitor visitor = new InlineMethodVisitor(icu, opportunities);
			cu.accept(visitor);

			nbrInvocations.add(visitor.getNbrInvocations());
		});

		try (OutputStream out = Files.newOutputStream(Paths.get(System.getProperty(Main.LOGFILE_KEY)))) {

			long sum = 0;

			for (Long n : nbrInvocations) {
				sum += n;
			}

			out.write(("nbrOpportunities = " + opportunities.size() + ", nbrInvocations = " + sum).getBytes());

		} catch (IOException e) {
			e.printStackTrace();
		}
		return opportunities;
	}
}
