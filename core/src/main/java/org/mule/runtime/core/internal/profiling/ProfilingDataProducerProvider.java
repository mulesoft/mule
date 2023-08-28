/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.profiling;

import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.ProfilingProducerScope;

/**
 * Provides {@link org.mule.runtime.api.profiling.ProfilingDataProducer}
 *
 * @param <T>
 */
public interface ProfilingDataProducerProvider<T extends ProfilingEventContext, O> {

  /**
   * @param profilingProducerScope the {@link ProfilingProducerScope} that determines the profiling data producer to provide
   * @param <T>                    the {@link ProfilingEventContext} for the provided @{link
   *                               {@link org.mule.runtime.api.profiling.ProfilingDataProducer}
   * @return the profiling data producer.
   */
  <T extends ProfilingEventContext, S> ResettableProfilingDataProducer<T, S> getProfilingDataProducer(
                                                                                                      ProfilingProducerScope profilingProducerScope);

}
