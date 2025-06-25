package org.alfine.refactoring.processors;

import java.util.Map;

public class SimpleDescriptor {
	private final String              id;
	private final Map<String, String> args;

	public SimpleDescriptor(String id, Map<String, String> args) {
		this.id   = id;
		this.args = args;
	}

	public String getID() {
		return this.id;
	}

	public Map<String, String> getArguments() {
		return this.args;
	}
}
