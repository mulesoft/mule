/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractConnectionException;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DO_NOT_RETRY;

import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.transaction.ExtensionTransactionKey;

/**
 * Utilities for handling reconnection on operations that use a connection.
 *
 * @since 4.2.3
 */
public class ReconnectionUtils {

  /**
   * @param t the {@link Throwable} thrown during the execution of the operation
   * @param context the {@link ExecutionContextAdapter} that contains the context information about the operation's execution
   *
   * @return whether or not the operation should be retried
   */
  public static boolean shouldRetry(Throwable t, ExecutionContextAdapter<?> context) {
    if (Boolean.valueOf(context.getVariable(DO_NOT_RETRY)) || !extractConnectionException(t).isPresent()) {
      return false;
    }

    if (isTransactionActive()) {
      Transaction tx = TransactionCoordination.getInstance().getTransaction();

      return !tx.hasResource(new ExtensionTransactionKey(context.getConfiguration().get()));
    }

    return true;
  }
}
