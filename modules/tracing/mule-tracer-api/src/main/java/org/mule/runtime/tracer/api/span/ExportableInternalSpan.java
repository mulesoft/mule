/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.api.span;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.mule.runtime.tracer.api.span.exporter.SpanExporter;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * An extension of {@link InternalSpan} that is exportable.
 */
public interface ExportableInternalSpan extends InternalSpan {

  /**
   * The span is exported.
   */
  void export();

  static ExportableInternalSpan asExportable(InternalSpan internalSpan) {
    if (internalSpan instanceof ExportableInternalSpan) {
      return (ExportableInternalSpan) internalSpan;
    }

    return new ExportableInternalSpanWrapper(internalSpan);
  }

  class ExportableInternalSpanWrapper implements ExportableInternalSpan {

    private final InternalSpan delegate;

    public ExportableInternalSpanWrapper(InternalSpan delegate) {
      this.delegate = delegate;
    }

    @Override
    public void export() {
      // Nothing to do.
    }

    @Override
    public void end() {
      this.delegate.end();
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
      return this.delegate.getSpanExporter();
    }

    @Override
    public ExportableInternalSpan updateChildSpanExporter(InternalSpan childInternalSpan) {
      return this.delegate.updateChildSpanExporter(childInternalSpan);
    }

    @Override
    public void forEachAttribute(BiConsumer<String, String> biConsumer) {
      this.delegate.forEachAttribute(biConsumer);
    }

    @Override
    public Map<String, String> serializeAsMap() {
      return this.delegate.serializeAsMap();
    }

    @Override
    public int getAttributesCount() {
      return this.delegate.getAttributesCount();
    }

    @Override
    public Span getParent() {
      return this.delegate.getParent();
    }

    @Override
    public SpanIdentifier getIdentifier() {
      return this.delegate.getIdentifier();
    }

    @Override
    public String getName() {
      return this.delegate.getName();
    }

    @Override
    public SpanDuration getDuration() {
      return this.delegate.getDuration();
    }

    @Override
    public List<SpanError> getErrors() {
      return this.delegate.getErrors();
    }

    @Override
    public boolean hasErrors() {
      return this.delegate.hasErrors();
    }
  }
}
