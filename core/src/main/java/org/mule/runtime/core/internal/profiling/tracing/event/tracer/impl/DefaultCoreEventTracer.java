/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl;

import static java.util.Collections.emptyMap;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.execution.tracing.DistributedTraceContextAware;
import org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanFactory;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExportOnEndSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel.OpenTelemetryResourcesProvider;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizationInfo;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.InternalSpanExportManager;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel.ExportOnEndCoreEventSpanFactory;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.CoreEventTracer;

import java.util.Map;

/**
 * A default implementation for a {@link CoreEventTracer}.
 *
 * @since 4.5.0
 */
public class DefaultCoreEventTracer implements CoreEventTracer {

  private final CoreEventSpanFactory coreEventSpanFactory;
  private final MuleConfiguration muleConfiguration;
  private final ArtifactType artifactType;

  /**
   * @return a builder for a {@link DefaultCoreEventTracer}.
   */
  public static DefaultEventTracerBuilder getCoreEventTracerBuilder() {
    return new DefaultEventTracerBuilder();
  }

  private DefaultCoreEventTracer(MuleConfiguration muleConfiguration,
                                 ArtifactType artifactType,
                                 InternalSpanExportManager<EventContext> spanExportManager) {
    this.muleConfiguration = muleConfiguration;
    this.artifactType = artifactType;
    this.coreEventSpanFactory = new ExportOnEndCoreEventSpanFactory(spanExportManager);
  }

  @Override
  public InternalSpan startComponentSpan(CoreEvent coreEvent,
                                         SpanCustomizationInfo spanCustomizationInfo) {
    return startCurrentSpanIfPossible(coreEvent,
                                      coreEventSpanFactory.getSpan(coreEvent,
                                                                   muleConfiguration,
                                                                   artifactType,
                                                                   spanCustomizationInfo));
  }

  @Override
  public void endCurrentSpan(CoreEvent coreEvent) {
    endCurrentSpanIfPossible(coreEvent);
  }

  private InternalSpan startCurrentSpanIfPossible(CoreEvent coreEvent, InternalSpan currentSpan) {
    EventContext eventContext = coreEvent.getContext();

    if (eventContext instanceof DistributedTraceContextAware) {
      ((DistributedTraceContextAware) eventContext)
          .getDistributedTraceContext()
          .setCurrentSpan(currentSpan);
    }

    return currentSpan;
  }

  private void endCurrentSpanIfPossible(CoreEvent coreEvent) {
    EventContext eventContext = coreEvent.getContext();
    if (eventContext instanceof DistributedTraceContextAware) {
      ((DistributedTraceContextAware) eventContext)
          .getDistributedTraceContext()
          .endCurrentContextSpan();
    }
  }

  @Override
  public Map<String, String> getDistributedTraceContextMap(CoreEvent event) {
    if (event instanceof DistributedTraceContextAware) {
      DistributedTraceContext distributedTraceContext = ((DistributedTraceContextAware) event).getDistributedTraceContext();
      ExportOnEndSpan span = distributedTraceContext.getCurrentSpan().map(
                                                                          e -> getInternalSpanOpentelemetryExecutionSpanFunction(e))
          .orElse(null);

      if (span == null) {
        return emptyMap();
      }

      Map<String, String> contextMap = OpenTelemetryResourcesProvider.getDistributedTraceContextMap(span);
      contextMap.putAll(distributedTraceContext.tracingFieldsAsMap());
      contextMap.putAll(distributedTraceContext.baggageItemsAsMap());

      return contextMap;
    } else {
      return emptyMap();
    }
  }

  private ExportOnEndSpan getInternalSpanOpentelemetryExecutionSpanFunction(InternalSpan internalSpan) {
    if (internalSpan instanceof ExportOnEndSpan) {
      return (ExportOnEndSpan) internalSpan;
    }

    return null;
  }

  /**
   * A Builder for a {@link DefaultEventTracerBuilder}.
   *
   * @since 4.5.0
   */
  public static final class DefaultEventTracerBuilder {

    private MuleConfiguration muleConfiguration;
    private InternalSpanExportManager<EventContext> spanExportManager;
    private ArtifactType artifactType;

    public DefaultEventTracerBuilder withMuleConfiguration(MuleConfiguration muleConfiguration) {
      this.muleConfiguration = muleConfiguration;
      return this;
    }

    public DefaultEventTracerBuilder withSpanExporterManager(InternalSpanExportManager<EventContext> spanExportManager) {
      this.spanExportManager = spanExportManager;
      return this;
    }

    public DefaultEventTracerBuilder withArtifactType(ArtifactType artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    public DefaultCoreEventTracer build() {
      return new DefaultCoreEventTracer(muleConfiguration, artifactType, spanExportManager);
    }
  }
}


