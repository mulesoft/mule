/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.ComponentSpanIdentifier.componentSpanIdentifierFrom;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getCurrentSpan;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.execution.tracing.DistributedTraceContextAware;
import org.mule.runtime.core.internal.profiling.tracing.Clock;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExportOnEndSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.InternalSpanExportManager;
import org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanFactory;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizationInfo;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExecutionSpan;

import java.util.Map;

/**
 * A {@link CoreEventSpanFactory} that provides {@link org.mule.runtime.api.profiling.tracing.Span} that exports the
 * {@link org.mule.runtime.api.profiling.tracing.Span} on end.
 *
 * @since 4.5.0
 */
public class ExportOnEndCoreEventSpanFactory implements CoreEventSpanFactory {

  private final InternalSpanExportManager<EventContext> internalSpanExportManager;

  public ExportOnEndCoreEventSpanFactory(InternalSpanExportManager<EventContext> internalSpanExportManager) {
    this.internalSpanExportManager = internalSpanExportManager;
  }

  @Override
  public InternalSpan getSpan(CoreEvent coreEvent, MuleConfiguration muleConfiguration,
                              ArtifactType artifactType,
                              SpanCustomizationInfo spanCustomizationInfo) {
    return getExportOnEndSpan(muleConfiguration,
                              artifactType,
                              coreEvent,
                              spanCustomizationInfo);
  }

  private ExportOnEndSpan getExportOnEndSpan(MuleConfiguration muleConfiguration,
                                             ArtifactType artifactType,
                                             CoreEvent coreEvent, SpanCustomizationInfo spanCustomizationInfo) {

    EventContext eventContext = coreEvent.getContext();

    ExportOnEndSpan exportOnEndSpan = new ExportOnEndSpan(new ExecutionSpan(spanCustomizationInfo.getName(coreEvent),
                                                                            componentSpanIdentifierFrom(muleConfiguration.getId(),
                                                                                                        eventContext
                                                                                                            .getCorrelationId()),
                                                                            Clock.getDefault().now(),
                                                                            null,
                                                                            getCurrentSpan(eventContext).orElse(null),
                                                                            spanCustomizationInfo.isPolicySpan()),
                                                          eventContext,
                                                          internalSpanExportManager,
                                                          spanCustomizationInfo.getChildSpanCustomizationInfo(),
                                                          muleConfiguration,
                                                          spanCustomizationInfo.isExportable(coreEvent),
                                                          spanCustomizationInfo.noExportUntil());

    updateSpanNameAndAttributes(eventContext, exportOnEndSpan);


    Map<String, String> attributes =
        spanCustomizationInfo.getAttributes(coreEvent, muleConfiguration, artifactType);

    for (Map.Entry<String, String> entry : attributes.entrySet()) {
      exportOnEndSpan.addAttribute(entry.getKey(), entry.getValue());
    }

    return exportOnEndSpan;
  }

  private void updateSpanNameAndAttributes(EventContext eventContext, ExportOnEndSpan exportOnEndSpan) {
    if (eventContext instanceof DistributedTraceContextAware) {
      DistributedTraceContext distributedTraceContext =
          ((DistributedTraceContextAware) eventContext).getDistributedTraceContext();
      distributedTraceContext.updateSpanNameAndAttributes(exportOnEndSpan);
    }
  }
}
