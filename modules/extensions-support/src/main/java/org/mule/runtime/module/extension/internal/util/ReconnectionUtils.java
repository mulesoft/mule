/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractCauseOfType;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractConnectionException;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.requiresConnectionProvisioning;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.COMPONENT_CONFIG_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DO_NOT_RETRY;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.IS_TRANSACTIONAL;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFilteredParameters;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ConnectableComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ConnectionInterceptor;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ExtensionConnectionSupplier;
import org.mule.runtime.module.extension.internal.runtime.execution.interceptor.InterceptorChain;
import org.mule.runtime.module.extension.internal.runtime.streaming.CursorResetInterceptor;
import org.mule.runtime.module.extension.internal.runtime.streaming.PagingProviderProducer;
import org.mule.runtime.module.extension.internal.runtime.transaction.ExtensionTransactionKey;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Utilities for handling reconnection on operations that use a connection.
 *
 * @since 4.2.3
 */
public class ReconnectionUtils {

  public static Consumer<Throwable> NULL_THROWABLE_CONSUMER = e -> {
  };

  /**
   * @param t       the {@link Throwable} thrown during the execution of the operation
   * @param context the {@link ExecutionContextAdapter} that contains the context information about the operation's execution
   * @return whether or not the operation should be retried
   *
   * @since 4.2.3 - 4.3.0
   */
  public static boolean shouldRetry(Throwable t, ExecutionContextAdapter<?> context) {
    Optional<String> contextConfigName = context.getConfiguration().map(ConfigurationInstance::getName);
    Optional<ConnectionException> connectionException = extractConnectionException(t);
    if (!connectionException.isPresent() || Boolean.valueOf(context.getVariable(DO_NOT_RETRY))) {
      return false;
    }

    // Transactions are bound to a connection, so the tx cannot continue on a newly established connection.
    // Because of this, operations within transactions cannot be retried.
    if (isPartOfActiveTransaction(context.getConfiguration().get())
        || extractCauseOfType(t, TransactionException.class).isPresent()) {
      return false;
    }

    return isConnectionExceptionFromCurrentComponent(connectionException.get(), contextConfigName.orElse(null));
  }

  /**
   * To fix reconnection for paged operations that fail after the first page, the connection exception is intercepted at the
   * {@link PagingProviderProducer} and enriched with additional information. This method reads that information and determines if
   * the operation should be retried.
   *
   * This method first checks if the operation was involved in a transaction. If so, it returns false. Then it checks that the
   * context trying to retry this operation has the same config as the operation itself. This is to prevent other components from
   * retrying the operation. If the config names do no match, it returns false. Otherwise or if the connection exception was not
   * enriched, this method returns true.
   *
   * @param connectionException the {@link ConnectionException} thrown during the execution of the operation
   * @param contextConfigName   the config name for the context that is attempting to retry the operation
   * @return whether or not the operation should be retried
   */
  private static boolean isConnectionExceptionFromCurrentComponent(ConnectionException connectionException,
                                                                   String contextConfigName) {
    Boolean isTransactional = (Boolean) connectionException.getInfo().get(IS_TRANSACTIONAL);
    if (isTransactional != null && isTransactional) {
      return false;
    }
    Object operationConfigName = connectionException.getInfo().get(COMPONENT_CONFIG_NAME);
    if (operationConfigName != null && contextConfigName != null) {
      return contextConfigName.equals(operationConfigName);
    }
    return true;
  }

  /**
   * @param configurationInstance the {@link ConfigurationInstance} to check.
   * @return whether or not it is part of an active transaction.
   *
   * @since 4.2.3 - 4.3.0
   */
  public static boolean isPartOfActiveTransaction(ConfigurationInstance configurationInstance) {
    if (isTransactionActive()) {
      Transaction tx = TransactionCoordination.getInstance().getTransaction();
      return tx != null && tx.hasResource(new ExtensionTransactionKey(configurationInstance));
    }
    return false;
  }

  /**
   * Creates an {@link InterceptorChain} that enables reconnection for connected components
   *
   * @param extensionModel     the {@link ExtensionModel}
   * @param componentModel     the {@link ComponentModel}
   * @param connectionSupplier the connection supplier
   * @param reflectionCache    a {@link ReflectionCache}
   * @return a new {@link InterceptorChain}
   * @since 4.5.0
   */
  public static InterceptorChain createReconnectionInterceptorsChain(ExtensionModel extensionModel,
                                                                     ComponentModel componentModel,
                                                                     ExtensionConnectionSupplier connectionSupplier,
                                                                     ReflectionCache reflectionCache) {
    InterceptorChain.Builder chainBuilder = InterceptorChain.builder();

    if (requiresConnectionInterceptors(componentModel)) {
      addConnectionInterceptors(chainBuilder, extensionModel, componentModel, connectionSupplier, reflectionCache);
    }

    return chainBuilder.build();
  }

  private static boolean requiresConnectionInterceptors(ComponentModel componentModel) {
    if (componentModel instanceof ConnectableComponentModel) {
      return requiresConnectionProvisioning((ConnectableComponentModel) componentModel);
    }

    return false;
  }

  private static void addConnectionInterceptors(InterceptorChain.Builder chainBuilder,
                                                ExtensionModel extensionModel,
                                                ComponentModel componentModel,
                                                ExtensionConnectionSupplier connectionSupplier,
                                                ReflectionCache reflectionCache) {
    chainBuilder.addInterceptor(new ConnectionInterceptor(connectionSupplier));
    addCursorResetInterceptor(chainBuilder, extensionModel, componentModel, reflectionCache);
  }

  private static void addCursorResetInterceptor(InterceptorChain.Builder chainBuilder,
                                                ExtensionModel extensionModel,
                                                ComponentModel componentModel,
                                                ReflectionCache reflectionCache) {
    Map<ParameterGroupModel, Set<ParameterModel>> streamParameters =
        getFilteredParameters(componentModel, getStreamParameterFilter(extensionModel));
    if (!streamParameters.isEmpty()) {
      chainBuilder.addInterceptor(new CursorResetInterceptor(streamParameters, reflectionCache));
    }
  }

  private static Predicate<ParameterModel> getStreamParameterFilter(ExtensionModel extensionModel) {
    ClassLoader extensionClassLoader = getClassLoader(extensionModel);
    return p -> getType(p.getType(), extensionClassLoader)
        .filter(clazz -> InputStream.class.isAssignableFrom(clazz) || Iterator.class.isAssignableFrom(clazz))
        .isPresent();
  }
}
