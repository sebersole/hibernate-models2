/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

/**
 * A simple entity for initial work on the binding
 *
 * @author Steve Ebersole
 */
@Entity
@Table(name = "simpletons", comment = "Stupid is as stupid does")
@SecondaryTable(name = "simple_stuff", schema = "my_schema", catalog = "my_catalog", comment = "Don't sweat it")
@FilterDef( name = "by-name", defaultCondition = "name = :name", parameters = @ParamDef( name = "name", type = String.class ) )
public class SimpleEntity {
	@Id
	private Integer id;
	@Basic
	private String name;
	@Basic
	@Column(table = "simple_stuff", name = "datum")
	private String data;

	protected SimpleEntity() {
		// for Hibernate use
	}

	public SimpleEntity(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}
