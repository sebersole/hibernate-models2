/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

/**
 * @author Marco Belladelli
 */
public interface JaxbJoinColumn extends JaxbColumn {
	JaxbForeignKeyImpl getForeignKey();

	String getReferencedColumnName();
}
