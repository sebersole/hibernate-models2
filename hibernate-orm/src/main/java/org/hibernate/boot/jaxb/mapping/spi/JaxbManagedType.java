/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

import jakarta.persistence.AccessType;

/**
 * Common interface for JAXB bindings representing entities, mapped-superclasses and embeddables (JPA collective
 * calls these "managed types" in terms of its Metamodel api).
 *
 * @author Strong Liu
 * @author Steve Ebersole
 */
public interface JaxbManagedType {
	String getClazz();
	void setClazz(String className);

	Boolean isMetadataComplete();
	void setMetadataComplete(Boolean isMetadataComplete);

	AccessType getAccess();
	void setAccess(AccessType value);

	String getDescription();
	void setDescription(String value);

	JaxbAttributesContainer getAttributes();
}
