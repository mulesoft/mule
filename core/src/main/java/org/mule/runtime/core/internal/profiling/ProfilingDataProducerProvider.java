/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
