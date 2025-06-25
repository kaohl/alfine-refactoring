package org.alfine.refactoring.suppliers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MethodSet {
	private Set<String> fragments;
	private Set<String> classes;
	private Set<String> methods;

	public MethodSet(Path methodsFile) {
		this.fragments = new HashSet<>();
		this.classes   = new HashSet<>();
		this.methods   = new HashSet<>();
		try {
			List<String> methods = Files.readAllLines(methodsFile);
			for (String s : methods) {
				MethodSet.parseMethod(s, this.fragments, this.classes, this.methods);
			}
			System.out.println("--- HOT METHODS ---");
			System.out.println("Fragments:");
			for (String fragment : this.fragments) {
				System.out.println("FRG: " + fragment);
			}
			System.out.println("Classes:");
			for (String clazz : this.classes) {
				System.out.println("CLS: " + clazz);
			}
			System.out.println("Methods:");
			for (String method: this.methods) {
				System.out.println("MTH: " + method);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void parseMethod(String methodSignature, Set<String> fragments, Set<String> classes, Set<String> methods) {
		String s = methodSignature.trim();
		if (s == "") {
			return;
		}
		methods.add(s);

		String   qname = s.substring(0, s.indexOf('('));
		String[] parts = qname.split("\\.");

		StringBuilder pkg = new StringBuilder();
		for (int i = 0; i < parts.length - 1; ++i) {
			char x = parts[i].charAt(0);
			if (Character.isUpperCase(x)) {
				StringBuilder cls = new StringBuilder(pkg);
				if (i > 0) {
					cls.append(".");
				}
				cls.append(parts[i]);
				classes.add(cls.toString());
				for (int j = i + 1; j < parts.length - 1; ++j) {
					cls.append(".");
					cls.append(parts[j]);
					classes.add(cls.toString());
				}
				break;
			}
			if (i > 0) {
				pkg.append(".");
			}
			pkg.append(parts[i]);
		}
		fragments.add(pkg.toString());
	}

	public int size() {
		return this.methods.size();
	}

	public boolean hasFragment(String name) {
		return fragments.contains(name);
	}

	public boolean hasClass(String clazzQName) {
		return classes.contains(clazzQName);
	}

	public boolean hasMethod(String method) {
		return methods.contains(method);
	}
}
