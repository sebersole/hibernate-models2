/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.EnumSet;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.models.spi.MemberDetails;

/// Normalizes JPA and Hibernate cascade annotations for mapping properties.
///
/// Cascade metadata is declared on the source member, but the boot mapping model
/// stores the effective cascade style on [org.hibernate.mapping.Property].  This
/// helper mirrors the upstream aggregation rules closely enough for the phased
/// binder prototype: start with the JPA cascade array, add Hibernate's
/// `@Cascade`, apply orphan-removal when the caller has such a source, and then
/// include mapping defaults.
///
/// Keeping this as a small binder helper avoids making individual attribute
/// binders know how to merge the different cascade vocabularies.
///
/// @author Steve Ebersole
@SuppressWarnings("removal")
public class CascadeBinder {
	public static EnumSet<CascadeType> aggregateCascadeTypes(
			jakarta.persistence.CascadeType[] cascadeTypes,
			MemberDetails member,
			boolean orphanRemoval,
			BindingState bindingState) {
		final EnumSet<CascadeType> cascades = convertToHibernateCascadeTypes( cascadeTypes );

		final Cascade hibernateCascade = member.getDirectAnnotationUsage( Cascade.class );
		if ( hibernateCascade != null ) {
			for ( CascadeType cascadeType : hibernateCascade.value() ) {
				cascades.add( cascadeType );
			}
		}

		if ( orphanRemoval ) {
			cascades.add( CascadeType.DELETE_ORPHAN );
			cascades.add( CascadeType.REMOVE );
		}

		cascades.addAll( bindingState.getMetadataBuildingContext().getEffectiveDefaults().getDefaultCascadeTypes() );
		return cascades;
	}

	public static boolean hasOrphanDelete(EnumSet<CascadeType> cascadeTypes) {
		return cascadeTypes.contains( CascadeType.DELETE_ORPHAN );
	}

	private static EnumSet<CascadeType> convertToHibernateCascadeTypes(jakarta.persistence.CascadeType[] cascadeTypes) {
		final EnumSet<CascadeType> cascades = EnumSet.noneOf( CascadeType.class );
		if ( cascadeTypes == null ) {
			return cascades;
		}

		for ( jakarta.persistence.CascadeType cascadeType : cascadeTypes ) {
			cascades.add( convertToHibernateCascadeType( cascadeType ) );
		}
		return cascades;
	}

	private static CascadeType convertToHibernateCascadeType(jakarta.persistence.CascadeType cascadeType) {
		return switch ( cascadeType ) {
			case ALL -> CascadeType.ALL;
			case PERSIST -> CascadeType.PERSIST;
			case MERGE -> CascadeType.MERGE;
			case REMOVE -> CascadeType.REMOVE;
			case REFRESH -> CascadeType.REFRESH;
			case DETACH -> CascadeType.DETACH;
		};
	}
}
