package org.alfine.refactoring.suppliers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Vector;

import org.alfine.refactoring.framework.Workspace;
import org.alfine.refactoring.framework.launch.Main;
import org.alfine.refactoring.utils.ASTHelper;
import org.alfine.refactoring.utils.Generator;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class RandomInlineMethodSupplier extends RefactoringSupplier {

	public RandomInlineMethodSupplier(Workspace workspace, Generator generator) {
		super(workspace, generator);
	}

	@Override
	protected Supply collectOpportunities() {

		Vector<Long> nbrInvocations = new Vector<>();

		VectorSupply supply = new VectorSupply();

		visitCompilationUnits(icu -> {
			CompilationUnit cu = ASTHelper.getCompilationUnit(icu);
			InlineMethodVisitor visitor = new InlineMethodVisitor(icu, supply);
			cu.accept(visitor);

			nbrInvocations.add(visitor.getNbrInvocations());
		});

		try (OutputStream out = Files.newOutputStream(Paths.get(System.getProperty(Main.LOGFILE_KEY)))) {

			long sum = 0;

			for (Long n : nbrInvocations) {
				sum += n;
			}

			out.write(("nbrOpportunities = " + supply.size() + ", nbrInvocations = " + sum).getBytes());

		} catch (IOException e) {
			e.printStackTrace();
		}
		return supply;
	}
}
