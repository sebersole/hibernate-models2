/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.event.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hibernate.jpa.event.spi.Callback;
import org.hibernate.jpa.event.spi.CallbackDefinition;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

/**
 * @author Steve Ebersole
 */
public class ListenerCallback extends AbstractCallback {

	public static class Definition implements CallbackDefinition {
		private final Class<?> listenerClass;
		private final Method callbackMethod;
		private final CallbackType callbackType;

		public Definition(Class<?> listenerClass, Method callbackMethod, CallbackType callbackType) {
			this.listenerClass = listenerClass;
			this.callbackMethod = callbackMethod;
			this.callbackType = callbackType;
		}

		@Override
		public Callback createCallback(ManagedBeanRegistry beanRegistry) {
			return new ListenerCallback( beanRegistry.getBean( listenerClass ), callbackMethod, callbackType );
		}
	}

	private final Method callbackMethod;
	private final ManagedBean<?> listenerManagedBean;

	ListenerCallback(ManagedBean<?> listenerManagedBean, Method callbackMethod, CallbackType callbackType) {
		super( callbackType );
		this.listenerManagedBean = listenerManagedBean;
		this.callbackMethod = callbackMethod;
	}

	@Override
	public boolean performCallback(Object entity) {
		try {
			callbackMethod.invoke( listenerManagedBean.getBeanInstance(), entity );
			return true;
		}
		catch (InvocationTargetException e) {
			//keep runtime exceptions as is
			if ( e.getTargetException() instanceof RuntimeException ) {
				throw (RuntimeException) e.getTargetException();
			}
			else {
				throw new RuntimeException( e.getTargetException() );
			}
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}
}
