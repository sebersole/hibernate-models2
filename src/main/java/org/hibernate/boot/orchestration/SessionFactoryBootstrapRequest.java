/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.orchestration;

import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.spi.MetadataBuildingContext;

/// Request for the current SessionFactory bootstrap slice.
///
/// This request is intentionally small.  It represents the point in the PoC
/// where source collection has already produced [AvailableResources] and the
/// caller has already created the [MetadataBuildingContext] to receive bound ORM
/// mapping state.
///
/// Later bootstrap slices are expected to add earlier inputs, such as resolved
/// settings and entry-point-specific source contributions, before adding final
/// SessionFactory creation inputs.
///
/// @author Steve Ebersole
public record SessionFactoryBootstrapRequest(
		/// Normalized source resources to categorize and bind.
		AvailableResources availableResources,

		/// Metadata-building context that owns the target metadata collector.
		MetadataBuildingContext metadataBuildingContext) {
}
