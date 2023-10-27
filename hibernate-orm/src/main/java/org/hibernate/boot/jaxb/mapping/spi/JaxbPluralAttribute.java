/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

import java.util.List;

import org.hibernate.boot.internal.LimitedCollectionClassification;

import jakarta.persistence.EnumType;
import jakarta.persistence.TemporalType;

/**
 * JAXB binding interface for plural attributes
 *
 * @author Brett Meyer
 */
public interface JaxbPluralAttribute extends JaxbPersistentAttribute {
	JaxbPluralFetchModeImpl getFetchMode();
	void setFetchMode(JaxbPluralFetchModeImpl mode);

	JaxbCollectionIdImpl getCollectionId();
	void setCollectionId(JaxbCollectionIdImpl id);


	LimitedCollectionClassification getClassification();
	void setClassification(LimitedCollectionClassification value);

	String getOrderBy();
	void setOrderBy(String value);

	JaxbOrderColumnImpl getOrderColumn();
	void setOrderColumn(JaxbOrderColumnImpl value);

	String getSort();
	void setSort(String value);

	JaxbMapKeyImpl getMapKey();
	void setMapKey(JaxbMapKeyImpl value);

	JaxbMapKeyClassImpl getMapKeyClass();
	void setMapKeyClass(JaxbMapKeyClassImpl value);

	TemporalType getMapKeyTemporal();

	void setMapKeyTemporal(TemporalType value);

	EnumType getMapKeyEnumerated();

	void setMapKeyEnumerated(EnumType value);

	List<JaxbAttributeOverrideImpl> getMapKeyAttributeOverride();

	List<JaxbConvertImpl> getMapKeyConvert();

	JaxbMapKeyColumnImpl getMapKeyColumn();

	void setMapKeyColumn(JaxbMapKeyColumnImpl value);

	List<JaxbMapKeyJoinColumnImpl> getMapKeyJoinColumn();

	JaxbForeignKeyImpl getMapKeyForeignKey();

	void setMapKeyForeignKey(JaxbForeignKeyImpl value);

	List<JaxbHbmFilterImpl> getFilters();
}
