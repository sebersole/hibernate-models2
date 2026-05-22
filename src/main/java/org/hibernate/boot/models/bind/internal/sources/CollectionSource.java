/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.sources;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Bag;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderColumn;

/// Source-model facts for an [org.hibernate.mapping.Collection].
///
/// Like [BasicValueSource], this is a local prototype of information that may
/// eventually belong directly on collection mapping objects.  A plural member carries
/// several independent source concepts: the collection classification, the value type,
/// the optional map-key type, the collection-table declaration, and index/key annotations.
/// Keeping those facts together avoids re-deriving them from the Java collection class in
/// each binder step.
///
/// Historically, collection mapping had to support boot sources that did not have a
/// Hibernate Models member behind them.  In that world, the mapping object mostly held
/// the already-derived physical details: role name, collection table, key value, element
/// value, index value, and so on.  If upstream no longer needs to preserve legacy
/// `hbm.xml` as an equivalent source, collection mappings can retain their source model
/// identity and defer more interpretation until the mapping model has enough surrounding
/// context.
///
/// The key distinction is that the plural member is not the same thing as any one value
/// inside the collection.  Given `Map<K,V>`, the Java member contributes at least three
/// mapping concepts:
///
/// - the collection container, represented by [org.hibernate.mapping.Map]
/// - the collection element, represented by the mapping for `V`
/// - the collection index/key, represented by the mapping for `K`
///
/// Likewise, a `List<E>` contributes both the element value for `E` and a synthetic list
/// index value.  This record keeps those source-model facts together so downstream
/// binders do not need to rediscover that `member.getElementType()` is the element source
/// while `member.getMapKeyType()` is the map-key source.
///
/// The [#kind()] is deliberately source-oriented.  It says what semantic collection
/// classification the source member requested: set, bag, list, or map.  That is related
/// to, but not identical with, the concrete `org.hibernate.mapping.Collection` subclass.
/// For example, a Java `List` without `@Bag` is modeled as an indexed list, while a
/// Java `List` with Hibernate `@Bag` is modeled as a bag.  Keeping that decision on the
/// source object makes the classification explicit and testable.
///
/// In an upstream mapping-model version, a collection mapping might directly retain some
/// equivalent of:
///
/// - the plural source [MemberDetails]
/// - the effective element [TypeDetails]
/// - the effective map-key [TypeDetails], when applicable
/// - the source collection classification
/// - the collection table source annotation or implicit naming source
/// - the list-index or map-key column source annotation
///
/// That would make later naming, type resolution, and annotation interpretation depend
/// on source-model facts rather than repeated binder-local helper methods.
///
/// @author Steve Ebersole
public record CollectionSource(
		/// The semantic collection classification requested by the source member.
		///
		/// This is the source-level decision that eventually drives which
		/// [org.hibernate.mapping.Collection] subclass is created.  Keeping it here makes
		/// rules such as "`List` means indexed list unless `@Bag` is present" explicit.
		Kind kind,

		/// The plural Hibernate Models member that contributed this collection mapping.
		///
		/// The member remains important even after the collection object exists because
		/// element annotations, collection table annotations, list-index annotations, and
		/// map-key annotations are all declared on this same Java member.
		MemberDetails member,

		/// The effective source type for the collection element.
		///
		/// For `Collection<E>`, `List<E>`, and `Set<E>`, this is `E`.  For `Map<K,V>`,
		/// this is `V`.  It is intentionally not the collection container type itself.
		TypeDetails elementType,

		/// The effective source type for the map key, or `null` for non-map collections.
		///
		/// This exists because map keys have their own basic-value binding concerns:
		/// `@MapKeyEnumerated`, `@MapKeyTemporal`, `@MapKeyColumn`, Hibernate
		/// `@MapKeyJavaType`, and similar annotations all describe this type, not the
		/// collection element type.
		TypeDetails mapKeyType,

		/// The `@CollectionTable` annotation declared on the plural member.
		///
		/// This is currently required to have an explicit name in the prototype binder.
		/// Longer term, the source should also be able to represent an implicit
		/// collection-table naming request with enough context for the naming strategy.
		CollectionTable collectionTable) {
	/// Source-level collection classification.
	public enum Kind {
		/// A set-valued collection.
		SET,

		/// A bag-valued collection, including explicit Hibernate `@Bag`.
		BAG,

		/// An indexed list-valued collection.
		LIST,

		/// A map-valued collection with a distinct key/index value.
		MAP
	}

	/// Creates a collection source for an element collection member.
	///
	/// This method centralizes the collection-classification rules used by the binder.
	/// That is exactly the sort of logic that becomes easier to reason about if the
	/// upstream mapping model stores source facts directly instead of asking each binder
	/// to inspect Java collection classes and annotations independently.
	public static CollectionSource elementCollection(MemberDetails member) {
		final Class<?> collectionType = member.getType().determineRawClass().toJavaClass();
		final Kind kind;
		if ( java.util.Set.class.isAssignableFrom( collectionType ) ) {
			kind = Kind.SET;
		}
		else if ( java.util.List.class.isAssignableFrom( collectionType )
				&& !member.hasDirectAnnotationUsage( Bag.class ) ) {
			kind = Kind.LIST;
		}
		else if ( java.util.Map.class.isAssignableFrom( collectionType ) ) {
			kind = Kind.MAP;
		}
		else {
			kind = Kind.BAG;
		}

		return new CollectionSource(
				kind,
				member,
				member.getElementType(),
				kind == Kind.MAP ? member.getMapKeyType() : null,
				member.getDirectAnnotationUsage( CollectionTable.class )
		);
	}

	/// Whether the collection element value should be modeled as a component.
	///
	/// The source member can express embeddable-element intent in two ways: through the
	/// element type itself being annotated `@Embeddable`, or through `@Embedded` on the
	/// collection member.
	public boolean hasEmbeddableElement() {
		return elementType.determineRawClass().hasDirectAnnotationUsage( Embeddable.class )
				|| member.hasDirectAnnotationUsage( Embedded.class );
	}

	/// The explicit list-index column source, if one was declared.
	///
	/// A missing annotation still represents a meaningful source request for lists:
	/// use the implicit/default index column.
	public OrderColumn orderColumn() {
		return member.getDirectAnnotationUsage( OrderColumn.class );
	}

	/// The explicit map-key column source, if one was declared.
	///
	/// A missing annotation still represents a meaningful source request for maps:
	/// use the implicit/default map-key column.
	public MapKeyColumn mapKeyColumn() {
		return member.getDirectAnnotationUsage( MapKeyColumn.class );
	}

	/// The collection-table join columns as a list.
	///
	/// The conversion from annotation array to list is intentionally kept near the source
	/// object because these columns are source-level instructions for how the collection
	/// table joins back to its owner.
	public List<JoinColumn> joinColumns() {
		if ( collectionTable == null ) {
			return List.of();
		}
		if ( collectionTable.joinColumns().length == 0 ) {
			return List.of();
		}
		final ArrayList<JoinColumn> result = new ArrayList<>( collectionTable.joinColumns().length );
		for ( JoinColumn joinColumn : collectionTable.joinColumns() ) {
			result.add( joinColumn );
		}
		return result;
	}
}
