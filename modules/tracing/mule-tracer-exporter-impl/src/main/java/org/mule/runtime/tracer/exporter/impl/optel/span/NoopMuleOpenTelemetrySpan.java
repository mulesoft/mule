/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.exporter.impl.optel.span;

import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.impl.span.InternalSpan;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;

/**
 * An OpenTelemetry Span that does not export but propagates the root span.
 *
 * @since 4.5.0
 */
public class NoopMuleOpenTelemetrySpan implements MuleOpenTelemetrySpan {

  private MuleOpenTelemetrySpan rootSpan;
  private Set<String> noExportableUntil = Collections.emptySet();

  public NoopMuleOpenTelemetrySpan(
                                   MuleOpenTelemetrySpan rootSpan) {
    this.rootSpan = rootSpan;
  }

  public NoopMuleOpenTelemetrySpan() {}

  @Override
  public Context getSpanOpenTelemetryContext() {
    if (rootSpan == null) {
      return Context.current();
    }
    return Context.current().with(rootSpan);
  }

  @Override
  public void end(InternalSpan internalSpan, InitialSpanInfo initialSpanInfo, String artifactId, String artifactType) {
    // Noop
  }

  @Override
  public Set<String> getNoExportUntil() {
    return noExportableUntil;
  }

  @Override
  public Map<String, String> getDistributedTraceContextMap() {
    return rootSpan.getDistributedTraceContextMap();
  }

  @Override
  public void setNoExportUntil(Set<String> noExportableUntil) {
    this.noExportableUntil = noExportableUntil;
  }

  @Override
  public void setPolicy(boolean notIntercepting) {
    // Nothing to do
  }

  @Override
  public void setRoot(boolean propagateSpanFromParent) {
    // Nothing to do.
  }

  @Override
  public boolean onlyPropagateNamesAndAttributes() {
    return true;
  }

  @Override
  public boolean isRoot() {
    return false;
  }

  @Override
  public <T> Span setAttribute(AttributeKey<T> attributeKey, T value) {
    return this;
  }

  @Override
  public Span addEvent(String s, Attributes attributes) {
    return this;
  }

  @Override
  public Span addEvent(String s, Attributes attributes, long l, TimeUnit timeUnit) {
    return this;
  }

  @Override
  public Span setStatus(StatusCode statusCode, String s) {
    return this;
  }

  @Override
  public Span recordException(Throwable throwable, Attributes attributes) {
    return this;
  }

  @Override
  public Span updateName(String s) {
    return this;
  }

  @Override
  public void end() {
    // Noop
  }

  @Override
  public void end(long l, TimeUnit timeUnit) {
    // Noop
  }

  @Override
  public SpanContext getSpanContext() {
    if (rootSpan != null) {
      return rootSpan.getSpanContext();
    }
    return SpanContext.getInvalid();
  }

  @Override
  public boolean isRecording() {
    return false;
  }
}
