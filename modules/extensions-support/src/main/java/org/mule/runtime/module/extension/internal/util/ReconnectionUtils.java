/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractConnectionException;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.COMPONENT_CONFIG_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DO_NOT_RETRY;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.IS_TRANSACTIONAL;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.streaming.PagingProviderProducer;
import org.mule.runtime.module.extension.internal.runtime.transaction.ExtensionTransactionKey;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Utilities for handling reconnection on operations that use a connection.
 *
 * @since 4.2.3
 */
public class ReconnectionUtils {

  public static Consumer<Throwable> NULL_THROWABLE_CONSUMER = e -> {
  };

  /**
   * @param t the {@link Throwable} thrown during the execution of the operation
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

    if (isPartOfActiveTransaction(context.getConfiguration().get())) {
      return false;
    }

    return isConnectionExceptionFromCurrentComponent(connectionException.get(), contextConfigName.orElse(null));
  }

  /**
   * To fix reconnection for paged operations that fail after the first page, the connection exception is intercepted at
   * the {@link PagingProviderProducer} and enriched with additional information. This method reads that information and
   * determines if the operation should be retried.
   *
   * This method first checks if the operation was involved in a transaction. If so, it returns false.
   * Then it checks that the context trying to retry this operation has the same config as the operation itself. This is
   * to prevent other components from retrying the operation. If the config names do no match, it returns false.
   * Otherwise or if the connection exception was not enriched, this method returns true.
   *
   * @param connectionException the {@link ConnectionException} thrown during the execution of the operation
   * @param contextConfigName the config name for the context that is attempting to retry the operation
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
}
