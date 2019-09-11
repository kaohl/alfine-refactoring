package org.alfine.utils;

public class Pair<U, V> {
	private final U first;
	private final V second;
	
	public Pair(U u, V v) {
		this.first   = u;
		this.second = v;
	}

	public U getFirst() {
		return this.first;
	}

	public V getSecond() {
		return this.second;
	}
}