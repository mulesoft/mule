/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.producer;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.threading.ThreadSnapshotCollector;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentThreadingProfilingEventContext;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;

/**
 * A {@link ProfilingDataProducer} for producing data related to component threading.
 *
 * @see org.mule.runtime.api.profiling.type.ComponentThreadingProfilingEventType
 *
 * @since 4.4
 */
public class ComponentThreadingProfilingDataProducer
    implements ProfilingDataProducer<ComponentThreadingProfilingEventContext> {

  private final DefaultProfilingService defaultProfilingService;
  private final ProfilingEventType<ComponentThreadingProfilingEventContext> profilingEventType;
  private final ThreadSnapshotCollector threadSnapshotCollector;

  public ComponentThreadingProfilingDataProducer(DefaultProfilingService defaultProfilingService,
                                                 ProfilingEventType<ComponentThreadingProfilingEventContext> profilingEventType,
                                                 ThreadSnapshotCollector threadSnapshotCollector) {
    this.defaultProfilingService = defaultProfilingService;
    this.profilingEventType = profilingEventType;
    this.threadSnapshotCollector = threadSnapshotCollector;
  }

  @Override
  public void triggerProfilingEvent(ComponentThreadingProfilingEventContext eventContext) {
    eventContext.setThreadSnapshot(threadSnapshotCollector.getCurrentThreadSnapshot());
    defaultProfilingService.notifyEvent(eventContext, profilingEventType);
  }
}
