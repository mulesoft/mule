/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.span.factory;

import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.exporter.api.SpanExporterFactory;
import org.mule.runtime.tracer.impl.span.ExportOnEndExecutionSpan;

import javax.inject.Inject;

public class ExecutionSpanFactory implements EventSpanFactory {

  @Inject
  private SpanExporterFactory spanExporterFactory;

  @Override
  public InternalSpan getSpan(SpanContext spanContext,
                              InitialSpanInfo initialSpanInfo) {
    return ExportOnEndExecutionSpan.createExportOnEndExecutionSpan(spanExporterFactory, spanContext.getSpan().orElse(null),
                                                                   initialSpanInfo);
  }

  @Override
  public SpanSnifferManager getSpanSnifferManager() {
    return spanExporterFactory.getSpanSnifferManager();
  }
}
