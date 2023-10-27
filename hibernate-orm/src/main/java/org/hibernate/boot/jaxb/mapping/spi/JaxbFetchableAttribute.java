/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

import jakarta.persistence.FetchType;

/**
 * JAXB binding interface for EAGER/LAZY
 *
 * @apiNote All standard attributes are fetchable (basics allow FetchType as well); this
 * contract distinguishes ANY mappings which are always eager and so do not allow
 * specifying FetchType.
 *
 * @author Brett Meyer
 * @author Steve Ebersole
 */
public interface JaxbFetchableAttribute extends JaxbPersistentAttribute {
	FetchType getFetch();
	void setFetch(FetchType value);
}
