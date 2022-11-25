/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method.distributedtracecontext;

import static org.mule.runtime.tracer.api.span.validation.Assertion.SUCCESSFUL_ASSERTION;

import static java.util.Optional.of;

import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.span.info.StartSpanInfo;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.impl.span.method.StartEventSpanMethod;
import org.mule.runtime.tracer.impl.span.factory.EventSpanFactory;

import java.util.Optional;

/**
 * A {@link StartEventSpanMethod} that takes the {@link SpanContext} as carrier for the current span.
 *
 * @since 4.5.0
 */
public class DistributedTraceContextStartSpanMethod implements StartEventSpanMethod<SpanContext> {

  private final MuleConfiguration muleConfiguration;
  private final ArtifactType artifactType;
  private final EventSpanFactory spanFactory;
  private final String artifactId;

  public static DistributedTraceContextStartSpanMethodBuilder getDistributedTraceContextStartSpanMethodBuilder() {
    return new DistributedTraceContextStartSpanMethodBuilder();
  }

  private DistributedTraceContextStartSpanMethod(MuleConfiguration muleConfiguration,
                                                 ArtifactType artifactType,
                                                 String artifactId,
                                                 EventSpanFactory spanFactory) {
    this.muleConfiguration = muleConfiguration;
    this.artifactType = artifactType;
    this.artifactId = artifactId;
    this.spanFactory = spanFactory;
  }

  @Override
  public Optional<InternalSpan> start(SpanContext context, CoreEvent coreEvent,
                                      StartSpanInfo spanCustomizationInfo) {
    return start(context, coreEvent, spanCustomizationInfo, SUCCESSFUL_ASSERTION);
  }

  @Override
  public Optional<InternalSpan> start(SpanContext context, CoreEvent coreEvent,
                                      StartSpanInfo spanCustomizationInfo,
                                      Assertion assertion) {
    InternalSpan newSpan;
    newSpan = spanFactory.getSpan(context, coreEvent, artifactId, artifactType, spanCustomizationInfo);
    context.setSpan(newSpan, assertion);
    return of(newSpan);
  }

  /**
   * Builder for a {@link DistributedTraceContextStartSpanMethod}.
   *
   * @since 4.5.0
   */
  public static class DistributedTraceContextStartSpanMethodBuilder {

    public static final String MULE_CONFIGURATION_IS_NULL_MESSAGE = "Mule configuration is null";
    public static final String ARTIFACT_TYPE_IS_NULL_MESSAGE = "Artifact type is null";
    public static final String EVENT_SPAN_FACTORY_IS_NULL_MESSAGE = "Event span factory is null";
    public static final String ARTIFACT_ID_IS_NULL_MESSAGE = "Artifact id is null";
    private String artifactId;

    private MuleConfiguration muleConfiguration;

    private ArtifactType artifactType;

    private EventSpanFactory eventSpanFactory;

    private DistributedTraceContextStartSpanMethodBuilder() {}

    /**
     * @param muleConfiguration {@link MuleConfiguration} the mule configuration.
     * @return the corresponding builder.
     */
    public DistributedTraceContextStartSpanMethodBuilder withMuleConfiguration(MuleConfiguration muleConfiguration) {
      this.muleConfiguration = muleConfiguration;
      return this;
    }

    /**
     * @param artifactType {@link ArtifactType} the artifact type.
     * @return the corresponding builder.
     */
    public DistributedTraceContextStartSpanMethodBuilder withArtifactType(ArtifactType artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    /**
     * @param eventSpanFactory {@link EventSpanFactory} the event span factory.
     * @return the corresponding event span factory.
     */
    public DistributedTraceContextStartSpanMethodBuilder withEventSpanFactory(EventSpanFactory eventSpanFactory) {
      this.eventSpanFactory = eventSpanFactory;
      return this;
    }


    public DistributedTraceContextStartSpanMethodBuilder withArtifactId(String artifacId) {
      this.artifactId = artifacId;
      return this;
    }

    public DistributedTraceContextStartSpanMethod build() {
      if (muleConfiguration == null) {
        throw new IllegalArgumentException(MULE_CONFIGURATION_IS_NULL_MESSAGE);
      }

      if (artifactType == null) {
        throw new IllegalArgumentException(ARTIFACT_TYPE_IS_NULL_MESSAGE);
      }

      if (artifactId == null) {
        throw new IllegalArgumentException(ARTIFACT_ID_IS_NULL_MESSAGE);
      }

      if (eventSpanFactory == null) {
        throw new IllegalArgumentException(EVENT_SPAN_FACTORY_IS_NULL_MESSAGE);
      }

      return new DistributedTraceContextStartSpanMethod(muleConfiguration, artifactType, artifactId, eventSpanFactory);
    }

  }
}
