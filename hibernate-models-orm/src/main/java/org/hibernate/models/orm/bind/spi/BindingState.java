/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.spi;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.NamedConsumer;
import org.hibernate.models.orm.bind.internal.binders.TableBinder;
import org.hibernate.models.orm.bind.internal.binders.IdentifiableTypeBinder;
import org.hibernate.models.orm.bind.internal.binders.ManagedTypeBinder;
import org.hibernate.models.orm.categorize.spi.FilterDefRegistration;
import org.hibernate.models.orm.categorize.spi.ManagedTypeMetadata;
import org.hibernate.models.source.spi.ClassDetails;

/**
 * The idea here is mostly the role filled by InFlightMetadataCollector upstream.
 * We should incorporate any improvements here into there when we move this upstream.
 *
 * @author Steve Ebersole
 */
public interface BindingState {
	MetadataBuildingContext getMetadataBuildingContext();

	default Database getDatabase() {
		return getMetadataBuildingContext().getMetadataCollector().getDatabase();
	}

	void apply(FilterDefRegistration registration);

	int getTableCount();
	void forEachTable(NamedConsumer<TableReference> consumer);
	<T extends TableReference> T getTableByName(String name);
	void addTable(TableReference table);
	void registerTableSecondPass(TableBinder.TableSecondPass secondPass);


	void registerTypeBinder(ManagedTypeMetadata type, ManagedTypeBinder binder);

	default ManagedTypeBinder getTypeBinder(ManagedTypeMetadata type) {
		return getTypeBinder( type.getClassDetails() );
	}

	ManagedTypeBinder getTypeBinder(ClassDetails type);
	IdentifiableTypeBinder getSuperTypeBinder(ClassDetails type);

}
