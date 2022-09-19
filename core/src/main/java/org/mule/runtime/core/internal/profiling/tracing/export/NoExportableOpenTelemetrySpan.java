/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.export;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;

import java.util.concurrent.TimeUnit;

/**
 * A {@link Span} that is not propagated nor exportable.
 *
 * @since 4.5.0
 */
@SuppressWarnings("NullableProblems")
public class NoExportableOpenTelemetrySpan implements Span {

  private static final Span INSTANCE = new NoExportableOpenTelemetrySpan();

  public static Span getNoExportableOpentelemetrySpan() {
    return INSTANCE;
  }

  private NoExportableOpenTelemetrySpan() {}

  @Override
  public <T> Span setAttribute(AttributeKey<T> attributeKey, T t) {
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
    // Nothing to do.
  }

  @Override
  public void end(long l, TimeUnit timeUnit) {
    // Nothing to do.
  }

  @Override
  public SpanContext getSpanContext() {
    return SpanContext.getInvalid();
  }

  @Override
  public boolean isRecording() {
    return false;
  }
}
