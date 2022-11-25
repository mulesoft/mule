/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.factory;

import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.info.StartSpanInfo;
import org.mule.runtime.tracer.exporter.api.SpanExporterFactory;

import javax.inject.Inject;

import static org.mule.runtime.tracer.impl.span.ExecutionSpan.getExecutionSpanBuilder;

public class ExportOnEndEventSpanFactory implements EventSpanFactory {

  public static final String ARTIFACT_ID = "artifact.id";
  public static final String ARTIFACT_TYPE = "artifact.type";

  @Inject
  private SpanExporterFactory spanExporterFactory;

  @Override
  public InternalSpan getSpan(SpanContext spanContext, CoreEvent coreEvent,
                              String artifactId,
                              ArtifactType artifactType, StartSpanInfo startSpanInfo) {
    InternalSpan internalSpan = getExecutionSpanBuilder()
        .withSpanCustomizationInfo(startSpanInfo)
        .withArtifactId(artifactId)
        .withParentSpan(spanContext.getSpan().orElse(null))
        .withSpanExporterFactory(spanExporterFactory)
        .build();

    startSpanInfo.getStartAttributes().forEach(internalSpan::addAttribute);

    internalSpan.addAttribute(ARTIFACT_ID, artifactId);
    internalSpan.addAttribute(ARTIFACT_TYPE, artifactType.getAsString());

    return internalSpan;
  }

  @Override
  public SpanSnifferManager getSpanSnifferManager() {
    return spanExporterFactory.getSpanExporterManager();
  }
}
