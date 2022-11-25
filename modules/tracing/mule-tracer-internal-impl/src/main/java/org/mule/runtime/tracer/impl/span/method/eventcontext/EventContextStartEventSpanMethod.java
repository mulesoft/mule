/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method.eventcontext;

import static org.mule.runtime.tracer.api.span.validation.Assertion.SUCCESSFUL_ASSERTION;
import static org.mule.runtime.tracer.impl.span.method.distributedtracecontext.DistributedTraceContextStartSpanMethod.getDistributedTraceContextStartSpanMethodBuilder;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.span.info.StartSpanInfo;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.impl.span.method.FailsafeDistributedTraceContextOperation;
import org.mule.runtime.tracer.impl.span.method.StartEventSpanMethod;
import org.mule.runtime.tracer.impl.span.factory.EventSpanFactory;
import org.mule.runtime.tracer.impl.span.method.distributedtracecontext.DistributedTraceContextGetter;
import org.mule.runtime.tracer.impl.span.method.distributedtracecontext.DistributedTraceContextStartSpanMethod;

import java.util.Optional;

import org.slf4j.Logger;

/**
 * A {@link StartEventSpanMethod} that uses {@link EventContext} for the {@link SpanContext}.
 *
 * @since 4.5.0
 */
public class EventContextStartEventSpanMethod implements StartEventSpanMethod<EventContext> {

  private static final DistributedTraceContextGetter<EventContext> DISTRIBUTED_TRACE_CONTEXT_GETTER =
      EventContextDistributedTraceContextGetter.getDistributedTraceContextGetter();

  public static final String ERROR_MESSAGE = "Error when starting a component span";
  public static final Class<EventContextStartEventSpanMethod> LOGGGER = EventContextStartEventSpanMethod.class;

  private final FailsafeDistributedTraceContextOperation failsafeDistributedTraceContextOperation;

  private final DistributedTraceContextStartSpanMethod distributedTraceContextStartSpanMethod;

  public static EventContextStartEventSpanMethodBuilder getEventContextStartEventSpanMethodBuilder() {
    return new EventContextStartEventSpanMethodBuilder();
  }

  private EventContextStartEventSpanMethod(FailsafeDistributedTraceContextOperation failsafeDistributedTraceContextOperation,
                                           DistributedTraceContextStartSpanMethod distributedTraceContextStartSpanMethod) {
    this.failsafeDistributedTraceContextOperation = failsafeDistributedTraceContextOperation;
    this.distributedTraceContextStartSpanMethod = distributedTraceContextStartSpanMethod;
  }

  @Override
  public Optional<InternalSpan> start(EventContext context, CoreEvent coreEvent,
                                      StartSpanInfo spanCustomizationInfo) {
    return start(context, coreEvent, spanCustomizationInfo, SUCCESSFUL_ASSERTION);
  }

  @Override
  public Optional<InternalSpan> start(EventContext context, CoreEvent coreEvent,
                                      StartSpanInfo spanCustomizationInfo,
                                      Assertion assertion) {
    return failsafeDistributedTraceContextOperation
        .execute(() -> distributedTraceContextStartSpanMethod
            .start(DISTRIBUTED_TRACE_CONTEXT_GETTER.getDistributedTraceContext(context), coreEvent, spanCustomizationInfo));
  }

  /**
   * Builder for {@link EventContextStartEventSpanMethod}
   */
  public static class EventContextStartEventSpanMethodBuilder {

    public static final String MULE_CONFIGURATION_IS_NULL_MESSAGE = "Mule configuration is null";
    public static final String ARTIFACT_TYPE_IS_NULL_MESSAGE = "Artifact type is null";
    public static final String EVENT_SPAN_FACTORY_IS_NULL_MESSAGE = "Event span factory is null";
    public static final String ARTIFACT_ID_IS_NULL_MESSAGE = "Artifact id is null";

    private boolean propagateErrors;

    private Logger logger = getLogger(LOGGGER);

    private MuleConfiguration muleConfiguration;

    private ArtifactType artifactType;

    private EventSpanFactory eventSpanFactory;

    private String artifacId;

    /**
     * @param propagateErrors if the method should propagate errors. This is intended for tests.
     *
     * @return the corresponding builder.
     */
    public EventContextStartEventSpanMethodBuilder propagateErrors(boolean propagateErrors) {
      this.propagateErrors = propagateErrors;
      return this;
    }

    /**
     * @param logger the logger to use.
     *
     * @return the corresponding builder.
     */
    public EventContextStartEventSpanMethodBuilder withLogger(Logger logger) {
      this.logger = logger;
      return this;
    }

    /**
     * @param muleConfiguration the {@link MuleConfiguration}
     *
     * @return the corresponding builder.
     */
    public EventContextStartEventSpanMethodBuilder withMuleConfiguration(MuleConfiguration muleConfiguration) {
      this.muleConfiguration = muleConfiguration;
      return this;
    }

    /**
     * @param artifactType the {@link ArtifactType}
     *
     * @return the corresponding builder.
     */
    public EventContextStartEventSpanMethodBuilder withArtifactType(ArtifactType artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    public EventContextStartEventSpanMethodBuilder withArtifactId(String artifactId) {
      this.artifacId = artifactId;
      return this;
    }

    /**
     * @param eventSpanFactory {@link EventSpanFactory} the event span factory.
     * @return the corresponding event span factory.
     */
    public EventContextStartEventSpanMethodBuilder withEventSpanFactory(EventSpanFactory eventSpanFactory) {
      this.eventSpanFactory = eventSpanFactory;
      return this;
    }

    public EventContextStartEventSpanMethod build() {

      if (muleConfiguration == null) {
        throw new IllegalArgumentException(MULE_CONFIGURATION_IS_NULL_MESSAGE);
      }

      if (artifactType == null) {
        throw new IllegalArgumentException(ARTIFACT_TYPE_IS_NULL_MESSAGE);
      }

      if (artifacId == null) {
        throw new IllegalArgumentException(ARTIFACT_ID_IS_NULL_MESSAGE);
      }

      if (eventSpanFactory == null) {
        throw new IllegalArgumentException(EVENT_SPAN_FACTORY_IS_NULL_MESSAGE);
      }

      FailsafeDistributedTraceContextOperation failsafeDistributedTraceContextOperation =
          FailsafeDistributedTraceContextOperation.getFailsafeDistributedTraceContextOperation(logger, ERROR_MESSAGE,
                                                                                               propagateErrors);

      DistributedTraceContextStartSpanMethod distributedTraceContextStartSpanMethod =
          getDistributedTraceContextStartSpanMethodBuilder()
              .withMuleConfiguration(muleConfiguration)
              .withArtifactType(artifactType)
              .withArtifactId(artifacId)
              .withEventSpanFactory(eventSpanFactory)
              .build();

      return new EventContextStartEventSpanMethod(failsafeDistributedTraceContextOperation,
                                                  distributedTraceContextStartSpanMethod);
    }
  }
}
