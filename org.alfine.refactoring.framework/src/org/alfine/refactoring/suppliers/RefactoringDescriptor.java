package org.alfine.refactoring.suppliers;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

public abstract class RefactoringDescriptor implements Comparable<RefactoringDescriptor> {

	public static final String ID_NAME = "id";

	/** Sorted Map to get a predictable order of entries which we use
	 *  to produce a comparable string for refactoring descriptors. */
	private Map<String, String> args = new TreeMap<>();
	private Map<String, String> meta = new TreeMap<>();

	public RefactoringDescriptor() {
		this.meta.put(ID_NAME, getRefactoringID());
	}

	/** Create a new descriptor with empty argument map. */
	public RefactoringDescriptor(Map<String, String> args) {
		this();
		this.args.putAll(args);
	}

	public RefactoringDescriptor(Map<String, String> args, Map<String, String> meta) {
		this(args);
		this.meta.putAll(meta);
	}

	/** Return refactoring descriptor ID. */
	public abstract String getRefactoringID();

	/** Return present set of keys. */
	public Set<String> keySet() {
		return this.args.keySet();
	}

	/** Return refactoring argument associated with `key`. */
	public String getArg(String key) {
		return this.args.get(key);
	}

	/** Put refactoring argument into argument map. */
	public void putArg(String key, String value) {
		this.args.put(key, value);
	}

	/** This method should only be used to construct a JavaRefactoringDescriptor. */
	protected Map<String, String> getArgumentMap() {
		return this.args;
	}

	@Override
	public String toString() {
		JsonObjectBuilder meta = Json.createObjectBuilder();
		for (Entry<String, String> entry: this.meta.entrySet()) {
			meta.add(entry.getKey(), entry.getValue());
		}
		JsonObjectBuilder args = Json.createObjectBuilder();
		for (Entry<String, String> entry: this.args.entrySet()) {
			args.add(entry.getKey(), entry.getValue());
		}
		JsonObjectBuilder obj = Json.createObjectBuilder();
		obj.add("args", args.build());
		obj.add("meta", meta.build());
		return obj.build().toString();
	}

	/** Return a string representation of this descriptor. */
	public String getCacheLine() {
		return toString();
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
		System.out.println("Descriptor arguments:");
		for (Entry<String, String> entry : getArgumentMap().entrySet()) {
			System.out.println(String.format("  %s = %s", entry.getKey(), entry.getValue()));
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
