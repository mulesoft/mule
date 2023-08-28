/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor;

import static org.mule.runtime.core.internal.component.ComponentUtils.getFromAnnotatedObjectOrFail;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.util.ObjectUtils;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;

/**
 * Abstract implementation that provides the infrastructure for intercepting message processors. It doesn't implement
 * InterceptingMessageProcessor itself, to let individual subclasses make that decision \. This simply provides an implementation
 * of setNext and holds the next message processor as an attribute.
 * 
 * @deprecated This is kept just because it is in a `privileged` package.
 */
@Deprecated
public abstract class AbstractInterceptingMessageProcessorBase extends AbstractComponent
    implements Processor, MuleContextAware {

  protected Logger logger = LoggerFactory.getLogger(getClass());

  protected MuleContext muleContext;

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
    if (next instanceof MuleContextAware) {
      ((MuleContextAware) next).setMuleContext(context);
    }
  }

  public FlowConstruct getFlowConstruct() {
    return getFromAnnotatedObjectOrFail(muleContext.getConfigurationComponentLocator(), this);
  }

  public final Processor getListener() {
    return next;
  }

  public void setListener(Processor next) {
    if (next != null) {
      this.next = next;
    }
  }

  protected Processor next = event -> event;

  protected CoreEvent processNext(CoreEvent event) throws MuleException {
    return processNext(event, Mono.from(((BaseEventContext) event.getContext()).getResponsePublisher()));
  }

  protected CoreEvent processNext(CoreEvent event, Publisher<CoreEvent> responsePublisher) throws MuleException {
    if (event == null) {
      if (logger.isDebugEnabled()) {
        logger.trace("MuleEvent is null.  Next MessageProcessor '" + next.getClass().getName() + "' will not be invoked.");
      }
      return null;
    } else {
      if (logger.isTraceEnabled()) {
        logger.trace("Invoking next MessageProcessor: '" + next.getClass().getName() + "' ");
      }
      return processToApply(event, next, false, responsePublisher);
    }
  }

  public MuleContext getMuleContext() {
    return muleContext;
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }

  protected boolean isEventValid(CoreEvent event) {
    return event != null;
  }

  protected ReactiveProcessor applyNext() {
    if (next == null) {
      return publisher -> publisher;
    }
    return publisher -> from(publisher).doOnNext(event -> logNextMessageProcessorInvocation()).transform(next);
  }

  private void logNextMessageProcessorInvocation() {
    if (logger.isTraceEnabled()) {
      logger.trace("Invoking next MessageProcessor: '" + next.getClass().getName() + "' ");
    }
  }

}
