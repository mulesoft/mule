/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.producer;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentExecutionProfilingEventContext;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;
import org.mule.runtime.core.internal.profiling.context.ComponentExecutionProfilingEventContextWithThreadProfiling;
import org.mule.runtime.core.internal.profiling.threading.OperationThreadSnapshotCollector;

/**
 * Default {@link ProfilingDataProducer} returned by a diagnostic service.
 *
 * @since 4.4
 */
public class ComponentExecutionProfilingDataProducer
    implements ProfilingDataProducer<ComponentExecutionProfilingEventContext> {

  private final DefaultProfilingService defaultProfilingService;
  private final ProfilingEventType<ComponentExecutionProfilingEventContext> profilingEventType;
  private final OperationThreadSnapshotCollector operationThreadSnapshotCollector;

  public ComponentExecutionProfilingDataProducer(DefaultProfilingService defaultProfilingService,
                                                 ProfilingEventType<ComponentExecutionProfilingEventContext> profilingEventType,
                                                 OperationThreadSnapshotCollector operationThreadSnapshotCollector) {
    this.defaultProfilingService = defaultProfilingService;
    this.profilingEventType = profilingEventType;
    this.operationThreadSnapshotCollector = operationThreadSnapshotCollector;
  }

  @Override
  public void triggerProfilingEvent(ComponentExecutionProfilingEventContext profilingEventContext) {
    // TODO: Add a capability flag (could be something like "threading_profiling") has a separate task in order to check
    //  design and implementation of such feature.
    ComponentExecutionProfilingEventContext decorated = new ComponentExecutionProfilingEventContextWithThreadProfiling(profilingEventContext, operationThreadSnapshotCollector.collect());
    defaultProfilingService.notifyEvent(decorated, profilingEventType);
  }
}
