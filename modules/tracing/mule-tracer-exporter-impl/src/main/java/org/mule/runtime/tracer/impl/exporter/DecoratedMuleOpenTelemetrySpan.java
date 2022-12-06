/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.tracer.api.span.error.InternalSpanError.getInternalSpanError;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.common.Attributes.of;
import static io.opentelemetry.api.trace.StatusCode.ERROR;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.impl.exporter.optel.resources.OpenTelemetryResources;
import org.mule.runtime.tracer.impl.exporter.optel.span.MuleOpenTelemetrySpan;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;

/**
 * An exportable opentelemetry span that adds more info on export.
 *
 * @since 4.5.0
 */
public class DecoratedMuleOpenTelemetrySpan implements MuleOpenTelemetrySpan {

  private static final TextMapSetter<Map<String, String>> SETTER = Map::put;

  public static final String EXCEPTION_EVENT_NAME = "exception";
  public static final AttributeKey<String> EXCEPTION_TYPE_KEY = stringKey("exception.type");
  public static final AttributeKey<String> EXCEPTION_MESSAGE_KEY = stringKey("exception.message");
  public static final AttributeKey<String> EXCEPTION_STACK_TRACE_KEY = stringKey("exception.stacktrace");
  public static final AttributeKey<Boolean> EXCEPTION_ESCAPED_KEY = booleanKey("exception.escaped");

  public static final String EXCEPTIONS_HAS_BEEN_RECORDED = "Exceptions has been recorded.";
  public static final String ARTIFACT_ID = "artifact.id";
  public static final String ARTIFACT_TYPE = "artifact.type";

  public static final String SPAN_KIND = "span.kind.override";
  public static final String STATUS = "status.override";
  public static final String RECORD_EVENTS_READABLE_SPAN_CLASS = "io.opentelemetry.sdk.trace.RecordEventsReadableSpan";
  public static final String SPAN_KIND_FIELD_NAME = "kind";
  public static final Field SPAN_KIND_FIELD = getSpanKindField();

  private static Field getSpanKindField() {
    try {
      Field spanKindField =  Class.forName(RECORD_EVENTS_READABLE_SPAN_CLASS).getDeclaredField(SPAN_KIND_FIELD_NAME);
      spanKindField.setAccessible(true);
      return spanKindField;
    } catch (NoSuchFieldException | ClassNotFoundException e) {
      return null;
    }
  }

  private final Span delegate;
  private Set<String> noExportableUntil = new HashSet<>();
  private boolean policy;
  private boolean root;

  public DecoratedMuleOpenTelemetrySpan(Span delegate) {
    this.delegate = delegate;
  }

  @Override
  public Context getSpanOpenTelemetryContext() {
    return Context.current().with(delegate);
  }

  @Override
  public void end(InternalSpan internalSpan, InitialSpanInfo initialSpanInfo, String artifactId, String artifactType) {
    if (internalSpan.hasErrors()) {
      delegate.setStatus(ERROR, EXCEPTIONS_HAS_BEEN_RECORDED);
      recordSpanExceptions(internalSpan);
    }
    initialSpanInfo.getInitialAttributes().forEach(delegate::setAttribute);
    updateSpanKind(internalSpan);
    updateSpanStatus(internalSpan);
    internalSpan.getAttributes().forEach(delegate::setAttribute);
    delegate.setAttribute(ARTIFACT_ID, artifactId);
    delegate.setAttribute(ARTIFACT_TYPE, artifactType);
    delegate.end(internalSpan.getDuration().getEnd(), NANOSECONDS);
  }

  private void updateSpanStatus(InternalSpan internalSpan) {
    String spanStatus = internalSpan.getAttributes().remove(STATUS);
    if (spanStatus != null){
      delegate.setStatus(StatusCode.valueOf(spanStatus));
    }
  }

  private void updateSpanKind(InternalSpan internalSpan) {
    String spanKind = internalSpan.getAttributes().remove(SPAN_KIND);
    if (spanKind != null) {
      if (delegate.getClass().getName().equals(RECORD_EVENTS_READABLE_SPAN_CLASS) && SPAN_KIND_FIELD != null) {
        try {
          SPAN_KIND_FIELD.set(delegate, SpanKind.valueOf(spanKind));
        } catch (IllegalAccessException e) {
          throw new MuleRuntimeException(createStaticMessage(format("Span Kind of span: %s could not be changed to: %s", internalSpan.getName(), spanKind)), e);
        }
      }
    }
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
  public void setPolicy(boolean policy) {
    this.policy = policy;
  }

  @Override
  public void setRoot(boolean root) {
    this.root = root;
  }

  @Override
  public boolean onlyPropagateNamesAndAttributes() {
    return policy;
  }

  @Override
  public boolean isRoot() {
    return root;
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
    Attributes errorAttributes = of(EXCEPTION_TYPE_KEY, spanError.getError().getErrorType().toString(),
        EXCEPTION_MESSAGE_KEY, spanError.getError().getDescription(),
        EXCEPTION_STACK_TRACE_KEY,
        getInternalSpanError(spanError).getErrorStacktrace().toString(),
        EXCEPTION_ESCAPED_KEY, spanError.isEscapingSpan());
    delegate.addEvent(EXCEPTION_EVENT_NAME, errorAttributes);
  }

}
