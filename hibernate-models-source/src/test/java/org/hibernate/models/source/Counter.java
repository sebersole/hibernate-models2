/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source;

/**
 * @author Steve Ebersole
 */
public class Counter {
	private int count;

	public Counter() {
		this( 0 );
	}

	public Counter(int count) {
		this.count = count;
	}

	public void set(int count) {
		this.count = count;
	}

	public int get() {
		return count;
	}

	public int getAndIncrement() {
		return count++;
	}

	public int incrementAndGet() {
		return ++count;
	}

	public int getAndDecrement() {
		return count--;
	}

	public int decrementAndGet() {
		return --count;
	}
}
