/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.core.internal.profiling.tracing.export.OpentelemetrySpanExporter.OPENTELEMETRY_SPAN_VISITOR;
import static org.mule.runtime.core.internal.trace.DistributedTraceContext.emptyDistributedEventContext;

import static java.util.Optional.empty;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.execution.tracing.DistributedTraceContextAware;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExportOnEndSpan;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.parameter.PropagateAllDistributedTraceContextManager;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;

/**
 * {@link ArgumentResolver} that yields instances of {@link DistributedTraceContextManager}
 *
 * @since 4.5.0
 */
public class DistributedTraceContextManagerResolver implements ArgumentResolver<DistributedTraceContextManager> {

  // Insert the context as Header
  TextMapSetter<Map<String, String>> setter =
      Map::put;

  @Override
  public DistributedTraceContextManager resolve(ExecutionContext executionContext) {
    return new PropagateAllDistributedTraceContextManager(getDistributedTraceContext(((ExecutionContextAdapter<?>) executionContext)
        .getEvent()));
  }

  private DistributedTraceContext getDistributedTraceContext(CoreEvent event) {
    if (event instanceof DistributedTraceContextAware) {
      DistributedTraceContext distributedTraceContext = ((DistributedTraceContextAware) event).getDistributedTraceContext();
      ExportOnEndSpan span = distributedTraceContext.getCurrentSpan().map(
                                                                          e -> getInternalSpanOpentelemetryExecutionSpanFunction(e))
          .orElse(null);
      Map<String, String> map = new HashMap<>();
      map.putAll(distributedTraceContext.tracingFieldsAsMap());
      map.putAll(distributedTraceContext.baggageItemsAsMap());
      GlobalOpenTelemetry.get().getPropagators().getTextMapPropagator()
          .inject(Context.current().with(span.getSpanExporter().visit(OPENTELEMETRY_SPAN_VISITOR)), map, setter);

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
        public void endCurrentContextSpan() {
          // Nothing to do.
        }

        @Override
        public void setCurrentSpan(InternalSpan span) {
          // Nothing to do.
        }

        @Override
        public Optional<InternalSpan> getCurrentSpan() {
          return empty();
        }
      };
    }

    return emptyDistributedEventContext();
  }

  private ExportOnEndSpan getInternalSpanOpentelemetryExecutionSpanFunction(InternalSpan internalSpan) {
    if (internalSpan instanceof ExportOnEndSpan) {
      return (ExportOnEndSpan) internalSpan;
    }

    return null;
  }
}
