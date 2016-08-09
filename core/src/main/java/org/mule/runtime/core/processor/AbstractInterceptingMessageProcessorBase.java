/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.InternalMessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorContainer;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.util.NotificationUtils;
import org.mule.runtime.core.util.ObjectUtils;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation that provides the infrastructure for intercepting message processors. It doesn't implement
 * InterceptingMessageProcessor itself, to let individual subclasses make that decision \. This simply provides an implementation
 * of setNext and holds the next message processor as an attribute.
 */
public abstract class AbstractInterceptingMessageProcessorBase extends AbstractAnnotatedObject
    implements MessageProcessor, MuleContextAware, MessageProcessorContainer {

  protected Logger logger = LoggerFactory.getLogger(getClass());

  protected MuleContext muleContext;

  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  public final MessageProcessor getListener() {
    return next;
  }

  public void setListener(MessageProcessor next) {
    this.next = next;
  }

  protected MessageProcessor next;

  protected MuleEvent processNext(MuleEvent event) throws MuleException {
    if (next == null) {
      return event;
    } else if (event == null) {
      if (logger.isDebugEnabled()) {
        logger.trace("MuleEvent is null.  Next MessageProcessor '" + next.getClass().getName() + "' will not be invoked.");
      }
      return null;
    } else if (VoidMuleEvent.getInstance().equals(event)) {
      return event;
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

  protected boolean isEventValid(MuleEvent event) {
    return event != null && !(event instanceof VoidMuleEvent);
  }

  @Override
  public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
    if (next instanceof InternalMessageProcessor) {
      return;
    }
    if (next instanceof MessageProcessorChain) {
      NotificationUtils.addMessageProcessorPathElements(((MessageProcessorChain) next).getMessageProcessors(),
                                                        pathElement.getParent());
    } else if (next != null) {
      NotificationUtils.addMessageProcessorPathElements(Arrays.asList(next), pathElement.getParent());
    }
  }
}
