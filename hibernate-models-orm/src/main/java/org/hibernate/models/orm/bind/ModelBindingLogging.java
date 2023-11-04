/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind;

import org.hibernate.models.Internal;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.INFO;

/**
 * todo : find the proper min/max id range
 *
 * @author Steve Ebersole
 */
@Internal
public interface ModelBindingLogging extends BasicLogger {
	String NAME = "org.hibernate.models.orm";

	Logger MODEL_BINDING_LOGGER = Logger.getLogger( NAME );
}
