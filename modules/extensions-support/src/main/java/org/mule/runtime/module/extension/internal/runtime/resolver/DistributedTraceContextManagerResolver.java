/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils.resolveDistributedTraceContext;

import static java.util.Optional.empty;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpanError;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.CoreEventTracer;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.TracingCondition;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.parameter.PropagateAllDistributedTraceContextManager;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * {@link ArgumentResolver} that yields instances of {@link DistributedTraceContextManager}
 *
 * @since 4.5.0
 */
public class DistributedTraceContextManagerResolver implements ArgumentResolver<DistributedTraceContextManager> {

  private final CoreEventTracer coreEventTracer;

  public DistributedTraceContextManagerResolver(CoreEventTracer coreEventTracer) {
    this.coreEventTracer = coreEventTracer;
  }

  @Override
  public DistributedTraceContextManager resolve(ExecutionContext executionContext) {
    return new PropagateAllDistributedTraceContextManager(resolveDistributedTraceContext(((ExecutionContextAdapter<?>) executionContext)
        .getEvent(), coreEventTracer));
  }

  private DistributedTraceContext getDistributedTraceContext(CoreEvent event) {
    Map<String, String> map = coreEventTracer.getDistributedTraceContextMap(event);

    return new DistributedTraceContext() {

      @Override
      public Optional<String> getTraceFieldValue(String key) {
        return Optional.ofNullable(map.get(key));
      }

      @Override
      public Map<String, String> tracingFieldsAsMap() {
        return map;
      }

      @Override
      public Optional<String> getBaggageItem(String key) {
        return empty();
      }

      @Override
      public Map<String, String> baggageItemsAsMap() {
        return new HashMap<>();
      }

      @Override
      public DistributedTraceContext copy() {
        return this;
      }

      @Override
      public void endCurrentContextSpan(TracingCondition tracingCondition) {
        // Nothing to do.
      }

      @Override
      public void recordErrorAtCurrentSpan(InternalSpanError error) {
        // Nothing to do.
      }

      @Override
      public void setCurrentSpan(InternalSpan span, TracingCondition tracingCondition) {
        // Nothing to do.
      }

      @Override
      public Optional<InternalSpan> getCurrentSpan() {
        return empty();
      }
    };
  }
}
