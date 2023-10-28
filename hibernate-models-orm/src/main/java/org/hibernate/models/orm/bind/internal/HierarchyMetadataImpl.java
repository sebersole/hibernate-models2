/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.models.ModelsException;
import org.hibernate.models.orm.bind.spi.BindingContext;
import org.hibernate.models.orm.bind.spi.HierarchyMetadata;
import org.hibernate.models.orm.bind.spi.IdMapping;
import org.hibernate.models.orm.categorize.spi.AttributeMetadata;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
import org.hibernate.models.orm.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.source.spi.ClassDetails;

/**
 * @author Steve Ebersole
 */
public class HierarchyMetadataImpl implements HierarchyMetadata {
	private final EntityHierarchy entityHierarchy;

	private Object collectedIdAttributes;
	private IdMapping idMapping;

	private AttributeMetadata versionAttribute;
	private AttributeMetadata tenantIdAttribute;

	// todo : row-id?
	// todo : others?

	public HierarchyMetadataImpl(EntityHierarchy entityHierarchy) {
		this.entityHierarchy = entityHierarchy;

		final EntityTypeMetadata rootEntity = entityHierarchy.getRoot();
		final ClassDetails rootEntityClassDetails = rootEntity.getClassDetails();
	}

	@Override public EntityHierarchy getEntityHierarchy() {
		return entityHierarchy;
	}

	@Override public Object getCollectedIdAttributes() {
		return collectedIdAttributes;
	}

	@Override
	public IdMapping getIdMapping() {
		return idMapping;
	}

	public void collectIdAttribute(AttributeMetadata member) {
		assert member != null;

		if ( collectedIdAttributes == null ) {
			collectedIdAttributes = member;
		}
		else if ( collectedIdAttributes instanceof List ) {
			//noinspection unchecked,rawtypes
			final List<AttributeMetadata> membersList = (List) collectedIdAttributes;
			membersList.add( member );
		}
		else if ( collectedIdAttributes instanceof AttributeMetadata ) {
			final ArrayList<AttributeMetadata> combined = new ArrayList<>();
			combined.add( (AttributeMetadata) collectedIdAttributes );
			combined.add( member );
			collectedIdAttributes = combined;
		}
	}

	@Override public AttributeMetadata getVersionAttribute() {
		return versionAttribute;
	}

	public void collectVersionAttribute(AttributeMetadata member) {
		if ( versionAttribute != null ) {
			throw new ModelsException( "Encountered multiple @Version attributes for hierarchy " + getEntityHierarchy().getRoot()
					.getEntityName() );
		}
		versionAttribute = member;
	}

	@Override public AttributeMetadata getTenantIdAttribute() {
		return tenantIdAttribute;
	}

	public void collectTenantIdAttribute(AttributeMetadata member) {
		if ( tenantIdAttribute != null ) {
			throw new ModelsException( "Encountered multiple @TenantId attributes for hierarchy " + getEntityHierarchy().getRoot()
					.getEntityName() );
		}
		tenantIdAttribute = member;
	}

	public void resolve(BindingContext bindingContext) {
		this.idMapping = resolveIdMapping( bindingContext );

	}

	private IdMapping resolveIdMapping(BindingContext bindingContext) {
		if ( collectedIdAttributes instanceof List ) {
			// we know this is non-aggregated
			//noinspection unchecked
			final List<AttributeMetadata> idAttributes = (List<AttributeMetadata>) collectedIdAttributes;

			// find the id class
			final ClassDetails idClassType = findIdClass( entityHierarchy, bindingContext );

			return new NonAggregatedIdMappingImpl( idAttributes, idClassType );
		}

		final AttributeMetadata idAttribute = (AttributeMetadata) collectedIdAttributes;

		if ( idAttribute.getNature() == AttributeMetadata.AttributeNature.BASIC ) {
			return new BasicIdMapping( idAttribute );
		}

		if ( idAttribute.getNature() == AttributeMetadata.AttributeNature.EMBEDDED ) {
			return new AggregatedIdMappingImpl( idAttribute );
		}

		throw new ModelsException(
				String.format(
						Locale.ROOT,
						"Unexpected attribute nature [%s] - %s",
						idAttribute.getNature(),
						entityHierarchy.getRoot().getEntityName()
				)
		);
	}

	private ClassDetails findIdClass(EntityHierarchy entityHierarchy, BindingContext bindingContext) {
		// todo : do this.  for now, just return null
		return null;
	}
}
