/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.ComponentSpanIdentifier.componentSpanIdentifierFrom;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getSpanName;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getCurrentSpan;

import static java.lang.System.currentTimeMillis;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExportOnEndSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.InternalSpanExporterFactory;
import org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanFactory;
import org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanCustomizer;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExecutionSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;

/**
 * A {@link CoreEventSpanFactory} that provides {@link org.mule.runtime.api.profiling.tracing.Span} that exports the
 * {@link org.mule.runtime.api.profiling.tracing.Span} on end.
 *
 * @since 4.5.0
 */
public class ExportOnEndCoreEventSpanFactory implements CoreEventSpanFactory {

  private static final CoreEventSpanCustomizer defaultCoreEventSpanCustomizer = new DefaultEventSpanCustomizer();
  private final InternalSpanExporterFactory<?> internalSpanExporterFactory;

  public ExportOnEndCoreEventSpanFactory(InternalSpanExporterFactory<?> internalSpanExporterFactory) {
    this.internalSpanExporterFactory = internalSpanExporterFactory;
  }

  @Override
  public InternalSpan getSpan(CoreEvent event, Component component, MuleConfiguration muleConfiguration) {
    return getExportOnEndSpan(component,
                              muleConfiguration,
                              event.getContext(),
                              defaultCoreEventSpanCustomizer.getName(event, component));
  }

  @Override
  public InternalSpan getSpan(CoreEvent coreEvent, Component component, MuleConfiguration muleConfiguration,
                              CoreEventSpanCustomizer coreEventSpanCustomizer) {
    return getExportOnEndSpan(component, muleConfiguration, coreEvent.getContext(),
                              coreEventSpanCustomizer.getName(coreEvent, component));
  }

  private ExportOnEndSpan getExportOnEndSpan(Component component, MuleConfiguration muleConfiguration,
                                             EventContext eventContext, String name) {
    return new ExportOnEndSpan(new ExecutionSpan(name,
                                                 componentSpanIdentifierFrom(muleConfiguration.getId(),
                                                                             component.getLocation(),
                                                                             eventContext.getCorrelationId()),
                                                 currentTimeMillis(),
                                                 null,
                                                 getCurrentSpan(eventContext).orElse(null)),
                               eventContext,
                               internalSpanExporterFactory);
  }

  @Override
  public ExportedSpanCapturer getExportedSpanCapturer() {
    return internalSpanExporterFactory.getExportedSpanCapturer();
  }

  private static final class DefaultEventSpanCustomizer implements CoreEventSpanCustomizer {

    @Override
    public String getName(CoreEvent coreEvent, Component component) {
      return getSpanName(component.getIdentifier());
    }
  }
}
