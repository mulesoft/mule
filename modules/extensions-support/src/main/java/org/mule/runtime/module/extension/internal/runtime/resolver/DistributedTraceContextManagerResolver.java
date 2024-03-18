/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;


import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.parameter.PropagateAllDistributedTraceContextManager;
import org.mule.runtime.tracer.api.EventTracer;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

/**
 * {@link ArgumentResolver} that yields instances of {@link DistributedTraceContextManager}
 *
 * @since 4.5.0
 */
public class DistributedTraceContextManagerResolver implements ArgumentResolver<DistributedTraceContextManager> {

  private final EventTracer<CoreEvent> coreEventEventTracer;

  public DistributedTraceContextManagerResolver(EventTracer<CoreEvent> coreEventEventTracer) {
    this.coreEventEventTracer = coreEventEventTracer;
  }

  @Override
  public DistributedTraceContextManager resolve(ExecutionContext executionContext) {
    return new PropagateAllDistributedTraceContextManager(((ExecutionContextAdapter<?>) executionContext)
        .getEvent(), coreEventEventTracer);
  }
}
