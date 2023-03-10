/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.context;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.tracer.api.span.ExportableInternalSpan;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.mule.runtime.tracer.api.span.ExportableInternalSpan.asExportable;
import static org.mule.runtime.tracer.impl.clock.Clock.getDefault;

/**
 * A {@link InternalSpan} that invokes the end of the span when the parent span ends.
 *
 * All the attributes for the parent span will also be propagated to the spans.
 */
public class NoExportOnEndInternalSpan implements ExportableInternalSpan {

  private final ExportableInternalSpan delegate;
  private long endTime;

  public NoExportOnEndInternalSpan(InternalSpan delegate) {
    this.delegate = asExportable(delegate);
  }

  @Override
  public ExportableInternalSpan updateChildSpanExporter(InternalSpan childInternalSpan) {
    return this.delegate.updateChildSpanExporter(childInternalSpan);
  }

  @Override
  public void updateRootName(String rootName) {
    delegate.updateRootName(rootName);
  }

  @Override
  public void setRootAttribute(String rootAttributeKey, String rootAttributeValue) {
    delegate.setRootAttribute(rootAttributeKey, rootAttributeValue);
  }

  @Override
  public void end() {
    this.endTime = getDefault().now();
  }

  @Override
  public void end(long endTime) {
    this.delegate.end(endTime);
  }

  @Override
  public void addError(InternalSpanError error) {
    this.delegate.addError(error);
  }

  @Override
  public void updateName(String name) {
    this.delegate.updateName(name);
  }

  @Override
  public SpanExporter getSpanExporter() {
    return delegate.getSpanExporter();
  }

  @Override
  public void forEachAttribute(BiConsumer<String, String> biConsumer) {
    delegate.forEachAttribute(biConsumer);
  }

  @Override
  public Map<String, String> serializeAsMap() {
    return delegate.serializeAsMap();
  }

  @Override
  public int getAttributesCount() {
    return delegate.getAttributesCount();
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
  public List<SpanError> getErrors() {
    return delegate.getErrors();
  }

  @Override
  public boolean hasErrors() {
    return delegate.hasErrors();
  }

  @Override
  public void addAttribute(String key, String value) {
    delegate.addAttribute(key, value);
  }

  @Override
  public void export() {
    this.delegate.end(endTime);
  }
}
