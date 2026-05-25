/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.orchestration;

import org.hibernate.boot.models.bind.internal.BindingContextImpl;
import org.hibernate.boot.models.bind.internal.BindingOptionsImpl;
import org.hibernate.boot.models.bind.internal.BindingStateImpl;
import org.hibernate.boot.models.bind.spi.BindingCoordinator;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.DomainModelCategorizer;
import org.hibernate.boot.spi.MetadataImplementor;

/// Orchestrates the boot-model path toward SessionFactory creation.
///
/// This class owns the ordering between bootstrap phases.  The current PoC slice
/// supports the first bridge needed by SessionFactory bootstrapping: turning
/// available model resources into ORM's boot-time [MetadataImplementor].
///
/// As the prototype grows, this class should remain the place where adjacent
/// phases are composed.  The individual phase components should continue to own
/// their mechanics.
///
/// @author Steve Ebersole
public class SessionFactoryBootstrap {
	/// Build ORM boot metadata from the current narrow bootstrap request.
	///
	/// The request is expected to already contain normalized source resources and
	/// the metadata-building context they should be bound into.
	///
	/// @param request The current bootstrap request
	///
	/// @return The metadata collector after categorization, binding, ordering, and
	/// validation have run
	public MetadataImplementor buildMetadata(SessionFactoryBootstrapRequest request) {
		final CategorizedDomainModel categorizedDomainModel = DomainModelCategorizer.categorize(
				request.availableResources(),
				request.metadataBuildingContext()
		);

		BindingCoordinator.coordinateBinding(
				categorizedDomainModel,
				new BindingStateImpl( request.metadataBuildingContext() ),
				new BindingOptionsImpl( request.metadataBuildingContext() ),
				new BindingContextImpl(
						categorizedDomainModel,
						request.metadataBuildingContext().getBootstrapContext()
				)
		);

		final MetadataImplementor metadata = request.metadataBuildingContext().getMetadataCollector();
		metadata.orderColumns( false );
		metadata.validate();
		return metadata;
	}
}
