/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.runtime.parameter;

import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils.resolveDistributedTraceContext;

import static java.util.Collections.unmodifiableMap;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.CoreEventTracer;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link DistributedTraceContextManager} that injects all the fields of the propagator context of the event.
 *
 * @since 4.5.0
 */
public class PropagateAllDistributedTraceContextManager implements DistributedTraceContextManager {

  private Map<String, String> contextMap;
  private final CoreEvent coreEvent;
  private final CoreEventTracer coreEventTracer;

  public PropagateAllDistributedTraceContextManager(CoreEvent coreEvent, CoreEventTracer coreEventTracer) {
    this.coreEventTracer = coreEventTracer;
    this.coreEvent = coreEvent;
  }

  private void resolveContextMap(DistributedTraceContext distributedTraceContext) {
    Map<String, String> contextMapToBuild = new HashMap<>(distributedTraceContext.tracingFieldsAsMap());
    contextMapToBuild.putAll(distributedTraceContext.baggageItemsAsMap());
    contextMap = unmodifiableMap(contextMapToBuild);
  }

  @Override
  public void setRemoteTraceContextMap(Map<String, String> contextMap) {
    this.contextMap = contextMap;
  }

  @Override
  public Map<String, String> getRemoteTraceContextMap() {
    if (contextMap == null) {
      resolveContextMap(resolveDistributedTraceContext(coreEvent, coreEventTracer));;
    }
    return contextMap;
  }

  @Override
  public void setCurrentSpanName(String name) {
    coreEventTracer.setCurrentSpanName(coreEvent, name);
  }

  @Override
  public void addCurrentSpanAttribute(String key, String value) {
    coreEventTracer.addCurrentSpanAttribute(coreEvent, key, value);

  }

  @Override
  public void addCurrentSpanAttributes(Map<String, String> attributes) {
    coreEventTracer.addCurrentSpanAttributes(coreEvent, attributes);
  }
}
