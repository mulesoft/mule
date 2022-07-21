/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.optel;

import static java.lang.System.currentTimeMillis;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.ComponentSpanIdentifier.componentSpanIdentifierFrom;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getSpanName;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getCurrentSpan;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.optel.OpenTelemetryResourcesProvider.getNewExportedSpanCapturer;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.optel.OpenTelemetryResourcesProvider.getOpentelemetryTracer;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.InternalSpan;
import org.mule.runtime.core.internal.profiling.OpentelemetrySpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanFactory;
import org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanCustomizer;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExecutionSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;

import io.opentelemetry.api.trace.Tracer;

/**
 * A {@link CoreEventSpanFactory} that provides open telemetry traced {@link org.mule.runtime.api.profiling.tracing.Span}.
 *
 * @since 4.5.0
 */
public class OpentelemetryTracedCoreEventSpanFactory implements CoreEventSpanFactory {

  private final Tracer openTelemetryTracer = getOpentelemetryTracer();

  private static final CoreEventSpanCustomizer defaultCoreEventSpanCustomizer = new DefaultEventSpanCustomizer();

  @Override
  public InternalSpan getSpan(CoreEvent event, Component component, MuleConfiguration muleConfiguration) {
    return getOpentelemetrySpan(component,
                                muleConfiguration,
                                event.getContext(),
                                defaultCoreEventSpanCustomizer.getName(event, component))
                                    .startOpentelemetrySpan();
  }

  @Override
  public InternalSpan getSpan(CoreEvent coreEvent, Component component, MuleConfiguration muleConfiguration,
                              CoreEventSpanCustomizer coreEventSpanCustomizer) {
    return getOpentelemetrySpan(component, muleConfiguration, coreEvent.getContext(),
                                coreEventSpanCustomizer.getName(coreEvent, component))
                                    .startOpentelemetrySpan();
  }

  private OpentelemetrySpan getOpentelemetrySpan(Component component, MuleConfiguration muleConfiguration,
                                                 EventContext eventContext, String name) {
    return new OpentelemetrySpan(new ExecutionSpan(name,
                                                   componentSpanIdentifierFrom(muleConfiguration.getId(),
                                                                               component.getLocation(),
                                                                               eventContext.getCorrelationId()),
                                                   currentTimeMillis(),
                                                   null,
                                                   getCurrentSpan(eventContext).orElse(null)),
                                 eventContext, openTelemetryTracer);
  }

  @Override
  public ExportedSpanCapturer getExportedSpanCapturer() {
    return getNewExportedSpanCapturer();
  }

  private static final class DefaultEventSpanCustomizer implements CoreEventSpanCustomizer {

    @Override
    public String getName(CoreEvent coreEvent, Component component) {
      return getSpanName(component.getIdentifier());
    }
  }
}
