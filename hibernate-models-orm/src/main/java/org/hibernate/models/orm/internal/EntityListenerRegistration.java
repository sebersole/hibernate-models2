/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.internal;

import org.hibernate.boot.jaxb.mapping.JaxbEntityListener;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.models.ModelsException;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.source.spi.MethodDetails;

/**
 * Represents an entity listener defined in XML.
 *
 * @apiNote Simply using {@code AnnotationUsage<EntityListeners>} is not enough here
 *
 * @see jakarta.persistence.EntityListeners
 *
 * @author Steve Ebersole
 */
public class EntityListenerRegistration {

	private final ClassDetails listenerClass;

	private final MethodDetails prePersistMethod;
	private final MethodDetails postPersistMethod;

	private final MethodDetails preRemoveMethod;
	private final MethodDetails postRemoveMethod;

	private final MethodDetails preUpdateMethod;
	private final MethodDetails postUpdateMethod;

	private final MethodDetails postLoadMethod;

	public EntityListenerRegistration(
			ClassDetails listenerClass,
			MethodDetails prePersistMethod,
			MethodDetails postPersistMethod,
			MethodDetails preRemoveMethod,
			MethodDetails postRemoveMethod,
			MethodDetails preUpdateMethod,
			MethodDetails postUpdateMethod,
			MethodDetails postLoadMethod) {
		this.listenerClass = listenerClass;
		this.prePersistMethod = prePersistMethod;
		this.postPersistMethod = postPersistMethod;
		this.preRemoveMethod = preRemoveMethod;
		this.postRemoveMethod = postRemoveMethod;
		this.preUpdateMethod = preUpdateMethod;
		this.postUpdateMethod = postUpdateMethod;
		this.postLoadMethod = postLoadMethod;

		if ( prePersistMethod == null
				&& postPersistMethod == null
				&& preRemoveMethod == null
				&& postRemoveMethod == null
				&& preUpdateMethod == null
				&& postUpdateMethod == null
				&& postLoadMethod == null ) {
			throw new ModelsException( "Mapping for entity-listener specified no callback methods - " + listenerClass.getClassName() );
		}
	}

	public ClassDetails getListenerClass() {
		return listenerClass;
	}

	public MethodDetails getPrePersistMethod() {
		return prePersistMethod;
	}

	public MethodDetails getPostPersistMethod() {
		return postPersistMethod;
	}

	public MethodDetails getPreRemoveMethod() {
		return preRemoveMethod;
	}

	public MethodDetails getPostRemoveMethod() {
		return postRemoveMethod;
	}

	public MethodDetails getPreUpdateMethod() {
		return preUpdateMethod;
	}

	public MethodDetails getPostUpdateMethod() {
		return postUpdateMethod;
	}

	public MethodDetails getPostLoadMethod() {
		return postLoadMethod;
	}
	public static EntityListenerRegistration from(JaxbEntityListener jaxbMapping, ClassDetailsRegistry classDetailsRegistry) {
		final ClassDetails listenerClassDetails = classDetailsRegistry.resolveClassDetails( jaxbMapping.getClazz() );
		final MutableObject<MethodDetails> prePersistMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postPersistMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preRemoveMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postRemoveMethod = new MutableObject<>();
		final MutableObject<MethodDetails> preUpdateMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postUpdateMethod = new MutableObject<>();
		final MutableObject<MethodDetails> postLoadMethod = new MutableObject<>();

		listenerClassDetails.forEachMethod( (index, methodDetails) -> {
			// todo : make this sensitive to method arguments once we have MethodDetails tracking arguments
			//		for now, just match name
			if ( jaxbMapping.getPrePersist() != null
					&& methodDetails.getName().equals( jaxbMapping.getPrePersist().getMethodName() ) ) {
				prePersistMethod.set( methodDetails );
			}
			else if ( jaxbMapping.getPostPersist().getMethodName() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostPersist().getMethodName() ) ) {
				postPersistMethod.set( methodDetails );
			}
			else if ( jaxbMapping.getPreRemove() != null
					&& methodDetails.getName().equals( jaxbMapping.getPreRemove().getMethodName() ) ) {
				preRemoveMethod.set( methodDetails );
			}
			else if ( jaxbMapping.getPostRemove() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostRemove().getMethodName() ) ) {
				postRemoveMethod.set( methodDetails );
			}
			else if ( jaxbMapping.getPreUpdate() != null
					&& methodDetails.getName().equals( jaxbMapping.getPreUpdate().getMethodName() ) ) {
				preUpdateMethod.set( methodDetails );
			}
			else if ( jaxbMapping.getPostUpdate() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostUpdate().getMethodName() ) ) {
				postUpdateMethod.set( methodDetails );
			}
			else if ( jaxbMapping.getPostLoad() != null
					&& methodDetails.getName().equals( jaxbMapping.getPostLoad().getMethodName() ) ) {
				postLoadMethod.set( methodDetails );
			}
		} );

		return new EntityListenerRegistration(
				listenerClassDetails,
				prePersistMethod.get(),
				postPersistMethod.get(),
				preRemoveMethod.get(),
				postRemoveMethod.get(),
				preUpdateMethod.get(),
				postUpdateMethod.get(),
				postLoadMethod.get()
		);
	}
}
