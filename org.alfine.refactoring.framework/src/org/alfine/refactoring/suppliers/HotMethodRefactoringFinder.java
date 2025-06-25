package org.alfine.refactoring.suppliers;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.alfine.refactoring.framework.Workspace;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HotMethodRefactoringFinder {

	private static final Logger logger = LoggerFactory.getLogger(HotMethodRefactoringFinder.class);

	public static CompilationUnit getCompilationUnit(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		CompilationUnit cu = (CompilationUnit)parser.createAST(null);

		boolean hasErrors = false;
	    IProblem[] problems = cu.getProblems();
	    if (problems != null && problems.length > 0) {
	        for (IProblem problem : problems) {
	        	if (problem.isError()) {
	        		hasErrors = true;
	        		logger.warn("Compilation problem: {}", problem);
	        	}
	        }
	    }
	    if (hasErrors) {
		    try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	    }
		return cu;
	}

	private final Workspace workspace;
	private final MethodSet methods;
	
	public HotMethodRefactoringFinder(Workspace workspace) {
		this.workspace = workspace;

		Path methodsFile = getWorkspace().getSrcPath().resolve("methods.config");
		this.methods = new MethodSet(methodsFile);

		if (this.methods.size() == 0) {
			throw new RuntimeException(
				"Bad hot methods configuration: " + methodsFile
			);
		}
	}

	protected Workspace getWorkspace() {
		return this.workspace;
	}

	/** Access workspace cache. */
	protected Cache getCache() {
		return getWorkspace().getCache();
	}

	protected void visitCompilationUnits(Consumer<? super ICompilationUnit> action) {

		List<IPackageFragment> fragments = getSourceFragments();
		
		System.out.println("visitCompilationUnits(), roots = ");

		for (IPackageFragment fragment : fragments) {
			System.out.println("\t" + fragment.getHandleIdentifier());
		}

		List<ICompilationUnit> units = fragments.stream()
		//.sorted((x,y) -> x.getElementName().compareTo(y.getElementName()))
		.flatMap(f -> {
			try {
				return Arrays.asList(f.getCompilationUnits()).stream(); // TODO: Is the order guaranteed to be sorted by filename?
			} catch (JavaModelException e) {}
			return java.util.stream.Stream.empty();
		})
		.collect(Collectors.toList());
		units = units.stream().filter(getCompilationUnitFilter()).collect(Collectors.toList());
		units.forEach(action);
	}

	/** Return sorted list of source roots. (The resulting list should be deterministic.)*/
	protected List<IPackageFragment> getSourceFragments() {
		return getWorkspace().getFragments(getPackageFragmentFilter())
		.stream()
		.sorted(Comparator.comparing(IPackageFragment::getHandleIdentifier))
		.collect(Collectors.toList());
	}

	private Predicate<IPackageFragment> getPackageFragmentFilter() {
		return new Predicate<IPackageFragment>() {
			@Override
			public boolean test(IPackageFragment fragment) {
				if (fragment instanceof IPackageFragment) {
					String  name   = ((IPackageFragment) fragment).getElementName();
					boolean result = methods.hasFragment(name);
					if (result) {
						System.out.println(String.format("INCLUDE (%s): %s", result ? "Y" : "N", name));
					}
					return result;
				}
				return false;
			}
		};
	}

	private Predicate<? super ICompilationUnit> getCompilationUnitFilter() {
		return new Predicate<ICompilationUnit>() {
			@Override
			public boolean test(ICompilationUnit u) {
				return true;
			}
		};
	}

	public void cacheOpportunities() {
		visitCompilationUnits(icu -> {
			CompilationUnit cu = HotMethodRefactoringFinder.getCompilationUnit(icu);
			cu.accept(new HotMethodVisitor(getCache(), icu, cu, this.methods));
		});
	}
}
