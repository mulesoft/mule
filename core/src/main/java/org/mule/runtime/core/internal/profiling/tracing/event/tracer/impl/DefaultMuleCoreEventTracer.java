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
import org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventExecutionSpanProvider;
import org.mule.runtime.core.internal.profiling.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanCustomizer;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.MuleCoreEventTracer;

/**
 * A default implementation for a {@link MuleCoreEventTracer}.
 *
 * @since 4.5.0
 */
public class DefaultMuleCoreEventTracer implements MuleCoreEventTracer {

  private final CoreEventExecutionSpanProvider defaultCoreEventExecutionSpanProvider;
  private final MuleConfiguration muleConfiguration;

  /**
   * @return a builder for a {@link DefaultMuleCoreEventTracer}.
   */
  public static DefaultMuleEventTracerBuilder getMuleEventTracerBuilder() {
    return new DefaultMuleEventTracerBuilder();
  }

  private DefaultMuleCoreEventTracer(MuleConfiguration muleConfiguration,
                                     CoreEventExecutionSpanProvider coreEventExecutionSpanProvider) {
    this.muleConfiguration = muleConfiguration;
    this.defaultCoreEventExecutionSpanProvider = coreEventExecutionSpanProvider;
  }

  @Override
  public InternalSpan startComponentExecutionSpan(CoreEvent coreEvent, Component component) {
    return setCurrentContextSpanInEventContextIfPossible(coreEvent,
                                                         defaultCoreEventExecutionSpanProvider.getSpan(coreEvent, component,
                                                                                                       muleConfiguration));
  }

  @Override
  public InternalSpan startComponentExecutionSpan(CoreEvent coreEvent, Component component,
                                                  CoreEventSpanCustomizer coreEventSpanCustomizer) {
    return setCurrentContextSpanInEventContextIfPossible(coreEvent,
                                                         defaultCoreEventExecutionSpanProvider.getSpan(coreEvent, component,
                                                                                                       muleConfiguration,
                                                                                                       coreEventSpanCustomizer));
  }

  @Override
  public void endCurrentExecutionSpan(CoreEvent coreEvent) {
    endCurrentContextSpanIfPosibble(coreEvent);
  }

  private InternalSpan setCurrentContextSpanInEventContextIfPossible(CoreEvent coreEvent, InternalSpan currentSpan) {
    EventContext eventContext = coreEvent.getContext();

    if (eventContext instanceof DistributedTraceContextAware) {
      ((DistributedTraceContextAware) eventContext)
          .getDistributedTraceContext()
          .setContextCurrentSpan(currentSpan);
    }

    return currentSpan;
  }

  private void endCurrentContextSpanIfPosibble(CoreEvent coreEvent) {
    EventContext eventContext = coreEvent.getContext();
    if (eventContext instanceof DistributedTraceContextAware) {
      ((DistributedTraceContextAware) eventContext)
          .getDistributedTraceContext()
          .endCurrentContextSpan();
    }
  }

  /**
   * A Builder for a {@link DefaultMuleEventTracerBuilder}.
   *
   * @since 4.5.0
   */
  public static final class DefaultMuleEventTracerBuilder {

    private MuleConfiguration muleConfiguration;
    private CoreEventExecutionSpanProvider coreEventExecutionSpanProvider;

    public DefaultMuleEventTracerBuilder withMuleConfiguration(MuleConfiguration muleConfiguration) {
      this.muleConfiguration = muleConfiguration;
      return this;
    }

    public DefaultMuleEventTracerBuilder withDefaultCoreEventExecutionSpanProvider(CoreEventExecutionSpanProvider coreEventExecutionSpanProvider) {
      this.coreEventExecutionSpanProvider = coreEventExecutionSpanProvider;
      return this;

    }

    public MuleCoreEventTracer build() {
      return new DefaultMuleCoreEventTracer(muleConfiguration, coreEventExecutionSpanProvider);
    }
  }
}


