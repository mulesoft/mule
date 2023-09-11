/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.internal.memory.profiling;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.ProfilingProducerScope;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.threading.ThreadSnapshotCollector;
import org.mule.runtime.api.profiling.tracing.ExecutionContext;
import org.mule.runtime.api.profiling.tracing.TracingService;
import org.mule.runtime.api.profiling.type.ProfilingEventType;

import java.util.function.Function;

/**
 * A Profiling Service that disables all data production for the memory management. The {@link ProfilingDataProducer} implements
 * operations that do not propagate the profiling data.
 *
 * @since 4.5.0
 */
public class NoOpMemoryProfilingService implements ProfilingService {

  private static final NoOpMemoryProfilingService INSTANCE = new NoOpMemoryProfilingService();

  public static ProfilingService getNoOpMemoryProfilingService() {
    return INSTANCE;
  }

  private NoOpMemoryProfilingService() {}

  private final TracingService noOpTracingService = new TracingService() {

    @Override
    public ExecutionContext getCurrentExecutionContext() {
      return null;
    }

    @Override
    public void deleteCurrentExecutionContext() {
      // No op
    }

    @Override
    public ExecutionContext setCurrentExecutionContext(ExecutionContext executionContext) {
      return null;
    }

  };

  @SuppressWarnings("rawtypes")
  private final ProfilingDataProducer<?, ?> profilingDataProducer = new ProfilingDataProducer() {


    @Override
    public void triggerProfilingEvent(ProfilingEventContext profilerEventContext) {
      // No op
    }

    @Override
    public void triggerProfilingEvent(Object sourceData, Function transformation) {
      // No op
    }
  };

  @SuppressWarnings("unchecked")
  @Override
  public <T extends ProfilingEventContext, S> ProfilingDataProducer<T, S> getProfilingDataProducer(
                                                                                                   ProfilingEventType<T> profilingEventType) {
    return (ProfilingDataProducer<T, S>) profilingDataProducer;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends ProfilingEventContext, S> ProfilingDataProducer<T, S> getProfilingDataProducer(
                                                                                                   ProfilingEventType<T> profilingEventType,
                                                                                                   ProfilingProducerScope producerContext) {
    return (ProfilingDataProducer<T, S>) profilingDataProducer;
  }

  @Override
  public <T extends ProfilingEventContext, S> void registerProfilingDataProducer(ProfilingEventType<T> profilingEventType,
                                                                                 ProfilingDataProducer<T, S> profilingDataProducer) {
    // Nothing to do
  }

  @Override
  public ThreadSnapshotCollector getThreadSnapshotCollector() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TracingService getTracingService() {
    return noOpTracingService;
  }

}
