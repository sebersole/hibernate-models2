/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.orchestration;

import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.spi.MetadataImplementor;

/// Resolved ORM boot metadata and associated boot-model products.
/// This product keeps the ORM metadata handoff explicit while preserving the
/// intermediate boot-model products needed by later SessionFactory bootstrap
/// experiments.
///
/// @author Steve Ebersole
public record ResolvedMetadata(
		MetadataImplementor metadata,
		CategorizedDomainModel categorizedDomainModel,
		BindingState bindingState,
		ResolvedBootstrapSettings bootstrapSettings) {
}
