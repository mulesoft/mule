/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transaction;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.TransactionProfilingEventContext;
import org.mule.runtime.api.tx.TransactionType;
import org.mule.runtime.core.internal.profiling.context.DefaultTransactionProfilingEventContext;
import org.mule.runtime.core.privileged.transaction.TransactionAdapter;

import static java.lang.System.currentTimeMillis;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_START;
import static org.mule.runtime.api.tx.TransactionType.LOCAL;
import static org.mule.runtime.api.tx.TransactionType.XA;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;

public final class TransactionUtils {

  private TransactionUtils() {

  }

  public static void profileTransactionAction(ProfilingService profilingService,
                                              ProfilingEventType<TransactionProfilingEventContext> type,
                                              ComponentLocation location) {
    if (!isTransactionActive() && !type.equals(TX_START)) {
      return;
    }
    TransactionAdapter tx = (TransactionAdapter) TransactionCoordination.getInstance().getTransaction();
    TransactionType txType = tx.isXA() ? XA : LOCAL;
    profilingService.getProfilingDataProducer(type)
        .triggerProfilingEvent(new DefaultTransactionProfilingEventContext(tx.getComponentLocation(), location, txType,
                                                                           currentTimeMillis()));
  }

}
