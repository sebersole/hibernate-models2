/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface JaxbTableMapping extends JaxbSchemaAware {
	List<JaxbCheckConstraintImpl> getCheck();
	String getComment();
	String getOptions();

	// todo : see what JPA 3.2 decides about some of these XSD decisions
	//		- https://github.com/jakartaee/persistence/pull/541

	String getCommentAttribute();
	String getOptionsAttribute();
}
