/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "agg_id_entities")
public class AggregatedIdEntity {
	@EmbeddedId
	private Pk id;

	@Version
	private Integer version;
	@TenantId
	private String tenantId;

	@Embeddable
	public static class Pk {
		private Integer id1;
		private Integer id2;
	}
}
