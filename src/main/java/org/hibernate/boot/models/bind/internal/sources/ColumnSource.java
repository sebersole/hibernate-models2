/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.sources;

import org.hibernate.internal.util.StringHelper;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.JoinColumn;

/**
 * Unified source for column-like mapping annotations.
 *
 * @author Steve Ebersole
 */
public interface ColumnSource {
	static ColumnSource from(jakarta.persistence.Column column) {
		return column == null ? null : new JpaColumnSource( column );
	}

	static ColumnSource from(JoinColumn joinColumn) {
		return joinColumn == null ? null : new JoinColumnSource( joinColumn );
	}

	static ColumnSource from(DiscriminatorColumn discriminatorColumn) {
		return discriminatorColumn == null ? null : new DiscriminatorColumnSource( discriminatorColumn );
	}

	String name();

	default String nonEmptyName() {
		return StringHelper.nullIfEmpty( name() );
	}

	boolean unique(boolean defaultValue);

	boolean nullable(boolean defaultValue);

	String columnDefinition();

	int length(int defaultValue);

	int precision(int defaultValue);

	int scale(int defaultValue);

	String options();

	String table();

	record JpaColumnSource(jakarta.persistence.Column column) implements ColumnSource {
		@Override
		public String name() {
			return column.name();
		}

		@Override
		public boolean unique(boolean defaultValue) {
			return column.unique();
		}

		@Override
		public boolean nullable(boolean defaultValue) {
			return column.nullable();
		}

		@Override
		public String columnDefinition() {
			return column.columnDefinition();
		}

		@Override
		public int length(int defaultValue) {
			return column.length();
		}

		@Override
		public int precision(int defaultValue) {
			return column.precision();
		}

		@Override
		public int scale(int defaultValue) {
			return column.scale();
		}

		@Override
		public String options() {
			return column.options();
		}

		@Override
		public String table() {
			return column.table();
		}
	}

	record JoinColumnSource(JoinColumn joinColumn) implements ColumnSource {
		@Override
		public String name() {
			return joinColumn.name();
		}

		@Override
		public boolean unique(boolean defaultValue) {
			return joinColumn.unique();
		}

		@Override
		public boolean nullable(boolean defaultValue) {
			return joinColumn.nullable();
		}

		@Override
		public String columnDefinition() {
			return joinColumn.columnDefinition();
		}

		@Override
		public int length(int defaultValue) {
			return defaultValue;
		}

		@Override
		public int precision(int defaultValue) {
			return defaultValue;
		}

		@Override
		public int scale(int defaultValue) {
			return defaultValue;
		}

		@Override
		public String options() {
			return joinColumn.options();
		}

		@Override
		public String table() {
			return joinColumn.table();
		}
	}

	record DiscriminatorColumnSource(DiscriminatorColumn discriminatorColumn) implements ColumnSource {
		@Override
		public String name() {
			return discriminatorColumn.name();
		}

		@Override
		public boolean unique(boolean defaultValue) {
			return defaultValue;
		}

		@Override
		public boolean nullable(boolean defaultValue) {
			return defaultValue;
		}

		@Override
		public String columnDefinition() {
			return discriminatorColumn.columnDefinition();
		}

		@Override
		public int length(int defaultValue) {
			return discriminatorColumn.length();
		}

		@Override
		public int precision(int defaultValue) {
			return defaultValue;
		}

		@Override
		public int scale(int defaultValue) {
			return defaultValue;
		}

		@Override
		public String options() {
			return discriminatorColumn.options();
		}

		@Override
		public String table() {
			return "";
		}
	}
}
