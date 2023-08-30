/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import static java.util.Objects.requireNonNull;

import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder.MessagingExceptionHandlerAware;

import org.mule.runtime.core.privileged.profiling.tracing.ComponentTracerAware;
import org.mule.runtime.tracer.api.component.ComponentTracer;
import org.mule.runtime.tracer.api.component.ComponentTracerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a simple route with the {@link Processor} it leads to.
 *
 * @since 4.3.0
 */
public class ProcessorRoute extends AbstractComponent implements MuleContextAware, Lifecycle {

  private final static Logger LOGGER = LoggerFactory.getLogger(ProcessorRoute.class);
  public static final String PROCESSOR_ROUTE_SPAN_NAME = "route";

  private final Processor processor;
  private ComponentTracer componentTracer;

  // just let the error be propagated to the outer chain...
  private FlowExceptionHandler messagingExceptionHandler = (exception, event) -> null;
  private MuleContext muleContext;

  public ProcessorRoute(Processor processor, ComponentTracerFactory componentTracerFactory) {
    requireNonNull(processor, "processor can't be null");
    this.processor = processor;
    this.componentTracer = componentTracerFactory.fromComponent(this, PROCESSOR_ROUTE_SPAN_NAME, "");
  }

  public Processor getProcessor() {
    return processor;
  }

  @Override
  public String toString() {
    return reflectionToString(this, SHORT_PREFIX_STYLE);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
    if (processor instanceof MuleContextAware) {
      ((MuleContextAware) processor).setMuleContext(context);
    }
  }

  public void setMessagingExceptionHandler(FlowExceptionHandler messagingExceptionHandler) {
    this.messagingExceptionHandler = messagingExceptionHandler;
  }

  public void setComponentTracer(ComponentTracer componentTracer) {
    this.componentTracer = componentTracer;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (processor instanceof MessagingExceptionHandlerAware) {
      ((MessagingExceptionHandlerAware) processor).setMessagingExceptionHandler(messagingExceptionHandler);
    }
    if (processor instanceof ComponentTracerAware) {
      ((ComponentTracerAware) processor)
          .setComponentTracer(componentTracer);
    }
    initialiseIfNeeded(processor, muleContext);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(processor);
  }

  /**
   * @param session an {@link ExpressionManagerSession}
   * @return whether this route can be taken given the context
   */
  public boolean accepts(ExpressionManagerSession session) {
    return true;
  }

  /**
   * @return an {@link ExecutableRoute} for this one.
   */
  ExecutableRoute toExecutableRoute() {
    return new ExecutableRoute(this);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(processor);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(processor, LOGGER);
  }
}
