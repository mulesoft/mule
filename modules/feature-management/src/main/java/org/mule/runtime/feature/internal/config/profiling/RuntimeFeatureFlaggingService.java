/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.config.profiling;

import org.mule.runtime.api.config.Feature;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.profiling.ProfilingProducerScope;
import org.mule.runtime.api.profiling.type.ProfilingEventType;

/**
 * A {@link FeatureFlaggingService} with some extra functionalities that are only used internally by the runtime. For using this
 * extra functionalities, the implementation has to be injected using this interface.
 */
public interface RuntimeFeatureFlaggingService extends FeatureFlaggingService {

  /**
   * Gets a {@link ProfilingDataProducerStatus} according to a {@link ProfilingProducerScope}.
   *
   * @param profilingEventType    the {@link ProfilingEventType}.
   * @param profilngProducerScope the {@link ProfilingProducerScope}.
   * @return
   */
  ProfilingDataProducerStatus getProfilingDataProducerStatus(ProfilingEventType<?> profilingEventType,
                                                             ProfilingProducerScope profilngProducerScope);


  /**
   * Register a profiling feature associated to a {@link ProfilingEventType} using a profiling feature suffix.
   *
   * @param profilingEventType
   * @param profilingFeatureSuffix
   */
  void registerProfilingFeature(ProfilingEventType<?> profilingEventType,
                                String profilingFeatureSuffix);

  /**
   * enables/disables a feature
   * 
   * @param profilingEventType     the {@link ProfilingEventType} to toggle.
   * @param profilingFeatureSuffix the suffix for the profiling feature.
   * @param status                 the status to set.
   */
  void toggleProfilingFeature(ProfilingEventType<?> profilingEventType, String profilingFeatureSuffix, boolean status);
}
