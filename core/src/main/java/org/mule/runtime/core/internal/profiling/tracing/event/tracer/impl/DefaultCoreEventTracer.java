/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.execution.tracing.DistributedTraceContextAware;
import org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanFactory;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanCustomizer;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.InternalSpanExportManager;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel.ExportOnEndCoreEventSpanFactory;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.CoreEventTracer;

/**
 * A default implementation for a {@link CoreEventTracer}.
 *
 * @since 4.5.0
 */
public class DefaultCoreEventTracer implements CoreEventTracer {

  private final CoreEventSpanFactory coreEventSpanFactory;
  private final MuleConfiguration muleConfiguration;

  /**
   * @return a builder for a {@link DefaultCoreEventTracer}.
   */
  public static DefaultEventTracerBuilder getCoreEventTracerBuilder() {
    return new DefaultEventTracerBuilder();
  }

  private DefaultCoreEventTracer(MuleConfiguration muleConfiguration,
                                 InternalSpanExportManager<EventContext> spanExportManager) {
    this.muleConfiguration = muleConfiguration;
    this.coreEventSpanFactory = new ExportOnEndCoreEventSpanFactory(spanExportManager);
  }

  @Override
  public InternalSpan startComponentSpan(CoreEvent coreEvent, Component component) {
    return startCurrentSpanIfPossible(coreEvent,
                                      coreEventSpanFactory.getSpan(coreEvent, component,
                                                                   muleConfiguration));
  }

  @Override
  public InternalSpan startComponentSpan(CoreEvent coreEvent, Component component,
                                         CoreEventSpanCustomizer coreEventSpanCustomizer) {
    return startCurrentSpanIfPossible(coreEvent,
                                      coreEventSpanFactory.getSpan(coreEvent, component,
                                                                   muleConfiguration,
                                                                   coreEventSpanCustomizer));
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

  /**
   * A Builder for a {@link DefaultEventTracerBuilder}.
   *
   * @since 4.5.0
   */
  public static final class DefaultEventTracerBuilder {

    private MuleConfiguration muleConfiguration;
    private InternalSpanExportManager<EventContext> spanExportManager;

    public DefaultEventTracerBuilder withMuleConfiguration(MuleConfiguration muleConfiguration) {
      this.muleConfiguration = muleConfiguration;
      return this;
    }

    public DefaultCoreEventTracer build() {
      return new DefaultCoreEventTracer(muleConfiguration, spanExportManager);
    }

    public DefaultEventTracerBuilder withSpanExporterManager(InternalSpanExportManager<EventContext> spanExportManager) {
      this.spanExportManager = spanExportManager;
      return this;
    }
  }
}


