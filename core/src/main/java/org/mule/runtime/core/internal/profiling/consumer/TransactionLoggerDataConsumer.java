/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_COMMIT;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_CONTINUE;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_ROLLBACK;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_START;
import static org.mule.runtime.core.internal.profiling.consumer.ComponentProfilingUtils.getTxInfo;
import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.collect.ImmutableSet.of;

import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.TransactionProfilingEventContext;
import org.mule.runtime.core.internal.profiling.consumer.annotations.RuntimeInternalProfilingDataConsumer;
import java.util.Set;
import java.util.function.Predicate;
import com.google.gson.Gson;
import org.slf4j.Logger;

/**
 * A {@link ProfilingDataConsumer} that logs transaction profiling events.
 */
@RuntimeInternalProfilingDataConsumer
public class TransactionLoggerDataConsumer implements ProfilingDataConsumer<TransactionProfilingEventContext> {

  private static final Logger LOGGER = getLogger(TransactionLoggerDataConsumer.class);
  private final Gson gson = new Gson();

  @Override
  public void onProfilingEvent(ProfilingEventType<TransactionProfilingEventContext> profilingEventType,
                               TransactionProfilingEventContext profilingEventContext) {
    Logger logger = getDataConsumerLogger();
    if (logger.isDebugEnabled()) {
      logger.debug(gson.toJson(getTxInfo(profilingEventType, profilingEventContext)));
    }
  }

  @Override
  public Set<ProfilingEventType<TransactionProfilingEventContext>> getProfilingEventTypes() {
    return of(TX_START, TX_COMMIT, TX_CONTINUE, TX_ROLLBACK);
  }

  @Override
  public Predicate<TransactionProfilingEventContext> getEventContextFilter() {
    return txCtx -> true;
  }

  protected Logger getDataConsumerLogger() {
    return LOGGER;
  }

}
