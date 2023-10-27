/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

/**
 * JAXB binding interface for natural-id definitions
 *
 * @author Steve Ebersole
 */
public interface JaxbNaturalId extends JaxbBaseAttributesContainer{
	/**
	 * The cache config associated with this natural-id
	 */
	JaxbCachingImpl getCaching();

	boolean isMutable();
	void setMutable(Boolean mutable);
}
