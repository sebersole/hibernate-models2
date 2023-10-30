/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.internal;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.models.orm.categorize.spi.AttributeMetadata;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
import org.hibernate.models.orm.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.models.orm.categorize.spi.MappedSuperclassTypeMetadata;
import org.hibernate.models.orm.categorize.spi.ModelCategorizationContext;
import org.hibernate.models.source.spi.ClassDetails;

import jakarta.persistence.AccessType;

/**
 * @author Steve Ebersole
 */
public class MappedSuperclassTypeMetadataImpl
		extends AbstractIdentifiableTypeMetadata
		implements MappedSuperclassTypeMetadata {

	private final AbstractIdentifiableTypeMetadata superType;
	private final List<AttributeMetadata> attributeList;

	public MappedSuperclassTypeMetadataImpl(
			ClassDetails classDetails,
			EntityHierarchy hierarchy,
			AccessType defaultAccessType,
			RootEntityAndSuperTypeConsumer superTypeConsumer,
			Consumer<IdentifiableTypeMetadata> typeConsumer,
			ModelCategorizationContext modelContext) {
		super( classDetails, hierarchy, false, defaultAccessType, superTypeConsumer, typeConsumer, modelContext );

		this.attributeList = resolveAttributes();
		superTypeConsumer.acceptTypeOrSuperType( this );

		// walk up
		this.superType = walkRootSuperclasses( classDetails, getAccessType(), superTypeConsumer, typeConsumer );
	}

	public MappedSuperclassTypeMetadataImpl(
			ClassDetails classDetails,
			EntityHierarchy hierarchy,
			AbstractIdentifiableTypeMetadata superType,
			Consumer<IdentifiableTypeMetadata> typeConsumer,
			ModelCategorizationContext modelContext) {
		super( classDetails, hierarchy, superType, typeConsumer, modelContext );

		this.superType = superType;
		this.attributeList = resolveAttributes();
	}

	@Override
	public IdentifiableTypeMetadata getSuperType() {
		return superType;
	}

	@Override
	protected List<AttributeMetadata> attributeList() {
		return attributeList;
	}
}
