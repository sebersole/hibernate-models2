/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.internal;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.annotations.Synchronize;
import org.hibernate.boot.model.CustomSql;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.models.orm.JpaAnnotations;
import org.hibernate.models.orm.categorize.spi.AttributeMetadata;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
import org.hibernate.models.orm.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.orm.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.models.orm.categorize.spi.ModelCategorizationContext;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;

import jakarta.persistence.AccessType;
import jakarta.persistence.Cacheable;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import static org.hibernate.models.internal.StringHelper.EMPTY_STRINGS;
import static org.hibernate.models.internal.StringHelper.isNotEmpty;
import static org.hibernate.models.internal.StringHelper.unqualify;

/**
 * @author Steve Ebersole
 */
public class EntityTypeMetadataImpl
		extends AbstractIdentifiableTypeMetadata
		implements EntityTypeMetadata, EntityNaming {
	private final EntityHierarchy hierarchy;

	private final String entityName;
	private final String jpaEntityName;

	private final AbstractIdentifiableTypeMetadata superType;
	private final List<AttributeMetadata> attributeList;

	private final boolean mutable;
	private final boolean cacheable;
	private final boolean isLazy;
	private final String proxy;
	private final int batchSize;
	private final String discriminatorMatchValue;
	private final boolean isSelectBeforeUpdate;
	private final boolean isDynamicInsert;
	private final boolean isDynamicUpdate;
	private final CustomSql customInsert;
	private final CustomSql customUpdate;
	private final CustomSql customDelete;
	private final String[] synchronizedTableNames;

	/**
	 * This form is intended for construction of root Entity.
	 */
	public EntityTypeMetadataImpl(
			ClassDetails classDetails,
			EntityHierarchy hierarchy,
			AccessType defaultAccessType,
			RootEntityAndSuperTypeConsumer superTypeConsumer,
			Consumer<IdentifiableTypeMetadata> typeConsumer,
			ModelCategorizationContext modelContext) {
		super( classDetails, hierarchy, true, defaultAccessType, superTypeConsumer, typeConsumer, modelContext );

		this.hierarchy = hierarchy;

		// NOTE: there is no annotation for `entity-name`; it comes exclusively from
		// XML mappings.  by default, the `entityName` is simply the entity class name.
		// all of which `ClassDetails#getName` already handles for us
		this.entityName = getClassDetails().getName();

		final AnnotationUsage<Entity> entityAnnotation = classDetails.getAnnotationUsage( JpaAnnotations.ENTITY );
		this.jpaEntityName = determineJpaEntityName( entityAnnotation, entityName );

		this.attributeList = resolveAttributes();
		superTypeConsumer.acceptTypeOrSuperType( this );

		// walk up
		this.superType = walkRootSuperclasses( classDetails, getAccessType(), superTypeConsumer, typeConsumer );

		this.mutable = determineMutability( classDetails, modelContext );
		this.cacheable = determineCacheability( classDetails, modelContext );
		this.synchronizedTableNames = determineSynchronizedTableNames();
		this.batchSize = determineBatchSize();
		this.isSelectBeforeUpdate = decodeSelectBeforeUpdate();
		this.isDynamicInsert = decodeDynamicInsert();
		this.isDynamicUpdate = decodeDynamicUpdate();
		this.customInsert = extractCustomSql( classDetails.getAnnotationUsage( SQLInsert.class ) );
		this.customUpdate = extractCustomSql( classDetails.getAnnotationUsage( SQLUpdate.class ) );
		this.customDelete = extractCustomSql( classDetails.getAnnotationUsage( SQLDelete.class ) );

		//noinspection deprecation
		final AnnotationUsage<Proxy> proxyAnnotation = classDetails.getAnnotationUsage( Proxy.class );
		if ( proxyAnnotation != null ) {
			final Boolean lazyValue = proxyAnnotation.getAttributeValue( "lazy" );
			this.isLazy = lazyValue == null || lazyValue;

			if ( this.isLazy ) {
				final ClassDetails proxyClassDetails = proxyAnnotation.getAttributeValue( "proxyClass" );
				if ( proxyClassDetails != null ) {
					this.proxy = proxyClassDetails.getName();
				}
				else {
					this.proxy = null;
				}
			}
			else {
				this.proxy = null;
			}
		}
		else {
			// defaults are that it is lazy and that the class itself is the proxy class
			this.isLazy = true;
			this.proxy = getEntityName();
		}

		final AnnotationUsage<DiscriminatorValue> discriminatorValueAnn = classDetails.getAnnotationUsage( DiscriminatorValue.class );
		if ( discriminatorValueAnn != null ) {
			this.discriminatorMatchValue = discriminatorValueAnn.getAttributeValue( "value" );
		}
		else {
			this.discriminatorMatchValue = null;
		}
	}


	/**
	 * This form is intended for construction of non-root Entity.
	 */
	public EntityTypeMetadataImpl(
			ClassDetails classDetails,
			EntityHierarchy hierarchy,
			AbstractIdentifiableTypeMetadata superType,
			Consumer<IdentifiableTypeMetadata> typeConsumer,
			ModelCategorizationContext modelContext) {
		super( classDetails, hierarchy, superType, typeConsumer, modelContext );
		this.hierarchy = hierarchy;

		// NOTE: this is no annotation for `entity-name`.  it comes exclusively from
		// XML mappings.  by default, the `entityName` is simply the entity class name.
		// all of which `ClassDetails#getName` already handles for us
		this.entityName = getClassDetails().getName();

		final AnnotationUsage<Entity> entityAnnotation = classDetails.getAnnotationUsage( JpaAnnotations.ENTITY );
		this.jpaEntityName = determineJpaEntityName( entityAnnotation, entityName );

		this.superType = superType;
		this.attributeList = resolveAttributes();

		this.mutable = determineMutability( classDetails, modelContext );
		this.cacheable = determineCacheability( classDetails, modelContext );
		this.synchronizedTableNames = determineSynchronizedTableNames();
		this.batchSize = determineBatchSize();
		this.isSelectBeforeUpdate = decodeSelectBeforeUpdate();
		this.isDynamicInsert = decodeDynamicInsert();
		this.isDynamicUpdate = decodeDynamicUpdate();
		this.customInsert = extractCustomSql( classDetails.getAnnotationUsage( SQLInsert.class ) );
		this.customUpdate = extractCustomSql( classDetails.getAnnotationUsage( SQLUpdate.class ) );
		this.customDelete = extractCustomSql( classDetails.getAnnotationUsage( SQLDelete.class ) );

		//noinspection deprecation
		final AnnotationUsage<Proxy> proxyAnnotation = classDetails.getAnnotationUsage( Proxy.class );
		if ( proxyAnnotation != null ) {
			final Boolean lazyValue = proxyAnnotation.getAttributeValue( "lazy" );
			this.isLazy = lazyValue == null || lazyValue;

			if ( this.isLazy ) {
				final ClassDetails proxyClassDetails = proxyAnnotation.getAttributeValue( "proxyClass" );
				if ( proxyClassDetails != null ) {
					this.proxy = proxyClassDetails.getName();
				}
				else {
					this.proxy = null;
				}
			}
			else {
				this.proxy = null;
			}
		}
		else {
			// defaults are that it is lazy and that the class itself is the proxy class
			this.isLazy = true;
			this.proxy = getEntityName();
		}

		final AnnotationUsage<DiscriminatorValue> discriminatorValueAnn = classDetails.getAnnotationUsage( DiscriminatorValue.class );
		if ( discriminatorValueAnn != null ) {
			this.discriminatorMatchValue = discriminatorValueAnn.getAttributeValue( "value" );
		}
		else {
			this.discriminatorMatchValue = null;
		}
	}

	@Override
	public IdentifiableTypeMetadata getSuperType() {
		return superType;
	}

	@Override
	protected List<AttributeMetadata> attributeList() {
		return attributeList;
	}

	@Override
	public EntityHierarchy getHierarchy() {
		return hierarchy;
	}

	@Override
	public String getEntityName() {
		return entityName;
	}

	@Override
	public String getJpaEntityName() {
		return jpaEntityName;
	}

	@Override
	public String getClassName() {
		return getClassDetails().getClassName();
	}

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public boolean isCacheable() {
		return cacheable;
	}

	@Override
	public String[] getSynchronizedTableNames() {
		return synchronizedTableNames;
	}

	@Override
	public int getBatchSize() {
		return batchSize;
	}

	@Override
	public boolean isSelectBeforeUpdate() {
		return isSelectBeforeUpdate;
	}

	@Override
	public boolean isDynamicInsert() {
		return isDynamicInsert;
	}

	@Override
	public boolean isDynamicUpdate() {
		return isDynamicUpdate;
	}

	@Override
	public CustomSql getCustomInsert() {
		return customInsert;
	}

	@Override
	public CustomSql getCustomUpdate() {
		return customUpdate;
	}

	@Override
	public CustomSql getCustomDelete() {
		return customDelete;
	}

	public String getDiscriminatorMatchValue() {
		return discriminatorMatchValue;
	}

	public boolean isLazy() {
		return isLazy;
	}

	public String getProxy() {
		return proxy;
	}


	private String determineJpaEntityName(AnnotationUsage<Entity> entityAnnotation, String entityName) {
		final String name = entityAnnotation.getAttributeValue( "name" );
		if ( isNotEmpty( name ) ) {
			return name;
		}
		return unqualify( entityName );
	}

	private boolean determineMutability(ClassDetails classDetails, ModelCategorizationContext modelContext) {
		final AnnotationUsage<Immutable> immutableAnn = classDetails.getAnnotationUsage( Immutable.class );
		return immutableAnn == null;
	}

	private boolean determineCacheability(
			ClassDetails classDetails,
			ModelCategorizationContext modelContext) {
		final AnnotationUsage<Cacheable> cacheableAnn = classDetails.getAnnotationUsage( Cacheable.class );
		switch ( modelContext.getSharedCacheMode() ) {
			case NONE: {
				return false;
			}
			case ALL: {
				return true;
			}
			case DISABLE_SELECTIVE: {
				// Disable caching for all `@Cacheable(false)`, enabled otherwise (including no annotation)
				//noinspection RedundantIfStatement
				if ( cacheableAnn == null || cacheableAnn.getBoolean( "value" ) ) {
					// not disabled
					return true;
				}
				else {
					// disable, there was an explicit `@Cacheable(false)`
					return false;
				}
			}
			default: {
				// ENABLE_SELECTIVE
				// UNSPECIFIED

				// Enable caching for all `@Cacheable(true)`, disable otherwise (including no annotation)
				//noinspection RedundantIfStatement
				if ( cacheableAnn != null && cacheableAnn.getBoolean( "value" ) ) {
					// enable, there was an explicit `@Cacheable(true)`
					return true;
				}
				else {
					return false;
				}
			}
		}
	}

	/**
	 * Build a CustomSql reference from {@link org.hibernate.annotations.SQLInsert},
	 * {@link org.hibernate.annotations.SQLUpdate}, {@link org.hibernate.annotations.SQLDelete}
	 * or {@link org.hibernate.annotations.SQLDeleteAll} annotations
	 */
	public static CustomSql extractCustomSql(AnnotationUsage<?> customSqlAnnotation) {
		if ( customSqlAnnotation == null ) {
			return null;
		}

		final String sql = customSqlAnnotation.getAttributeValue( "sql" );
		final boolean isCallable = customSqlAnnotation.getAttributeValue( "value" );

		final ResultCheckStyle checkValue = customSqlAnnotation.getAttributeValue( "check" );
		final ExecuteUpdateResultCheckStyle checkStyle;
		if ( checkValue == null ) {
			checkStyle = isCallable
					? ExecuteUpdateResultCheckStyle.NONE
					: ExecuteUpdateResultCheckStyle.COUNT;
		}
		else {
			checkStyle = ExecuteUpdateResultCheckStyle.fromResultCheckStyle( checkValue );
		}

		return new CustomSql( sql, isCallable, checkStyle );
	}

	private String[] determineSynchronizedTableNames() {
		final AnnotationUsage<Synchronize> synchronizeAnnotation = getClassDetails().getAnnotationUsage( Synchronize.class );
		if ( synchronizeAnnotation != null ) {
			return synchronizeAnnotation.<String>getList( "value" ).toArray( new String[0] );
		}
		return EMPTY_STRINGS;
	}

	private int determineBatchSize() {
		final AnnotationUsage<BatchSize> batchSizeAnnotation = getClassDetails().getAnnotationUsage( BatchSize.class );
		if ( batchSizeAnnotation != null ) {
			return batchSizeAnnotation.getAttributeValue( "size" );
		}
		return -1;
	}

	private boolean decodeSelectBeforeUpdate() {
		//noinspection deprecation
		final AnnotationUsage<SelectBeforeUpdate> selectBeforeUpdateAnnotation = getClassDetails().getAnnotationUsage( SelectBeforeUpdate.class );
		if ( selectBeforeUpdateAnnotation == null ) {
			return false;
		}
		return selectBeforeUpdateAnnotation.getBoolean( "value" );
	}

	private boolean decodeDynamicInsert() {
		final AnnotationUsage<DynamicInsert> dynamicInsertAnnotation = getClassDetails().getAnnotationUsage( DynamicInsert.class );
		if ( dynamicInsertAnnotation == null ) {
			return false;
		}

		return dynamicInsertAnnotation.getBoolean( "value" );
	}

	private boolean decodeDynamicUpdate() {
		final AnnotationUsage<DynamicUpdate> dynamicUpdateAnnotation = getClassDetails().getAnnotationUsage( DynamicUpdate.class );
		if ( dynamicUpdateAnnotation == null ) {
			return false;
		}
		return dynamicUpdateAnnotation.getBoolean( "value" );
	}
}
