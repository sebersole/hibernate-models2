/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import java.util.Locale;

import org.hibernate.tuple.GenerationTiming;

/**
 * JAXB marshalling for {@link GenerationTiming}
 *
 * @author Steve Ebersole
 */
public class GenerationTimingMarshalling {
	public static GenerationTiming fromXml(String name) {
		return name == null ? null : GenerationTiming.parseFromName( name );
	}

	public static String toXml(GenerationTiming generationTiming) {
		return ( null == generationTiming ) ?
				null :
				generationTiming.name().toLowerCase( Locale.ENGLISH );
	}
}
