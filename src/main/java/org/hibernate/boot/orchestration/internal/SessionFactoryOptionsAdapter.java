/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.orchestration.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

import org.hibernate.boot.settings.ResolvedSessionFactorySettings;
import org.hibernate.boot.spi.SessionFactoryOptions;

/**
 * Temporary adapter from the PoC's resolved factory settings to ORM's legacy
 * {@link SessionFactoryOptions} SPI.
 *
 * This adapter deliberately supports only the subset audited for the immediate
 * {@code SessionFactoryImpl} constructor path.  Unsupported method calls fail
 * loudly so each new runtime dependency can be audited before it becomes part of
 * {@link ResolvedSessionFactorySettings}.
 *
 * @author Steve Ebersole
 */
public final class SessionFactoryOptionsAdapter {
	private SessionFactoryOptionsAdapter() {
	}

	public static SessionFactoryOptions create(ResolvedSessionFactorySettings settings) {
		Objects.requireNonNull( settings );
		return (SessionFactoryOptions) Proxy.newProxyInstance(
				SessionFactoryOptions.class.getClassLoader(),
				new Class<?>[] { SessionFactoryOptions.class },
				new Handler( settings )
		);
	}

	private record Handler(ResolvedSessionFactorySettings settings) implements InvocationHandler {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
			return switch ( method.getName() ) {
				case "getUuid" -> settings.uuid();
				case "getServiceRegistry" -> settings.serviceRegistry();
				case "isJpaBootstrap" -> settings.jpaBootstrap();
				case "getSessionFactoryName" -> settings.sessionFactoryName();
				case "isSessionFactoryNameAlsoJndiName" -> settings.sessionFactoryNameAlsoJndiName();
				case "getStatementObserver" -> settings.statementObserver();
				case "getStatementInspector" -> settings.statementInspector();
				case "getInitialSessionCacheMode" -> settings.initialSessionCacheMode();
				case "getPhysicalConnectionHandlingMode" -> settings.physicalConnectionHandlingMode();
				case "getJdbcTimeZone" -> settings.jdbcTimeZone();
				case "isFlushBeforeCompletionEnabled" -> settings.flushBeforeCompletionEnabled();
				case "isAutoCloseSessionEnabled" -> settings.autoCloseSessionEnabled();
				case "isIdentifierRollbackEnabled" -> settings.identifierRollbackEnabled();
				case "getInterceptor" -> settings.interceptor();
				case "getSessionFactoryObservers" -> settings.sessionFactoryObservers();
				case "getValidatorFactoryReference" -> settings.validatorFactoryReference();
				case "isSecondLevelCacheEnabled" -> settings.secondLevelCacheEnabled();
				case "isQueryCacheEnabled" -> settings.queryCacheEnabled();
				case "getQueryCacheLayout" -> settings.queryCacheLayout();
				case "getTimestampsCacheFactory" -> settings.timestampsCacheFactory();
				case "getCacheRegionPrefix" -> settings.cacheRegionPrefix();
				case "isMinimalPutsEnabled" -> settings.minimalPutsEnabled();
					case "isStructuredCacheEntriesEnabled" -> settings.structuredCacheEntriesEnabled();
					case "isDirectReferenceCacheEntriesEnabled" -> settings.directReferenceCacheEntriesEnabled();
					case "isAutoEvictCollectionCache" -> settings.autoEvictCollectionCache();
					case "getCustomSqlFunctionMap" -> settings.customSqlFunctionMap();
					case "getCustomSqmFunctionRegistry" -> settings.customSqmFunctionRegistry();
					case "getCustomHqlTranslator" -> settings.customHqlTranslator();
					case "getCustomSqmTranslatorFactory" -> settings.customSqmTranslatorFactory();
					case "getCustomSqmMultiTableMutationStrategy" -> settings.customSqmMultiTableMutationStrategy();
					case "getCustomSqmMultiTableInsertStrategy" -> settings.customSqmMultiTableInsertStrategy();
					case "resolveCustomSqmMultiTableMutationStrategy" -> null;
					case "resolveCustomSqmMultiTableInsertStrategy" -> null;
					case "getJpaCompliance" -> settings.jpaCompliance();
					case "getCriteriaValueHandlingMode" -> settings.criteriaValueHandlingMode();
					case "getImmutableEntityUpdateQueryHandlingMode" -> settings.immutableEntityUpdateQueryHandlingMode();
					case "allowImmutableEntityUpdate" -> settings.immutableEntityUpdateQueryHandlingMode()
							!= org.hibernate.query.spi.ImmutableEntityUpdateQueryHandlingMode.EXCEPTION;
					case "isJsonFunctionsEnabled" -> settings.jsonFunctionsEnabled();
					case "isXmlFunctionsEnabled" -> settings.xmlFunctionsEnabled();
					case "isPortableIntegerDivisionEnabled" -> settings.portableIntegerDivisionEnabled();
					case "getNativeJdbcParametersIgnored" -> settings.nativeJdbcParametersIgnored();
					case "isCollectionsInDefaultFetchGroupEnabled" -> settings.collectionsInDefaultFetchGroupEnabled();
					case "areJPACallbacksEnabled" -> settings.jpaCallbacksEnabled();
					case "getDefaultBatchFetchSize" -> settings.defaultBatchFetchSize();
					case "getMaximumFetchDepth" -> settings.maximumFetchDepth();
					case "isSubselectFetchEnabled" -> settings.subselectFetchEnabled();
					case "isCommentsEnabled" -> settings.commentsEnabled();
					case "getTemporalTableStrategy" -> settings.temporalTableStrategy();
					case "getAuditStrategy" -> settings.auditStrategy();
					case "isMultiTenancyEnabled" -> settings.multiTenancyEnabled();
				case "getCurrentTenantIdentifierResolver" -> settings.currentTenantIdentifierResolver();
				case "getDefaultTenantIdentifierJavaType" -> settings.defaultTenantIdentifierJavaType();
				case "getDefaultCatalog" -> settings.defaultCatalog();
				case "getDefaultSchema" -> settings.defaultSchema();
				case "toString" -> toString();
				case "hashCode" -> System.identityHashCode( proxy );
				case "equals" -> proxy == args[0];
				default -> throw new UnsupportedOperationException(
						"SessionFactoryOptions method not resolved by the minimal adapter: "
								+ method.getName()
				);
			};
		}

		@Override
		public String toString() {
			return "SessionFactoryOptionsAdapter[" + settings.uuid() + "]";
		}
	}
}
