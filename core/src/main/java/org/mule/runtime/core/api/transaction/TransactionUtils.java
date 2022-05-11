/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transaction;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_START;
import static org.mule.runtime.api.tx.TransactionType.LOCAL;
import static org.mule.runtime.api.tx.TransactionType.XA;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static java.lang.System.currentTimeMillis;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.TransactionProfilingEventType;
import org.mule.runtime.api.profiling.type.context.TransactionProfilingEventContext;
import org.mule.runtime.api.tx.TransactionType;
import org.mule.runtime.core.internal.profiling.context.DefaultTransactionProfilingEventContext;
import org.mule.runtime.core.privileged.transaction.TransactionAdapter;

public final class TransactionUtils {

  private TransactionUtils() {

  }

  /**
   * Triggers a Profiling Event of type {@link TransactionProfilingEventType} if there is an ongoing transaction (or it is being
   * started with this event).
   */
  public static void profileTransactionAction(ProfilingDataProducer<TransactionProfilingEventContext, Object> dataProducer,
                                              ProfilingEventType<TransactionProfilingEventContext> type,
                                              ComponentLocation location) {
    if (!isTransactionActive() && !type.equals(TX_START)) {
      return;
    }
    TransactionAdapter tx = (TransactionAdapter) TransactionCoordination.getInstance().getTransaction();
    TransactionType txType = tx.isXA() ? XA : LOCAL;
    dataProducer.triggerProfilingEvent(new DefaultTransactionProfilingEventContext(tx.getComponentLocation(), location, txType,
                                                                                   currentTimeMillis()));
  }

}
