/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.TenantId;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "non_agg_id_entities")
@IdClass( NonAggregatedIdEntity.Pk.class )
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class NonAggregatedIdEntity {
	@Id private Integer id1;
	@Id private Integer id2;
	@Version private Integer version;
	@TenantId private String tenantId;

	public static class Pk {
		private Integer id1;
		private Integer id2;
	}
}
