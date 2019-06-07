/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

public class ProcessorRoute extends AbstractComponent implements MuleContextAware, Lifecycle {

  private final static Logger LOGGER = LoggerFactory.getLogger(ProcessorRoute.class);

  private final Processor messageProcessor;
  private Flux<CoreEvent> publisher;
  private LazyValue<FluxSink<CoreEvent>> sink;

  private MuleContext muleContext;

  public ProcessorRoute(Processor messageProcessor) {
    requireNonNull(messageProcessor, "messageProcessor can't be null");
    this.messageProcessor = messageProcessor;
  }

  public Processor getMessageProcessor() {
    return messageProcessor;
  }

  public Flux<CoreEvent> getPublisher() {
    return publisher;
  }

  public FluxSink<CoreEvent> getSink() {
    return sink.get();
  }

  @Override
  public String toString() {
    return reflectionToString(this, SHORT_PREFIX_STYLE);
  }

  // This class being just a logic-less tuple, it directly delegates lifecyle
  // events to its members, without any control.

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
    if (messageProcessor instanceof MuleContextAware) {
      ((MuleContextAware) messageProcessor).setMuleContext(context);
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(messageProcessor, muleContext);
    FluxSinkRecorder<CoreEvent> sinkRef = new FluxSinkRecorder<>();
    publisher = Flux.create(sinkRef).transform(messageProcessor);
    sink = new LazyValue<>(() -> sinkRef.getFluxSink());
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(messageProcessor);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(messageProcessor);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(messageProcessor, LOGGER);
  }
}
