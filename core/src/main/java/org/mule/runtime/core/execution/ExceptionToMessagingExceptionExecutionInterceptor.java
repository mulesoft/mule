/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import static org.apache.commons.lang.exception.ExceptionUtils.getRootCause;
import static org.mule.runtime.core.util.ExceptionUtils.createErrorEvent;
import static org.mule.runtime.core.util.ExceptionUtils.getRootCauseException;
import static org.mule.runtime.core.util.ExceptionUtils.putContext;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.exception.ErrorMessageAwareException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.message.ErrorBuilder;
import org.mule.runtime.core.util.ExceptionUtils;

import java.util.Optional;

/**
 * Replace any exception thrown with a MessagingException
 */
public class ExceptionToMessagingExceptionExecutionInterceptor implements MessageProcessorExecutionInterceptor {

  private MuleContext muleContext;
  private FlowConstruct flowConstruct;

  @Override
  public Event execute(Processor messageProcessor, Event event) throws MessagingException {
    try {
      return messageProcessor.process(event);
    } catch (Exception exception) {
      MessagingException messagingException;
      if (exception instanceof MessagingException) {
        //Use same exception, but make sure whether a new error is needed
        messagingException = (MessagingException) exception;
        // TODO - MULE-10266 - Once we remove the usage of MessagingException from within the mule component we can get rid of the
        // messagingException.causedExactlyBy(..) condition.
        event = createErrorEvent(event, messageProcessor, messagingException, muleContext);
      } else {
        //Create a ME and an error, both using the exception
        messagingException = new MessagingException(event, getRootCauseException(exception), messageProcessor);
        messagingException.setProcessedEvent(createErrorEvent(event, messageProcessor, messagingException, muleContext));
      }

      if (messagingException.getFailingMessageProcessor() == null) {
        throw putContext(messagingException, messageProcessor, event, flowConstruct, muleContext);
      } else {
        throw
            putContext(messagingException, messagingException.getFailingMessageProcessor(), event, flowConstruct, muleContext);
      }
    } catch (Throwable ex) {
      throw
          putContext(new MessagingException(event, ex, messageProcessor), messageProcessor, event, flowConstruct, muleContext);
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
