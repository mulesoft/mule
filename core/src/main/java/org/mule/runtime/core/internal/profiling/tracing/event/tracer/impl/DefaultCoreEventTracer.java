/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl;

import static org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl.DefaultCoreEventTracerUtils.safeExecuteWithDefaultOnThrowable;
import static org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl.DefaultCoreEventTracerUtils.safeExecute;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.slf4j.LoggerFactory.getLogger;

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
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;

/**
 * A default implementation for a {@link CoreEventTracer}.
 *
 * @since 4.5.0
 */
public class DefaultCoreEventTracer implements CoreEventTracer {

  /**
   * logger used by this class.
   */
  private static final Logger LOGGER = getLogger(DefaultCoreEventTracer.class);

  private final CoreEventSpanFactory coreEventSpanFactory;
  private final MuleConfiguration muleConfiguration;
  private final ArtifactType artifactType;
  private final boolean propagationOfExceptionsInTracing;
  private final Logger logger;

  /**
   * @return a builder for a {@link DefaultCoreEventTracer}.
   */
  public static DefaultEventTracerBuilder getCoreEventTracerBuilder() {
    return new DefaultEventTracerBuilder();
  }

  private DefaultCoreEventTracer(MuleConfiguration muleConfiguration,
                                 ArtifactType artifactType,
                                 InternalSpanExportManager<EventContext> spanExportManager,
                                 boolean propagationOfExceptionsInTracing,
                                 Logger logger) {
    this.muleConfiguration = muleConfiguration;
    this.artifactType = artifactType;
    this.coreEventSpanFactory = new ExportOnEndCoreEventSpanFactory(spanExportManager);
    this.propagationOfExceptionsInTracing = propagationOfExceptionsInTracing;
    this.logger = logger;
  }

  @Override
  public Optional<InternalSpan> startComponentSpan(CoreEvent coreEvent,
                                                   SpanCustomizationInfo spanCustomizationInfo) {
    return safeExecuteWithDefaultOnThrowable(() -> of(startCurrentSpanIfPossible(coreEvent,
                                                                                 coreEventSpanFactory.getSpan(coreEvent,
                                                                                                              muleConfiguration,
                                                                                                              artifactType,
                                                                                                              spanCustomizationInfo))),
                                             empty(),
                                             "Error when starting a component span",
                                             propagationOfExceptionsInTracing,
                                             logger);
  }

  @Override
  public void endCurrentSpan(CoreEvent coreEvent) {
    safeExecute(() -> endCurrentSpanIfPossible(coreEvent), "Error on ending current span", propagationOfExceptionsInTracing,
                logger);
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

  @Override
  public Map<String, String> getDistributedTraceContextMap(CoreEvent coreEvent) {
    EventContext eventContext = coreEvent.getContext();
    if (eventContext instanceof DistributedTraceContextAware) {
      return safeExecuteWithDefaultOnThrowable(() -> doGetDistributedTraceContextMap((DistributedTraceContextAware) eventContext),
                                               emptyMap(),
                                               "Error on getting distributed trace context", propagationOfExceptionsInTracing,
                                               logger);
    } else {
      return emptyMap();
    }
  }

  private Map<String, String> doGetDistributedTraceContextMap(DistributedTraceContextAware event) {
    DistributedTraceContext distributedTraceContext = event.getDistributedTraceContext();
    ExportOnEndSpan span = distributedTraceContext.getCurrentSpan().map(
                                                                        this::getInternalSpanOpentelemetryExecutionSpanFunction)
        .orElse(null);

    if (span == null) {
      return emptyMap();
    }

    Map<String, String> contextMap = OpenTelemetryResourcesProvider.getDistributedTraceContextMap(span);
    contextMap.putAll(distributedTraceContext.tracingFieldsAsMap());
    contextMap.putAll(distributedTraceContext.baggageItemsAsMap());

    return contextMap;
  }

  private void endCurrentSpanIfPossible(CoreEvent coreEvent) {
    EventContext eventContext = coreEvent.getContext();
    if (eventContext instanceof DistributedTraceContextAware) {
      ((DistributedTraceContextAware) eventContext)
          .getDistributedTraceContext()
          .endCurrentContextSpan();
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
    private boolean propagateExceptionsInTracing;
    private Logger logger;

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

    public DefaultEventTracerBuilder withLogger(Logger logger) {
      this.logger = logger;
      return this;
    }

    public DefaultEventTracerBuilder withPropagationOfExceptionsInTracing(boolean propagateExceptionsInTracing) {
      this.propagateExceptionsInTracing = propagateExceptionsInTracing;
      return this;
    }

    public DefaultCoreEventTracer build() {
      return new DefaultCoreEventTracer(muleConfiguration, artifactType, spanExportManager, propagateExceptionsInTracing,
                                        resolveLogger());
    }

    private Logger resolveLogger() {
      if (logger != null) {
        return logger;
      } else {
        return LOGGER;
      }
    }
  }
}


