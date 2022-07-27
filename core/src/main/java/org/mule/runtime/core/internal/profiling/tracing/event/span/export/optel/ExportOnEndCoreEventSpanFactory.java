/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.ComponentSpanIdentifier.componentSpanIdentifierFrom;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getLocationAsString;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getSpanName;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils.getCurrentSpan;

import static java.lang.System.currentTimeMillis;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanUtils;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExportOnEndSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.InternalSpanExportManager;
import org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanFactory;
import org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanCustomizer;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExecutionSpan;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link CoreEventSpanFactory} that provides {@link org.mule.runtime.api.profiling.tracing.Span} that exports the
 * {@link org.mule.runtime.api.profiling.tracing.Span} on end.
 *
 * @since 4.5.0
 */
public class ExportOnEndCoreEventSpanFactory implements CoreEventSpanFactory {

  private static final CoreEventSpanCustomizer defaultCoreEventSpanCustomizer = new DefaultEventSpanCustomizer();
  private final InternalSpanExportManager<EventContext> internalSpanExportManager;

  public ExportOnEndCoreEventSpanFactory(InternalSpanExportManager<EventContext> internalSpanExportManager) {
    this.internalSpanExportManager = internalSpanExportManager;
  }

  @Override
  public InternalSpan getSpan(CoreEvent event, Component component, MuleConfiguration muleConfiguration,
                              ArtifactType artifactType) {
    return getExportOnEndSpan(component,
                              muleConfiguration,
                              artifactType,
                              event,
                              defaultCoreEventSpanCustomizer.getName(event, component));
  }

  @Override
  public InternalSpan getSpan(CoreEvent coreEvent, Component component, MuleConfiguration muleConfiguration,
                              ArtifactType artifactType,
                              CoreEventSpanCustomizer coreEventSpanCustomizer) {
    return getExportOnEndSpan(component, muleConfiguration,
                              artifactType,
                              coreEvent,
                              coreEventSpanCustomizer.getName(coreEvent, component));
  }

  private ExportOnEndSpan getExportOnEndSpan(Component component, MuleConfiguration muleConfiguration,
                                             ArtifactType artifactType,
                                             CoreEvent coreEvent, String name) {

    EventContext eventContext = coreEvent.getContext();

    ExportOnEndSpan exportOnEndSpan = new ExportOnEndSpan(new ExecutionSpan(name,
                                                                            componentSpanIdentifierFrom(muleConfiguration.getId(),
                                                                                                        component.getLocation(),
                                                                                                        eventContext
                                                                                                            .getCorrelationId()),
                                                                            currentTimeMillis(),
                                                                            null,
                                                                            getCurrentSpan(eventContext).orElse(null)),
                                                          eventContext,
                                                          internalSpanExportManager);


    Map<String, String> attributes =
        defaultCoreEventSpanCustomizer.getAttributes(coreEvent, component, muleConfiguration, artifactType);

    for (Map.Entry<String, String> entry : attributes.entrySet()) {
      exportOnEndSpan.addAttribute(entry.getKey(), entry.getValue());
    }

    return exportOnEndSpan;
  }

  private static final class DefaultEventSpanCustomizer implements CoreEventSpanCustomizer {

    public static final String LOCATION_KEY = "location";
    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String ARTIFACT_ID_KEY = "artifactId";
    public static final String ARTIFACT_TYPE_ID = "artifactType";
    public static final String THREAD_START_ID_KEY = "threadStartId";
    public static final String THREAD_START_NAME_KEY = "threadStartName";

    @Override
    public String getName(CoreEvent coreEvent, Component component) {
      return getSpanName(component.getIdentifier());
    }

    @Override
    public Map<String, String> getAttributes(CoreEvent coreEvent, Component component,
                                             MuleConfiguration muleConfiguration, ArtifactType artifactType) {
      Map<String, String> attributes = new HashMap<>();
      attributes.put(LOCATION_KEY, getLocationAsString(component.getLocation()));
      attributes.put(CORRELATION_ID_KEY, coreEvent.getCorrelationId());
      attributes.put(ARTIFACT_ID_KEY, muleConfiguration.getId());
      attributes.put(ARTIFACT_TYPE_ID, artifactType.getAsString());
      attributes.put(THREAD_START_ID_KEY, Long.toString(Thread.currentThread().getId()));
      attributes.put(THREAD_START_NAME_KEY, Thread.currentThread().getName());
      return attributes;
    }
  }
}
