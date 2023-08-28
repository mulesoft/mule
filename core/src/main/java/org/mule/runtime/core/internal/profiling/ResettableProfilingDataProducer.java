/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.profiling;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingEventContext;

/**
 * A {@link ProfilingDataProducer} that may be resetted. It is useful to propagate changes. For example, when new
 * {@link ProfilingDataProducer} are discovered, as this would involve a change in the enablement status of the producer.
 *
 * @param <T> the class that encapsulates the data for the profiling event context.
 * @param <S> the source class to produce the profiling data.
 *
 * @since 4.5.0
 */
public interface ResettableProfilingDataProducer<T extends ProfilingEventContext, S> extends ProfilingDataProducer<T, S> {

  /**
   * Resets {@link ProfilingDataProducer} modifying internal status.
   * 
   * @see DefaultProfilingService
   */
  void reset();

}
