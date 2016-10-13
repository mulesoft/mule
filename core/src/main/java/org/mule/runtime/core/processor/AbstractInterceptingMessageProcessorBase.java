/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.mule.runtime.core.api.processor.MessageProcessors.newChain;
import static reactor.core.publisher.Flux.from;
import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.util.ObjectUtils;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation that provides the infrastructure for intercepting message processors. It doesn't implement
 * InterceptingMessageProcessor itself, to let individual subclasses make that decision \. This simply provides an implementation
 * of setNext and holds the next message processor as an attribute.
 */
public abstract class AbstractInterceptingMessageProcessorBase extends AbstractAnnotatedObject
    implements Processor, MuleContextAware, FlowConstructAware {

  protected Logger logger = LoggerFactory.getLogger(getClass());

  protected MuleContext muleContext;
  protected FlowConstruct flowConstruct;

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
    if (next instanceof MuleContextAware) {
      ((MuleContextAware) next).setMuleContext(context);
    }
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  public final Processor getListener() {
    return next;
  }

  public void setListener(Processor next) {
    this.next = next;
  }

  protected Processor next;

  protected Event processNext(Event event) throws MuleException {
    if (next == null) {
      return event;
    } else if (event == null) {
      if (logger.isDebugEnabled()) {
        logger.trace("MuleEvent is null.  Next MessageProcessor '" + next.getClass().getName() + "' will not be invoked.");
      }
      return null;
    } else {
      if (logger.isTraceEnabled()) {
        logger.trace("Invoking next MessageProcessor: '" + next.getClass().getName() + "' ");
      }
      return next.process(event);
    }
  }

  public MuleContext getMuleContext() {
    return muleContext;
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }

  protected boolean isEventValid(Event event) {
    return event != null;
  }

  protected Publisher<Event> applyNext(Publisher<Event> publisher) {
    if (next == null) {
      return publisher;
    }
    return from(publisher).doOnNext(event -> logNextMessageProcessorInvocation()).transform(next);
  }

  private void logNextMessageProcessorInvocation() {
    if (logger.isTraceEnabled()) {
      logger.trace("Invoking next MessageProcessor: '" + next.getClass().getName() + "' ");
    }
  }

}
