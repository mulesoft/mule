/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.producer;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingProducerScope;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ExtensionProfilingEventContext;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;
import org.mule.runtime.core.internal.profiling.ResettableProfilingDataProducer;
import org.mule.runtime.core.internal.profiling.consumer.annotations.RuntimeInternalProfilingDataConsumer;
import org.mule.runtime.feature.internal.config.profiling.ProfilingFeatureFlaggingService;
import org.mule.runtime.feature.internal.config.profiling.ProfilingDataProducerStatus;

import java.util.function.Function;

/**
 * A {@link ProfilingDataProducer} for producing data related to extensions.
 *
 * @see org.mule.runtime.api.profiling.type.ExtensionProfilingEventType
 * @since 4.4
 */
@RuntimeInternalProfilingDataConsumer
public class ExtensionProfilingDataProducer
    implements ResettableProfilingDataProducer<ExtensionProfilingEventContext, Object> {

  private final DefaultProfilingService defaultProfilingService;
  private final ProfilingEventType<ExtensionProfilingEventContext> profilingEventType;
  private ProfilingDataProducerStatus profilingProducerStatus;


  public ExtensionProfilingDataProducer(DefaultProfilingService defaultProfilingService,
                                        ProfilingEventType<ExtensionProfilingEventContext> profilingEventType,
                                        ProfilingProducerScope profilingProducerContext,
                                        ProfilingFeatureFlaggingService featureFlaggingService) {
    this.defaultProfilingService = defaultProfilingService;
    this.profilingEventType = profilingEventType;
    this.profilingProducerStatus =
        featureFlaggingService.getProfilingDataProducerStatus(profilingEventType, profilingProducerContext);
  }

  @Override
  public void triggerProfilingEvent(ExtensionProfilingEventContext profilingEventContext) {
    if (profilingProducerStatus == null) {
      reset();
    }

    if (profilingProducerStatus.isEnabled()) {
      defaultProfilingService.notifyEvent(profilingEventContext, profilingEventType);
    }
  }

  @Override
  public void triggerProfilingEvent(Object sourceData, Function<Object, ExtensionProfilingEventContext> transformation) {
    if (profilingProducerStatus.isEnabled()) {
      defaultProfilingService.notifyEvent(transformation.apply(sourceData), profilingEventType);
    }
  }

  @Override
  public void reset() {
    profilingProducerStatus.reset();
  }

}
