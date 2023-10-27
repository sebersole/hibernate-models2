/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

/**
 * JAXB binding interface for commonality between entity and mapped-superclass mappings
 *
 * @author Steve Ebersole
 */
public interface JaxbEntityOrMappedSuperclass extends JaxbManagedType, JaxbLifecycleCallbackContainer {
	JaxbIdClassImpl getIdClass();

	void setIdClass(JaxbIdClassImpl value);

	JaxbEmptyTypeImpl getExcludeDefaultListeners();

	void setExcludeDefaultListeners(JaxbEmptyTypeImpl value);

	JaxbEmptyTypeImpl getExcludeSuperclassListeners();

	void setExcludeSuperclassListeners(JaxbEmptyTypeImpl value);

	JaxbEntityListenersImpl getEntityListeners();

	void setEntityListeners(JaxbEntityListenersImpl value);
}
