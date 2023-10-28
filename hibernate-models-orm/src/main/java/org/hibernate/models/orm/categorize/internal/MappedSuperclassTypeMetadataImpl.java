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

	private final List<AttributeMetadata> attributeList;

	public MappedSuperclassTypeMetadataImpl(
			ClassDetails classDetails,
			EntityHierarchy hierarchy,
			AccessType defaultAccessType,
			Consumer<IdentifiableTypeMetadata> typeConsumer,
			ModelCategorizationContext modelContext) {
		super( classDetails, hierarchy, false, defaultAccessType, typeConsumer, modelContext );

		this.attributeList = resolveAttributes();
	}

	public MappedSuperclassTypeMetadataImpl(
			ClassDetails classDetails,
			EntityHierarchy hierarchy,
			AbstractIdentifiableTypeMetadata superType,
			Consumer<IdentifiableTypeMetadata> typeConsumer,
			ModelCategorizationContext modelContext) {
		super( classDetails, hierarchy, superType, typeConsumer, modelContext );

		this.attributeList = resolveAttributes();
	}

	@Override
	protected List<AttributeMetadata> attributeList() {
		return attributeList;
	}
}
