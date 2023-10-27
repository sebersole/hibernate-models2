/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.models.ModelsException;
import org.hibernate.models.source.UnknownClassException;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsBuilder;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.source.spi.PackageDetails;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractClassDetailsRegistry implements ClassDetailsRegistry {
	protected final Map<String, ClassDetails> classDetailsMap;

	// subtype per type
	protected final Map<String, List<ClassDetails>> subTypeClassDetailsMap;

	// for packages containing a package-info.class file
	protected final Map<String, PackageDetails> packageDetailsMap;


	protected AbstractClassDetailsRegistry() {
		this( new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>() );
	}

	protected AbstractClassDetailsRegistry(
			Map<String, ClassDetails> classDetailsMap,
			Map<String, List<ClassDetails>> subTypeClassDetailsMap,
			Map<String, PackageDetails> packageDetailsMap) {
		this.classDetailsMap = classDetailsMap;
		this.subTypeClassDetailsMap = subTypeClassDetailsMap;
		this.packageDetailsMap = packageDetailsMap;
	}

	@Override
	public List<ClassDetails> getDirectSubTypes(String superTypeName) {
		return subTypeClassDetailsMap.get( superTypeName );
	}

	@Override
	public void forEachDirectSubType(String superTypeName, ClassDetailsConsumer consumer) {
		final List<ClassDetails> directSubTypes = getDirectSubTypes( superTypeName );
		if ( directSubTypes == null ) {
			return;
		}
		for ( int i = 0; i < directSubTypes.size(); i++ ) {
			consumer.consume( directSubTypes.get( i ) );
		}
	}

	@Override
	public ClassDetails findClassDetails(String name) {
		return classDetailsMap.get( name );
	}

	@Override
	public ClassDetails getClassDetails(String name) {
		final ClassDetails named = classDetailsMap.get( name );
		if ( named == null ) {
			if ( "void".equals( name ) ) {
				return null;
			}
			throw new UnknownClassException( "Unknown managed class - " + name );
		}
		return named;
	}

	@Override
	public void forEachClassDetails(ClassDetailsConsumer consumer) {
		for ( Map.Entry<String, ClassDetails> entry : classDetailsMap.entrySet() ) {
			consumer.consume( entry.getValue() );
		}
	}

	@Override
	public ClassDetails resolveClassDetails(String name, ClassDetailsBuilder creator) {
		assert name != null;

		if ( "void".equals( name ) ) {
			return null;
		}

		if ( name.endsWith( "package-info" ) ) {
			throw new ModelsException( "Resolve " + name + " as a package, not a class" );
		}

		final ClassDetails existing = classDetailsMap.get( name );
		if ( existing != null ) {
			return existing;
		}

		return createClassDetails( name, creator );
	}

	protected abstract ClassDetails createClassDetails(String name, ClassDetailsBuilder creator);

	@Override public ClassDetails resolveClassDetails(
			String name,
			ClassDetailsCreator creator) {
		assert name != null;

		if ( name.endsWith( "package-info" ) ) {
			throw new ModelsException( "Resolve " + name + " as a package, not a class" );
		}

		final ClassDetails existing = classDetailsMap.get( name );
		if ( existing != null ) {
			return existing;
		}

		return createClassDetails( name, creator );
	}

	protected abstract ClassDetails createClassDetails(String name, ClassDetailsCreator creator);

	@Override
	public PackageDetails findPackageDetails(String name) {
		return packageDetailsMap.get( name );
	}

	@Override
	public PackageDetails getPackageDetails(String name) {
		final PackageDetails found = findPackageDetails( name );
		if ( found == null ) {
			throw new ModelsException( "Could not locate PackageDetails - " + name );
		}
		return found;
	}

	@Override
	public void forEachPackageDetails(PackageDetailsConsumer consumer) {
		for ( Map.Entry<String, PackageDetails> entry : packageDetailsMap.entrySet() ) {
			consumer.consume( entry.getValue() );
		}
	}

	@Override
	public PackageDetails resolvePackageDetails(String packageName, PackageDetailsCreator creator) {
		final PackageDetails existing = packageDetailsMap.get( packageName );
		if ( existing != null ) {
			return existing;
		}

		return createPackageDetails( packageName, creator );
	}

	protected abstract PackageDetails createPackageDetails(String packageName, PackageDetailsCreator creator);
}
