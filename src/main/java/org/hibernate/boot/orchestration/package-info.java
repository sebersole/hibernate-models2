/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Coordinates the high-level flow from normalized bootstrap inputs toward
 * SessionFactory creation.
 * <p>
 * This package is intentionally about phase ordering, not about the mechanics of
 * any one phase.  Entry-point-specific code should adapt its inputs into request
 * objects.  The orchestration layer then delegates to focused components for
 * source collection, categorization, binding, option resolution, and eventual
 * factory construction.
 * <p>
 * The current PoC slice is deliberately narrow: it starts with neutral source
 * contributions, explicit configuration values, and a service registry.  The
 * orchestrator resolves settings, creates available resources, creates the
 * metadata-building context, and then produces ORM
 * {@link org.hibernate.boot.spi.MetadataImplementor} by running categorization and
 * binding in order.  Later slices should grow the request and orchestrator rather
 * than introducing separate helpers that also own bootstrap phase order.
 *
 * @author Steve Ebersole
 */
package org.hibernate.boot.orchestration;
