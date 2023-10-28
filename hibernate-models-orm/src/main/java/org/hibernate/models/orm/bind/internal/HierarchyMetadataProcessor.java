/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal;

import java.util.List;
import java.util.Set;

import org.hibernate.annotations.TenantId;
import org.hibernate.models.internal.CollectionHelper;
import org.hibernate.models.orm.bind.spi.BindingContext;
import org.hibernate.models.orm.bind.spi.HierarchyMetadata;
import org.hibernate.models.orm.categorize.spi.AttributeMetadata;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
import org.hibernate.models.orm.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.orm.categorize.spi.CategorizedDomainModel;
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
public class HierarchyMetadataProcessor {

	public static List<HierarchyMetadata> preBindHierarchyAttributes(
			CategorizedDomainModel categorizedDomainModel,
			BindingContext bindingContext) {
		final Set<EntityHierarchy> entityHierarchies = categorizedDomainModel.getEntityHierarchies();
		final List<HierarchyMetadata> hierarchyIdMembers = CollectionHelper.arrayList( entityHierarchies.size() );

		for ( EntityHierarchy hierarchy : entityHierarchies ) {
			final HierarchyMetadataImpl hierarchyMetadata = new HierarchyMetadataImpl( hierarchy );
			hierarchyIdMembers.add( hierarchyMetadata );

			// todo : need to look up the hierarchy from root for these if not found local
			// 		- especially, consider a way to walk up only once

			final EntityTypeMetadata hierarchyRoot = hierarchy.getRoot();
			for ( AttributeMetadata attr : hierarchyRoot.getAttributes() ) {
				final MemberDetails attrMember = attr.getMember();
				final AnnotationUsage<EmbeddedId> eIdAnn = attrMember.getAnnotationUsage( EmbeddedId.class );
				if ( eIdAnn != null ) {
					hierarchyMetadata.collectIdAttribute( attr );
				}

				final AnnotationUsage<Id> idAnn = attrMember.getAnnotationUsage( Id.class );
				if ( idAnn != null ) {
					hierarchyMetadata.collectIdAttribute( attr );
				}

				final AnnotationUsage<Version> versionAnn = attrMember.getAnnotationUsage( Version.class );
				if ( versionAnn != null ) {
					hierarchyMetadata.collectVersionAttribute( attr );
				}

				final AnnotationUsage<TenantId> tenantIdAnn = attrMember.getAnnotationUsage( TenantId.class );
				if ( tenantIdAnn != null ) {
					hierarchyMetadata.collectTenantIdAttribute( attr );
				}
			}

			hierarchyMetadata.resolve( bindingContext );
		}

		return hierarchyIdMembers;
	}

}
