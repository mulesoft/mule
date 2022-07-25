/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.InternalSpanExportManager;
import org.mule.runtime.core.internal.profiling.tracing.export.InternalSpanExporter;

/**
 * A wrapper for a span that exports spans on end.
 *
 * @since 4.5.0
 */
public class ExportOnEndSpan implements InternalSpan {

  private final InternalSpan runtimeInternalSpan;
  private final InternalSpanExporter spanExporter;

  public ExportOnEndSpan(InternalSpan runtimeInternalSpan, EventContext eventContext,
                         InternalSpanExportManager<EventContext> internalSpanExportManager) {
    this.runtimeInternalSpan = runtimeInternalSpan;
    this.spanExporter = internalSpanExportManager.getInternalSpanExporter(eventContext, this);
  }

  @Override
  public Span getParent() {
    return runtimeInternalSpan.getParent();
  }

  @Override
  public SpanIdentifier getIdentifier() {
    return runtimeInternalSpan.getIdentifier();
  }

  @Override
  public String getName() {
    return runtimeInternalSpan.getName();
  }

  @Override
  public SpanDuration getDuration() {
    return runtimeInternalSpan.getDuration();
  }

  @Override
  public void end() {
    runtimeInternalSpan.end();
    spanExporter.export(this);
  }

  @Override
  public <T> T visit(InternalSpanVisitor<T> visitor) {
    return visitor.accept(this);
  }

  public InternalSpanExporter getSpanExporter() {
    return spanExporter;
  }
}
