/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl;

import static org.mule.runtime.core.internal.profiling.tracing.event.tracer.TracingCondition.NO_CONDITION;
import static org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl.DefaultCoreEventTracerUtils.safeExecuteWithDefaultOnThrowable;
import static org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl.DefaultCoreEventTracerUtils.safeExecute;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.event.trace.DistributedTraceContextGetter;
import org.mule.runtime.core.internal.event.trace.EventDistributedTraceContext;
import org.mule.runtime.core.internal.profiling.tracing.event.span.DefaultSpanError;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.TracingCondition;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.TracingConditionNotMetException;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizationInfo;
import org.mule.runtime.core.internal.execution.tracing.DistributedTraceContextAware;
import org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanFactory;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExportOnEndSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel.OpenTelemetryResourcesProvider;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.InternalSpanExportManager;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel.ExportOnEndCoreEventSpanFactory;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.CoreEventTracer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;

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
  private final Logger customLogger;

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
                                 Logger customLogger) {
    this.muleConfiguration = muleConfiguration;
    this.artifactType = artifactType;
    this.coreEventSpanFactory = new ExportOnEndCoreEventSpanFactory(spanExportManager);
    this.propagationOfExceptionsInTracing = propagationOfExceptionsInTracing;
    this.customLogger = customLogger;
  }

  @Override
  public Optional<InternalSpan> startComponentSpan(CoreEvent coreEvent,
                                                   SpanCustomizationInfo spanCustomizationInfo) {
    return startComponentSpan(coreEvent, spanCustomizationInfo, NO_CONDITION);
  }

  @Override
  public Optional<InternalSpan> startComponentSpan(CoreEvent coreEvent, SpanCustomizationInfo spanCustomizationInfo,
                                                   TracingCondition tracingCondition) {
    return safeExecuteWithDefaultOnThrowable(() -> of(startCurrentSpanIfPossible(coreEvent,
                                                                                 coreEventSpanFactory.getSpan(coreEvent,
                                                                                                              muleConfiguration,
                                                                                                              artifactType,
                                                                                                              spanCustomizationInfo),
                                                                                 tracingCondition)),
                                             empty(),
                                             "Error when starting a component span",
                                             propagationOfExceptionsInTracing,
                                             customLogger);
  }

  @Override
  public void endCurrentSpan(CoreEvent coreEvent) {
    endCurrentSpan(coreEvent, NO_CONDITION);
  }

  @Override
  public void endCurrentSpan(CoreEvent coreEvent, TracingCondition condition) {
    safeExecute(() -> endCurrentSpanIfPossible(coreEvent, condition), "Error on ending current span",
                propagationOfExceptionsInTracing,
                customLogger);
  }

  @Override
  public void recordErrorAtCurrentSpan(CoreEvent coreEvent, boolean isErrorEscapingCurrentSpan) {
    recordErrorAtCurrentSpan(coreEvent, () -> coreEvent.getError()
        .orElseThrow(() -> new IllegalArgumentException(String.format("Provided coreEvent [%s] does not declare an error.",
                                                                      coreEvent))),
                             isErrorEscapingCurrentSpan);
  }

  private void recordErrorAtCurrentSpan(CoreEvent coreEvent, Supplier<Error> spanError, boolean isErrorEscapingCurrentSpan) {
    safeExecute(() -> {
      EventContext eventContext = coreEvent.getContext();
      if (eventContext instanceof DistributedTraceContextAware) {
        ((DistributedTraceContextAware) eventContext)
            .getDistributedTraceContext()
            .recordErrorAtCurrentSpan(new DefaultSpanError(spanError.get(), coreEvent.getFlowCallStack(),
                                                           isErrorEscapingCurrentSpan));
      }
    }, "Error recording a span error at current span", propagationOfExceptionsInTracing, customLogger);
  }

  @Override
  public void injectDistributedTraceContext(EventContext eventContext,
                                            DistributedTraceContextGetter distributedTraceContextGetter) {
    if (eventContext instanceof DistributedTraceContextAware) {
      ((DistributedTraceContextAware) eventContext).setDistributedTraceContext(
                                                                               EventDistributedTraceContext.builder()
                                                                                   .withGetter(distributedTraceContextGetter)
                                                                                   .withPropagationOfExceptionsInTracing(propagationOfExceptionsInTracing)
                                                                                   .build());
    }
  }

  @Override
  public Map<String, String> getDistributedTraceContextMap(CoreEvent coreEvent) {
    return safeExecuteWithDefaultOnThrowable(() -> doGetDistributedTraceContextMap(coreEvent),
                                             emptyMap(),
                                             "Error on getting distributed trace context", propagationOfExceptionsInTracing,
                                             customLogger);
  }

  private InternalSpan startCurrentSpanIfPossible(CoreEvent coreEvent, InternalSpan currentSpan,
                                                  TracingCondition tracingCondition)
      throws TracingConditionNotMetException {
    EventContext eventContext = coreEvent.getContext();

    if (eventContext instanceof DistributedTraceContextAware) {
      ((DistributedTraceContextAware) eventContext)
          .getDistributedTraceContext()
          .setCurrentSpan(currentSpan, tracingCondition);
    }

    return currentSpan;
  }

  private Map<String, String> doGetDistributedTraceContextMap(CoreEvent coreEvent) {
    EventContext eventContext = coreEvent.getContext();
    if (eventContext instanceof DistributedTraceContextAware) {
      DistributedTraceContext distributedTraceContext =
          ((DistributedTraceContextAware) eventContext).getDistributedTraceContext();
      ExportOnEndSpan span = distributedTraceContext.getCurrentSpan().map(
                                                                          this::getInternalSpanOpenTelemetryExecutionSpanFunction)
          .orElse(null);

      if (span == null) {
        return emptyMap();
      }

      Map<String, String> contextMap = new HashMap<>();

      // First the remote context
      contextMap.putAll(distributedTraceContext.tracingFieldsAsMap());
      contextMap.putAll(distributedTraceContext.baggageItemsAsMap());
      // Then the current span. So that it will overwrite the common properties.
      contextMap.putAll(OpenTelemetryResourcesProvider.getDistributedTraceContextMap(span));

      return contextMap;
    } else {
      return emptyMap();
    }
  }

  private void endCurrentSpanIfPossible(CoreEvent coreEvent, TracingCondition condition) {
    EventContext eventContext = coreEvent.getContext();
    if (eventContext instanceof DistributedTraceContextAware) {
      ((DistributedTraceContextAware) eventContext)
          .getDistributedTraceContext()
          .endCurrentContextSpan(condition);
    }
  }

  private ExportOnEndSpan getInternalSpanOpenTelemetryExecutionSpanFunction(InternalSpan internalSpan) {
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


