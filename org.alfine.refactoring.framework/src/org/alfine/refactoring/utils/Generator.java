package org.alfine.refactoring.utils;

import java.util.Random;

import org.eclipse.jdt.core.IJavaElement;

@SuppressWarnings("serial")
public final class Generator extends Random {
	
	private final int     index;
	private      int     maxLength;
	private      boolean isLengthFixed;

	public Generator() {
		this.index         = 0; // Don't drop any values.
		this.maxLength     = 1; // Whatever.
		this.isLengthFixed = false;

		setSeed(0);
	}

	public Generator(long seed, int index) {
		this.index         = index;
		this.maxLength     = 1;     // Whatever.
		this.isLengthFixed = false;

		setSeed(seed);
		drop(index);
	}

	public Generator(long seed, int index, int maxLength, boolean isLengthFixed) {
		this.index         = index;
		this.maxLength     = maxLength;
		this.isLengthFixed = isLengthFixed;

		setSeed(seed);
		drop(index);
	}

	public void drop(int i) {
		while (i-- > 0) {
			nextInt();
		}
	}

	public void setLengthFixed(boolean b) {
		this.isLengthFixed = b;
	}

	public boolean getIsLengthFixed() {
		return this.isLengthFixed;
	}
	
	public void setMaxLength(int n) {
		this.maxLength = n;
	}

	public int getMaxLength() {
		return this.maxLength;
	}
	
	public int getIndex() {
		return index;
	}

	/** Generate a new name compatible with the specified `org.eclipse.jdt.core.IJavaElement' type. */
	public String genName(int type) {
		switch (type) {

		case IJavaElement.COMPILATION_UNIT:
			return genTypeName();

		case IJavaElement.FIELD:
			return genFieldName();

		case IJavaElement.LOCAL_VARIABLE:
			return genLocalName();

		case IJavaElement.METHOD:
			return genMethodName();

		case IJavaElement.TYPE:
			return genTypeName();

		case IJavaElement.TYPE_PARAMETER:
			return genTypeName(); // Should this be all caps or something?

		case IJavaElement.PACKAGE_FRAGMENT:
			return genLocalName(); // Should this be all lowercase?

		case IJavaElement.PACKAGE_FRAGMENT_ROOT:
			return genLocalName(); // Should this be all lowercase?

		default: throw new RuntimeException("Unexpected IJavaElement type `" + type + "' to rename");
		}
	}
	
	private String genTypeName() {
		return genNameWithUpperCaseStart();	
	}

	private String genFieldName() {
		return genNameWithLowerCaseStart();
	}

	private String genLocalName() {
		return genNameWithLowerCaseStart();
	}

	private String genMethodName() {
		return genNameWithLowerCaseStart();
	}

	private String genNameWithUpperCaseStart() {
		String name = generateId();
	    return ("" + name.charAt(0)).toUpperCase() + name.substring(1);
	}

	private String genNameWithLowerCaseStart() {
		String name = generateId();
		return ("" + name.charAt(0)).toLowerCase() + name.substring(1);
	}

	private String generateId() {

		// TODO: Remove this method or use it from methods above.

		StringBuilder sb = new StringBuilder();

		// Randomize identifier length (length is always at least 1).

		if (getMaxLength() == 0) {
			throw new RuntimeException("Option 'length' must be set to a non-zero natural number.");
		}

		int length = getMaxLength();

		if (!getIsLengthFixed()) {
			length = (int)(nextDouble() * length) + 1;
		}

		sb.append(next(true));

		while (--length > 0) {
			sb.append(next(false));
		}

		return sb.toString();
	}

	private char next(boolean isFirst) {
		int c = 0;
		while (!isValid((c = (nextInt() % 128)), isFirst));
		return (char)c;
	}

	private static boolean isValid(int i, boolean isFirst) {
		return Character.isAlphabetic(i) && (isFirst ? Character.isJavaIdentifierStart(i) : Character.isJavaIdentifierPart(i));
	}
}