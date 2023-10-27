/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.TenantId;
import org.hibernate.models.ModelsException;
import org.hibernate.models.internal.CollectionHelper;
import org.hibernate.models.orm.spi.AttributeMetadata;
import org.hibernate.models.orm.spi.EntityHierarchy;
import org.hibernate.models.orm.spi.EntityTypeMetadata;
import org.hibernate.models.orm.spi.OrmModelBuildingContext;
import org.hibernate.models.orm.spi.CategorizedDomainModel;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.MemberDetails;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

/**
 * Processes attributes which are conceptually at the level of the hierarchy - id, version, tenant-id
 *
 * @author Steve Ebersole
 */
public class HierarchyAttributeProcessor {
	public static List<HierarchyAttributeDescriptor> preBindHierarchyAttributes(
			CategorizedDomainModel categorizedDomainModel,
			OrmModelBuildingContext modelBuildingContext) {
		final Set<EntityHierarchy> entityHierarchies = categorizedDomainModel.getEntityHierarchies();
		final List<HierarchyAttributeDescriptor> hierarchyIdMembers = CollectionHelper.arrayList( entityHierarchies.size() );

		for ( EntityHierarchy hierarchy : entityHierarchies ) {
			final HierarchyAttributeDescriptor hierarchyAttributeDescriptor = new HierarchyAttributeDescriptor( hierarchy );
			hierarchyIdMembers.add( hierarchyAttributeDescriptor );

			final EntityTypeMetadata hierarchyRoot = hierarchy.getRoot();
			for ( AttributeMetadata attr : hierarchyRoot.getAttributes() ) {
				final MemberDetails attrMember = attr.getMember();
				final AnnotationUsage<EmbeddedId> eIdAnn = attrMember.getAnnotationUsage( EmbeddedId.class );
				if ( eIdAnn != null ) {
					hierarchyAttributeDescriptor.collectIdAttribute( attr );
				}

				final AnnotationUsage<Id> idAnn = attrMember.getAnnotationUsage( Id.class );
				if ( idAnn != null ) {
					hierarchyAttributeDescriptor.collectIdAttribute( attr );
				}

				final AnnotationUsage<Version> versionAnn = attrMember.getAnnotationUsage( Version.class );
				if ( versionAnn != null ) {
					hierarchyAttributeDescriptor.collectVersionAttribute( attr );
				}

				final AnnotationUsage<TenantId> tenantIdAnn = attrMember.getAnnotationUsage( TenantId.class );
				if ( tenantIdAnn != null ) {
					hierarchyAttributeDescriptor.collectTenantIdAttribute( attr );
				}
			}
		}

		return hierarchyIdMembers;
	}

	public static class HierarchyAttributeDescriptor {
		private final EntityHierarchy entityHierarchy;

		private Object collectedIdAttributes;
		private AttributeMetadata versionAttribute;
		private AttributeMetadata tenantIdAttribute;

		// todo : row-id?
		// todo : others?

		public HierarchyAttributeDescriptor(EntityHierarchy entityHierarchy) {
			this.entityHierarchy = entityHierarchy;
		}

		public EntityHierarchy getEntityHierarchy() {
			return entityHierarchy;
		}

		public Object getCollectedIdAttributes() {
			return collectedIdAttributes;
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

		public AttributeMetadata getVersionAttribute() {
			return versionAttribute;
		}

		public void collectVersionAttribute(AttributeMetadata member) {
			if ( versionAttribute != null ) {
				throw new ModelsException( "Encountered multiple @Version attributes for hierarchy " + getEntityHierarchy().getRoot().getEntityName() );
			}
			versionAttribute = member;
		}

		public AttributeMetadata getTenantIdAttribute() {
			return tenantIdAttribute;
		}

		public void collectTenantIdAttribute(AttributeMetadata member) {
			if ( tenantIdAttribute != null ) {
				throw new ModelsException( "Encountered multiple @TenantId attributes for hierarchy " + getEntityHierarchy().getRoot().getEntityName() );
			}
			tenantIdAttribute = member;
		}
	}
}
