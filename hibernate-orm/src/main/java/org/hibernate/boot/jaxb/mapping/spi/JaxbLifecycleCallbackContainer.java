/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

/**
 * JAXB binding interface for commonality between things which
 * allow callback declarations.  This includes <ul>
 *     <li>
 *         entities and mapped-superclasses
 *     </li>
 *     <li>
 *         entity-listener classes
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface JaxbLifecycleCallbackContainer {
	JaxbPrePersistImpl getPrePersist();
	void setPrePersist(JaxbPrePersistImpl value);

	JaxbPostPersistImpl getPostPersist();
	void setPostPersist(JaxbPostPersistImpl value);

	JaxbPreRemoveImpl getPreRemove();
	void setPreRemove(JaxbPreRemoveImpl value);

	JaxbPostRemoveImpl getPostRemove();
	void setPostRemove(JaxbPostRemoveImpl value);

	JaxbPreUpdateImpl getPreUpdate();
	void setPreUpdate(JaxbPreUpdateImpl value);

	JaxbPostUpdateImpl getPostUpdate();
	void setPostUpdate(JaxbPostUpdateImpl value);

	JaxbPostLoadImpl getPostLoad();
	void setPostLoad(JaxbPostLoadImpl value);
}
