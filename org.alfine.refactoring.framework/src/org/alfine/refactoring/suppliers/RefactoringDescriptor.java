package org.alfine.refactoring.suppliers;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContext;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RefactoringDescriptor implements Comparable<RefactoringDescriptor> {

	/** Sorted Map to get a predictable order of entries which we
	 *  use to produce a comparable string for refactoring descriptors. */
	private Map<String, String> args = new TreeMap<>();

	/** Predefined built-in descriptor argument key which should be defined on
	 *  descriptors that we want to distribute into bins in a histogram supply
	 *  (see `HistSupply`). An example is the ExtracMethod abstraction interval.
	 *  Whether we need it or not depends on how we want to bias our experiment. */
	public static final String KEY_HIST_BIN =
		"bin";

	public RefactoringDescriptor() {
	}

	/** Create a new descriptor with empty argument map. */
	public RefactoringDescriptor(Map<String, String> args) {
		for (Entry<String,String> kv : args.entrySet()) {
			this.args.put(kv.getKey(), kv.getValue());
		}
	}

	/** Populate map from line of key-value pairs. */
	public RefactoringDescriptor(String line) {

		String key = null;
		String val = null;

		for (String kv : line.split(" ")) {

			// Ignore leading space in cache line.

			if ("".equals(kv)) {
				continue;
			}

			int i = kv.indexOf('=');

			if (i != -1) {
				if (key != null) {
					this.args.put(key, val);
					key = val = null;
				}

				key = kv.substring(0, i).trim();
				val = kv.substring(i + 1).trim();
			} else {
				if (val == null) {
					val = "";
				}
				val += " " + kv; 
			}
		}

		if (key != null) {
			this.args.put(key, val);
		}
	}

	/** Return refactoring descriptor ID. */
	public abstract String getRefactoringID();

	/** The bin into which the descriptor is
	 *  added when a HistSupply is used. */
	public int histBin() {
		return
			this.args.containsKey(KEY_HIST_BIN)
			? Integer.parseInt(get(KEY_HIST_BIN))
			: 0;
	}

	/** Return present set of keys. */
	public Set<String> keySet() {
		return this.args.keySet();
	}

	/** Return refactoring argument associated with `key`. */
	public String get(String key) {
		return this.args.get(key);
	}

	/** Put refactoring argument into argument map. */
	public void put(String key, String value) {
		this.args.put(key, value);
	}

	/** This method should only be used to construct a JavaRefactoringDescriptor. */
	protected Map<String, String> getArgumentMap() {
		return this.args;
	}

	/** Create and return cache file entry which is a concatenation of argument map entries. */
	public String getCacheLine() {

		// TODO: Consider caching the cache line internally
		//       if this method gets called a lot (update if
		//       `put()` has been called since last call).

		StringBuilder sb = new StringBuilder();

		// We do not need to store the id because all opportunities
		// in the same file have the same id which we can hardcode in
		// the cache loading routine for in a given supplier.

		for (Entry<String, String> entry: this.args.entrySet()) {
			sb.append(" " + entry.getKey() + "=" + entry.getValue());
		}

		return sb.toString();
	}

	protected RefactoringContribution getRefactoringContribution() {

		String                  id           = null;
		RefactoringContribution contribution = null;

		id = getRefactoringID();

		if ((contribution = RefactoringCore.getRefactoringContribution(id)) == null) {
			Logger logger = LoggerFactory.getLogger(RefactoringDescriptor.class);
			logger.debug("No refactoring contribution for id = `{}`.");
			throw new RuntimeException("No refactoring contribution for id = `" + id + "`.");
		} else {
			return contribution;
		}
	}

	/** Configure this descriptor just before creating a
	 *  corresponding `JavaRefactoringDescriptor`. */
	protected abstract void configure();
	
	/** Configure corresponding `JavaRefactoringDescriptor`. */
	protected abstract void
	configureJavaRefactoringDescriptor(JavaRefactoringDescriptor descriptor);

	/** Return a JavaRefactoringDescriptor. */
	public JavaRefactoringDescriptor getDescriptor() {

		Logger logger = LoggerFactory.getLogger(RefactoringDescriptor.class);
		logger.debug("Descriptor arguments:");

		for (Entry<String, String> entry : getArgumentMap().entrySet()) {
			logger.debug("\t `{}` = `{}`", entry.getKey(), entry.getValue());
		}

		configure();

		RefactoringContribution   contribution = getRefactoringContribution();
		JavaRefactoringDescriptor defaultInit  = (JavaRefactoringDescriptor)contribution.createDescriptor();
		JavaRefactoringDescriptor descriptor   = (JavaRefactoringDescriptor)contribution.createDescriptor(
			getRefactoringID(),
			defaultInit.getProject(),
			defaultInit.getDescription(),
			defaultInit.getComment(),
			getArgumentMap(),
			defaultInit.getFlags()
		);

		configureJavaRefactoringDescriptor(descriptor);

		return descriptor;
	}
	
//	private Refactoring createRefactoring() {
//		
//		JavaRefactoringDescriptor descriptor = getDescriptor();
//
//		RefactoringStatus status = descriptor.validateDescriptor();
//
//		setValidationStatus(status);
//		
//// TODO: Move print into refactoring class to be processed before the refactoring is applied.
////
////		for (RefactoringStatusEntry entry : status.getEntries()) {
////			System.out.println("RefactoringStatusEntry from `validateDescriptor()':\n" + entry.toString());
////		}
//
//		if (status.hasError()) {
//			System.err.println("Invalid refactoring descriptor.");
//			return wrapper;
//		}
//
//		Refactoring refactoring = null;
//
//		try {
//			RefactoringContext ctx    = null;
//
//			status = new RefactoringStatus();
//
//			ctx = descriptor.createRefactoringContext(status);
//
//			setContextStatus(status);
//
////    TODO: Move to be processed before the refactoring is applied.
////			for (RefactoringStatusEntry entry : status.getEntries()) {
////				System.out.println("RefactoringStatusEntry:\n" + entry.toString());
////			}
//
//			if (status.hasError()) {
//				System.out.println("Invalid refactoring context.");
//				return wrapper;
//			}
//
//			refactoring = ctx.getRefactoring();
//
//		} catch (CoreException e) {
//			e.printStackTrace();
//		}
//
//		wrapper.setRefactoring(refactoring);
//
//		return wrapper;
//	}

	public org.alfine.refactoring.suppliers.Refactoring getRefactoring() {
		return new org.alfine.refactoring.suppliers.Refactoring(this);
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof RefactoringDescriptor)
				&& getCacheLine().equals(((RefactoringDescriptor)other).getCacheLine());
	}

	@Override
	public int compareTo(RefactoringDescriptor other) {
		return getCacheLine().compareTo(other.getCacheLine());
	}
}
