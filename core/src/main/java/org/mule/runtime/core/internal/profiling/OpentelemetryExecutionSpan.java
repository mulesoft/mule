/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling;

import static org.mule.runtime.core.internal.trace.DistributedTraceContext.emptyDistributedEventContext;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.core.internal.execution.tracing.DistributedTraceContextAware;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;

import static java.time.Instant.ofEpochMilli;

import java.util.Map;
import javax.annotation.Nullable;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;

/**
 * A Wrapper for a span that manages open telemetry spans.
 *
 * @since 4.5.0
 */
public class OpentelemetryExecutionSpan implements InternalSpan {

  private static final TextMapGetter<Map<String, String>> getter = new MuleOpenTelemetryRemoteContextGetter();

  private final InternalSpan delegate;
  private final Tracer tracer;
  private io.opentelemetry.api.trace.Span opentelemetrySpan;
  private final DistributedTraceContext distributedTraceContext;

  public OpentelemetryExecutionSpan(InternalSpan delegate, EventContext eventContext, Tracer tracer) {
    this.tracer = tracer;
    this.delegate = delegate;
    this.distributedTraceContext = resolveDistributedTraceContext(eventContext);
  }

  @Override
  public Span getParent() {
    return delegate.getParent();
  }

  @Override
  public SpanIdentifier getIdentifier() {
    return delegate.getIdentifier();
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public SpanDuration getDuration() {
    return delegate.getDuration();
  }

  @Override
  public void end() {
    delegate.end();
    opentelemetrySpan.end();
  }

  @Override
  public InternalSpan getInternalParentSpan() {
    return delegate.getInternalParentSpan();
  }

  public io.opentelemetry.api.trace.Span getOpentelemetrySpan() {
    return opentelemetrySpan;
  }

  public OpentelemetryExecutionSpan startOpentelemetrySpan() {
    opentelemetrySpan = tracer.spanBuilder(delegate.getName())
        .setParent(resolveParentOpentelemetrySpan())
        .setStartTimestamp(ofEpochMilli(this.getDuration().getStart())).startSpan();

    return this;
  }

  private Context resolveParentOpentelemetrySpan() {
    Span parentSpan = delegate.getParent();

    if (parentSpan instanceof OpentelemetryExecutionSpan) {
      io.opentelemetry.api.trace.Span parentOpentelemetrySpan = ((OpentelemetryExecutionSpan) parentSpan).getOpentelemetrySpan();
      if (parentOpentelemetrySpan != null) {
        return Context.current().with(parentOpentelemetrySpan);
      }
    }

    return GlobalOpenTelemetry.get().getPropagators().getTextMapPropagator()
        .extract(Context.current(), distributedTraceContext.tracingFieldsAsMap(), getter);
  }


  private DistributedTraceContext resolveDistributedTraceContext(EventContext eventContext) {
    if (eventContext instanceof DistributedTraceContextAware) {
      return ((DistributedTraceContextAware) eventContext).getDistributedTraceContext();
    }

    return emptyDistributedEventContext();
  }

  /**
   * An Internal {@link TextMapGetter} to retrieve the remote span context.
   *
   * This is used to resolve a remote OpTel Span propagated through W3C Trace Context.
   */
  private static class MuleOpenTelemetryRemoteContextGetter implements TextMapGetter<Map<String, String>> {

    @Override
    public Iterable<String> keys(Map<String, String> stringStringMap) {
      return stringStringMap.keySet();
    }

    @Nullable
    @Override
    public String get(@Nullable Map<String, String> stringStringMap, String s) {
      if (stringStringMap == null) {
        return null;
      }

      return stringStringMap.get(s);
    }
  }
}
