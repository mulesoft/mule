/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import static org.mule.runtime.core.api.util.ExceptionUtils.putContext;
import static org.mule.runtime.core.api.util.ExceptionUtils.updateMessagingException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replace any exception thrown with a MessagingException
 */
public class ExceptionToMessagingExceptionExecutionInterceptor implements MessageProcessorExecutionInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionToMessagingExceptionExecutionInterceptor.class);
  private MuleContext muleContext;
  private FlowConstruct flowConstruct;

  @Override
  public Event execute(Processor messageProcessor, Event event) throws MessagingException {
    try {
      return messageProcessor.process(event);
    } catch (Exception exception) {
      MessagingException messagingException;
      if (exception instanceof MessagingException) {
        messagingException = (MessagingException) exception;
      } else {
        messagingException = new MessagingException(event, exception, messageProcessor);
      }
      throw updateMessagingException(LOGGER, messageProcessor, messagingException, muleContext.getErrorTypeLocator(),
                                     muleContext.getErrorTypeRepository(), flowConstruct, muleContext);
    } catch (Throwable ex) {
      throw putContext(new MessagingException(event, ex, messageProcessor), messageProcessor, event, flowConstruct, muleContext);
    }
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }
}
