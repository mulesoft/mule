/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.consumer;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.MEMORY_BYTE_BUFFER_ALLOCATION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.MEMORY_BYTE_BUFFER_DEALLOCATION;
import static org.mule.runtime.core.internal.profiling.consumer.ComponentProfilingUtils.getByteBufferProfilingInfo;

import static com.google.common.collect.ImmutableSet.of;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ByteBufferProviderEventContext;
import org.mule.runtime.core.internal.profiling.consumer.annotations.RuntimeInternalProfilingDataConsumer;

import java.util.Set;
import java.util.function.Predicate;

import com.google.gson.Gson;
import org.slf4j.Logger;

/**
 * A {@link ProfilingDataConsumer} that logs information regarding allocation/deallocation of memory buffers.
 */
@RuntimeInternalProfilingDataConsumer
public class LoggerByteBufferAllocationProfilingDataConsumer
    implements ProfilingDataConsumer<ByteBufferProviderEventContext> {

  private static final Logger LOGGER = getLogger(LoggerByteBufferAllocationProfilingDataConsumer.class);

  private final Gson gson = new Gson();

  @Override
  public void onProfilingEvent(ProfilingEventType<ByteBufferProviderEventContext> profilingEventType,
                               ByteBufferProviderEventContext profilingEventContext) {
    Logger logger = getDataConsumerLogger();
    if (logger.isDebugEnabled()) {
      logger.debug(gson.toJson(getByteBufferProfilingInfo(profilingEventType, profilingEventContext)));
    }
  }

  @Override
  public Set<ProfilingEventType<ByteBufferProviderEventContext>> getProfilingEventTypes() {
    return of(MEMORY_BYTE_BUFFER_ALLOCATION, MEMORY_BYTE_BUFFER_DEALLOCATION);
  }

  @Override
  public Predicate<ByteBufferProviderEventContext> getEventContextFilter() {
    return processingStrategyProfilingEventContext -> true;
  }

  /**
   * @return the logger used for consuming the profiling data.
   */
  protected Logger getDataConsumerLogger() {
    return LOGGER;
  }
}
