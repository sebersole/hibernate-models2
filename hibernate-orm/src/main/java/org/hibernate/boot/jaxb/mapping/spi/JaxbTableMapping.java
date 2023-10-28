/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

/**
 * @author Steve Ebersole
 */
public interface JaxbTableMapping extends JaxbSchemaAware {
	JaxbCheckConstraint getCheck();
	String getComment();
	String getOptions();

	// todo : see what JPA 3.2 decides about some of these XSD decisions
	//		- https://github.com/jakartaee/persistence/pull/541

	String getCommentAttribute();
	String getOptionsAttribute();
}
