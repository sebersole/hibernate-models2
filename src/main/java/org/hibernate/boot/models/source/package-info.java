/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */

/**
 * Source-facing entry points for collecting the model resources that categorization
 * should consider.
 * <p>
 * This package sits between ORM boot source declarations and model categorization.
 * It normalizes explicit persistence-unit declarations, programmatic persistence
 * configuration, package metadata, annotated classes, and XML mapping bindings into
 * {@link org.hibernate.boot.models.source.AvailableResources}.  The processor then
 * applies XML contributions, resolves the visible managed types, builds their
 * inheritance state, and produces a categorized domain model.
 *
 * @author Steve Ebersole
 */
package org.hibernate.boot.models.source;
