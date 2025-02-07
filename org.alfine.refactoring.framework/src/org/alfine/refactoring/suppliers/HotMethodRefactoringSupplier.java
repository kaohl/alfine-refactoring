package org.alfine.refactoring.suppliers;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
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

public class HotMethodRefactoringSupplier implements Iterable<RefactoringDescriptor> {

	private static final Logger logger = LoggerFactory.getLogger(HotMethodRefactoringSupplier.class);

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
	private long            shuffleSeed;
	private long            selectSeed;
	private Set<String>     methodQNameAndPList;
	private Set<String>     methods;
	private Set<String>     includedFragments;
	
	public HotMethodRefactoringSupplier(Workspace workspace) {
		this.workspace = workspace;
		this.methodQNameAndPList = new HashSet<>();
		this.methods   = new HashSet<>();
		this.includedFragments = new HashSet<>();

		Cache.installCachePath(new ExtractConstantFieldDescriptor().getRefactoringID(), "hot.extract.field.txt");
		Cache.installCachePath(new InlineConstantFieldDescriptor().getRefactoringID(), "hot.inline.field.txt");
		Cache.installCachePath(new InlineMethodDescriptor().getRefactoringID(), "hot.inline.method.txt");
		Cache.installCachePath(new ExtractMethodDescriptor().getRefactoringID(), "hot.extract.method.txt");
		
		Cache.installCachePath(new RenameTypeDescriptor().getRefactoringID(),          "hot.rename.type.txt");
		Cache.installCachePath(new RenameMethodDescriptor().getRefactoringID(),        "hot.rename.method.txt");
		Cache.installCachePath(new RenameFieldDescriptor().getRefactoringID(),         "hot.rename.field.txt");
		Cache.installCachePath(new RenameLocalVariableDescriptor().getRefactoringID(), "hot.rename.local.variable.txt");
		Cache.installCachePath(new RenameTypeParameterDescriptor().getRefactoringID(), "hot.rename.type.parameter.txt");

		try {
			List<String> methods = Files.readAllLines(getWorkspace().getSrcPath().resolve("methods.config"));
			for (String s : methods) {
				s = s.trim();
				if (s != "") {
					System.out.println("Method String: '" + s + "'");
					String   qname = s.substring(0, s.indexOf('('));
					String[] parts = qname.split("\\.");
					String   method = parts[parts.length - 1];
					List<String> classes = new ArrayList<>(1); // TODO
					StringBuilder pkg = new StringBuilder();
					for (int i = 0; i < parts.length - 1; ++i) {
						if (Character.isUpperCase(parts[i].charAt(0))) {
							for (int j = i + 1; j < parts.length - 1; ++j) {
								classes.add(parts[j]);
							}
							break;
						}
						if (i > 0) {
							pkg.append(".");
						}
						pkg.append(parts[i]);
					}
					System.out.println("HOT METHOD");
					System.out.println("  q" + qname);
					System.out.println("  p" + pkg.toString());
					System.out.println("  c" + String.join(".", classes));
					System.out.println("  m" + method);

					this.methods.add(qname);
					this.includedFragments.add(pkg.toString());
					this.methodQNameAndPList.add(s);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (this.methods.size() == 0) {
			throw new RuntimeException("Perhaps you forgot to add a 'methods.txt' file in the assets/src folder?");
		}
	}

	protected Workspace getWorkspace() {
		return this.workspace;
	}

	/** Access workspace cache. */
	protected Cache getCache() {
		return getWorkspace().getCache();
	}

	public void setShuffleSeed(long shuffleSeed) {
		this.shuffleSeed = shuffleSeed;
	}

	public void setSelectSeed(long selectSeed) {
		this.selectSeed = selectSeed;
	}

	protected long getSelectSeed() {
		return this.selectSeed;
	}

	protected long getShuffleSeed() {
		return this.shuffleSeed;
	}

	public Supplier<Refactoring> getSupplier() {

		System.out.println("HotMethodRefactoringSupplier::getSupplier()");

//		TODO: Make sure cache and refactoring supplier does not sort opportunities; just pick an index from the file.
		
		Iterator<RefactoringDescriptor> iter = iterator();

		return new Supplier<Refactoring>() {

			@Override
			public Refactoring get() {
				
				System.out.println("Supplier::get()");

				RefactoringDescriptor opp = null;
				Refactoring           ref = null;

				while (ref == null && iter.hasNext()) {

					System.out.println("Trying to supply a refactoring...");

					if ((opp = iter.next()) != null) {
						ref = opp.getRefactoring();
					}
				}

				if (ref == null) {
					System.out.println("Supplier is empty. No more refactorings to supply.");
				}

				return ref;
			}
		};
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
					boolean result = includedFragments.contains(name);
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
			CompilationUnit cu = HotMethodRefactoringSupplier.getCompilationUnit(icu);
			cu.accept(new HotMethodVisitor(getCache(), icu, new Predicate<String>() {
				@Override
				public boolean test(String t) {
					return methodQNameAndPList.contains(t);// methods.contains(t);
				}
			}));
		});
	}

	@Override
	public Iterator<RefactoringDescriptor> iterator() {
		return getCache().makeSupplier((Cache cache) -> {

			// TODO: We generate refactorings of all types related to hot methods.
			//       Here we should therefore load all types of refactorings.
			//       Perhaps use a HistSupply where each bin is a type?, or
			//       simply add all to a single list (ListSupply)?, or create a specialized
			//       supply handling all types.
			
			/*
			final org.alfine.refactoring.suppliers.HistSupply supply =
				new org.alfine.refactoring.suppliers.HistSupply();

			cache
			.getCacheLines(new ExtractMethodDescriptor().getRefactoringID())
			.forEach(line -> supply.add(new ExtractMethodDescriptor(line)));

			Random shuffle  = new Random(getShuffleSeed());
			Random select = new Random(getSelectSeed());

			supply.shuffle(shuffle);

			return supply.iterator(select);
			*/
			return null;
		});
	}
}
