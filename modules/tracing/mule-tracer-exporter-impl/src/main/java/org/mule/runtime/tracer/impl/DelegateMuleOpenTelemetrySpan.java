/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.tracer.api.span.InternalSpan;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.trace.StatusCode.ERROR;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.mule.runtime.tracer.api.span.error.InternalSpanError.getInternalSpanError;

public class DelegateMuleOpenTelemetrySpan implements MuleOpenTelemetrySpan {

  private static final TextMapSetter<Map<String, String>> SETTER = Map::put;

  public static final String EXCEPTION_EVENT_NAME = "exception";
  public static final AttributeKey<String> EXCEPTION_TYPE_KEY = stringKey("exception.type");
  public static final AttributeKey<String> EXCEPTION_MESSAGE_KEY = stringKey("exception.message");
  public static final AttributeKey<String> EXCEPTION_STACK_TRACE_KEY = stringKey("exception.stacktrace");
  public static final AttributeKey<Boolean> EXCEPTION_ESCAPED_KEY = booleanKey("exception.escaped");

  public static final String EXCEPTIONS_HAS_BEEN_RECORDED = "Exceptions has been recorded.";

  private final Span delegate;
  private Set<String> noExportableUntil = new HashSet<>();
  private boolean notIntercepting;
  private boolean customizedInfoCarrier;


  public DelegateMuleOpenTelemetrySpan(Span delegate) {
    this.delegate = delegate;
  }

  @Override
  public Context getSpanOpenTelemetryContext() {
    return Context.current().with(delegate);
  }

  @Override
  public void end(InternalSpan internalSpan) {
    if (internalSpan.hasErrors()) {
      delegate.setStatus(ERROR, EXCEPTIONS_HAS_BEEN_RECORDED);
      recordSpanExceptions(internalSpan);
    }
    internalSpan.getAttributes().forEach(delegate::setAttribute);
    delegate.end(internalSpan.getDuration().getEnd(), NANOSECONDS);
  }

  @Override
  public Map<String, String> getDistributedTraceContextMap() {
    Map<String, String> contextMap = new HashMap<>();
    OpenTelemetryResources.getPropagator().getTextMapPropagator().inject(getSpanOpenTelemetryContext(), contextMap, SETTER);
    return contextMap;
  }

  @Override
  public void setNoExportUntil(Set<String> noExportableUntil) {
    this.noExportableUntil = noExportableUntil;
  }


  @Override
  public Set<String> getNoExportUntil() {
    return noExportableUntil;
  }

  @Override
  public void setNotIntercepting(boolean propagateUpdateName) {
    this.notIntercepting = propagateUpdateName;
  }

  @Override
  public void setCustomizableInformationCarrier(boolean propagateSpanFromParent) {
    this.customizedInfoCarrier = propagateSpanFromParent;
  }

  @Override
  public boolean isNotIntercepting() {
    return notIntercepting;
  }

  @Override
  public boolean isSetCustomizableInformationCarrier() {
    return customizedInfoCarrier;
  }

  @Override
  public <T> Span setAttribute(AttributeKey<T> attributeKey, T t) {
    return delegate.setAttribute(attributeKey, t);
  }

  @Override
  public Span setAttribute(String key, String value) {
    delegate.setAttribute(key, value);
    return this;
  }

  @Override
  public Span addEvent(String s, Attributes attributes) {
    return delegate.addEvent(s, attributes);
  }

  @Override
  public Span addEvent(String s, Attributes attributes, long l, TimeUnit timeUnit) {
    return delegate.addEvent(s, attributes, l, timeUnit);
  }

  @Override
  public Span setStatus(StatusCode statusCode, String s) {
    return delegate.setStatus(statusCode, s);
  }

  @Override
  public Span recordException(Throwable throwable, Attributes attributes) {
    return delegate.recordException(throwable, attributes);
  }

  @Override
  public Span updateName(String newName) {
    return delegate.updateName(newName);
  }

  @Override
  public void end() {
    delegate.end();
  }

  @Override
  public void end(long l, TimeUnit timeUnit) {
    delegate.end(l, timeUnit);
  }

  @Override
  public SpanContext getSpanContext() {
    return delegate.getSpanContext();
  }

  @Override
  public boolean isRecording() {
    return delegate.isRecording();
  }

  private void recordSpanExceptions(InternalSpan internalSpan) {
    internalSpan.getErrors().forEach(this::recordSpanException);
  }

  private void recordSpanException(SpanError spanError) {
    Attributes errorAttributes = Attributes.of(
                                               EXCEPTION_TYPE_KEY, spanError.getError().getErrorType().toString(),
                                               EXCEPTION_MESSAGE_KEY, spanError.getError().getDescription(),
                                               EXCEPTION_STACK_TRACE_KEY,
                                               getInternalSpanError(spanError).getErrorStacktrace().toString(),
                                               EXCEPTION_ESCAPED_KEY, spanError.isEscapingSpan());
    delegate.addEvent(EXCEPTION_EVENT_NAME, errorAttributes);
  }

}
