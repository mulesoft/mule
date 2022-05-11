/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.producer;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingProducerScope;
import org.mule.runtime.api.profiling.threading.ThreadSnapshotCollector;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentThreadingProfilingEventContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;
import org.mule.runtime.core.internal.profiling.ResettableProfilingDataProducer;
import org.mule.runtime.feature.internal.config.profiling.ProfilingFeatureFlaggingService;
import org.mule.runtime.feature.internal.config.profiling.ProfilingDataProducerStatus;

import java.util.function.Function;

/**
 * A {@link ProfilingDataProducer} for producing data related to component threading.
 *
 * @see org.mule.runtime.api.profiling.type.ComponentThreadingProfilingEventType
 * @since 4.4
 */
public class ComponentThreadingProfilingDataProducer
    implements ResettableProfilingDataProducer<ComponentThreadingProfilingEventContext, CoreEvent> {

  private final DefaultProfilingService defaultProfilingService;
  private final ProfilingEventType<ComponentThreadingProfilingEventContext> profilingEventType;
  private final ThreadSnapshotCollector threadSnapshotCollector;
  private ProfilingDataProducerStatus profilingProducerStatus;

  public ComponentThreadingProfilingDataProducer(DefaultProfilingService defaultProfilingService,
                                                 ProfilingEventType<ComponentThreadingProfilingEventContext> profilingEventType,
                                                 ThreadSnapshotCollector threadSnapshotCollector,
                                                 ProfilingProducerScope profilingProducerContext,
                                                 ProfilingFeatureFlaggingService featureFlaggingService) {
    this.defaultProfilingService = defaultProfilingService;
    this.profilingEventType = profilingEventType;
    this.threadSnapshotCollector = threadSnapshotCollector;
    this.profilingProducerStatus =
        featureFlaggingService.getProfilingDataProducerStatus(profilingEventType, profilingProducerContext);
  }

  @Override
  public void triggerProfilingEvent(ComponentThreadingProfilingEventContext eventContext) {
    if (profilingProducerStatus.isEnabled()) {
      eventContext.setThreadSnapshot(threadSnapshotCollector.getCurrentThreadSnapshot());
      defaultProfilingService.notifyEvent(eventContext, profilingEventType);
    }
  }

  @Override
  public void triggerProfilingEvent(CoreEvent sourceData,
                                    Function<CoreEvent, ComponentThreadingProfilingEventContext> transformation) {
    if (profilingProducerStatus.isEnabled()) {
      ComponentThreadingProfilingEventContext eventContext = transformation.apply(sourceData);
      eventContext.setThreadSnapshot(threadSnapshotCollector.getCurrentThreadSnapshot());
      defaultProfilingService.notifyEvent(eventContext, profilingEventType);
    }
  }

  @Override
  public void reset() {
    profilingProducerStatus.reset();
  }
}
