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

		// TODO:
		// Consider using a header with extract method size argument.
		// Cache-line format: `<header>|<args>`, where <header> may not contain '|'.
		// header = line.substring(0, line.indexOf('|'));
		// args = line.substring(line.indexOf('|') + 1);
		// line = args;
		//
		// But first check if we can add arguments to the map as usual without affecting
		// refactoring arguments, e.g., the number of statements that's extracted from
		// a method.

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
		return 0;
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

		// We do not need to store the id because we always
		// know which id all opportunities have in the cache
		// file we are loading from (as long as we do not mix
		// opportunities of different types). 
		//
		// sb.append("id=" + getRefactoringID());

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

	/** Create refactoring from descriptor. (Override this method
	 *  to create a refactoring without using the descriptor.)*/
	protected Refactoring createRefactoring() {

		JavaRefactoringDescriptor descriptor = getDescriptor();

		RefactoringStatus status = descriptor.validateDescriptor();

		for (RefactoringStatusEntry entry : status.getEntries()) {
			System.out.println("RefactoringStatusEntry from `validateDescriptor()':\n" + entry.toString());
		}

		if (status.hasFatalError()) {
			System.err.println("Invalid descriptor (FATAL).\n" + status);
			return null;
		}

		Refactoring refactoring = null;

		try {
			RefactoringContext ctx    = null;

			status = new RefactoringStatus();

			ctx = descriptor.createRefactoringContext(status);

			for (RefactoringStatusEntry entry : status.getEntries()) {
				System.out.println("RefactoringStatusEntry:\n" + entry.toString());
			}

			if (status.hasError()) {
				System.out.println("Status has errors. Refactoring can not be created.");
				return null;
			}

			refactoring = ctx.getRefactoring();

		} catch (CoreException e) {
			e.printStackTrace();
		}

		return refactoring;
	}

	public Refactoring getRefactoring() {
		return createRefactoring();
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
