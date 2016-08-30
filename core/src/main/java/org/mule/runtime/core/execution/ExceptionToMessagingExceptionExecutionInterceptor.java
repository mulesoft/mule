/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.execution.ExceptionContextProvider;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.ComponentIdentifier;
import org.mule.runtime.core.exception.ErrorTypeLocator;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.message.ErrorBuilder;

import java.util.Map.Entry;

/**
 * Replace any exception thrown with a MessagingException
 */
public class ExceptionToMessagingExceptionExecutionInterceptor implements MessageProcessorExecutionInterceptor {

  private MuleContext muleContext;
  private FlowConstruct flowConstruct;
  private ErrorTypeLocator errorTypeLocator;

  @Override
  public MuleEvent execute(MessageProcessor messageProcessor, MuleEvent event) throws MessagingException {
    try {
      return messageProcessor.process(event);
    } catch (Exception exception) {
      MessagingException messagingException;
      if (!(exception instanceof MessagingException) || ((MessagingException) exception).getEvent().getError() == null) {
        ErrorType errorType;
        errorType = getErrorTypeFromFailingProcessor(messageProcessor, exception);
        if (exception instanceof MessagingException) {
          messagingException = (MessagingException) exception;
        } else {
          messagingException = new MessagingException(event, exception, messageProcessor);
        }
        // TODO these break some tests and rely on the event mutability, check with PLG
        // event = MuleEvent.builder(event).error(ErrorBuilder.builder(exception).errorType(errorType).build()).build();
        event.setError(ErrorBuilder.builder(exception).errorType(errorType).build());
      } else {
        messagingException = (MessagingException) exception;
        //TODO - MULE-10266 - Once we remove the usage of MessagingException from within the mule component we can get rid of the messagingException.causedExactlyBy(..) condition.
        if (event.getError() == null || !(event.getError().getException().equals(exception)
            || messagingException.causedExactlyBy(event.getError().getException().getClass()))) {
          ErrorType errorType = getErrorTypeFromFailingProcessor(messageProcessor, exception);
          // TODO these break some tests and rely on the event mutability, check with PLG
          // event = MuleEvent.builder(event).error(ErrorBuilder.builder(exception).errorType(errorType).build()).build();
          event.setError(ErrorBuilder.builder(exception).errorType(errorType).build());
        }
      }

      if (messagingException.getFailingMessageProcessor() == null) {
        throw putContext(messagingException, messageProcessor, event);
      } else {
        throw putContext(messagingException, messagingException.getFailingMessageProcessor(), event);
      }
    } catch (Throwable ex) {
      throw putContext(new MessagingException(event, ex, messageProcessor), messageProcessor, event);
    }
  }

  private ErrorType getErrorTypeFromFailingProcessor(MessageProcessor messageProcessor, Exception exception) {
    ErrorType errorType;
    if (AnnotatedObject.class.isAssignableFrom(messageProcessor.getClass())) {
      ComponentIdentifier componentIdentifier =
          (ComponentIdentifier) ((AnnotatedObject) messageProcessor).getAnnotation(ComponentIdentifier.ANNOTATION_NAME);
      errorType = errorTypeLocator.lookupComponentErrorType(componentIdentifier, exception);
    } else {
      errorType = errorTypeLocator.lookupErrorType(exception);
    }
    return errorType;
  }

  private MessagingException putContext(MessagingException messagingException, MessageProcessor failingMessageProcessor,
                                        MuleEvent event) {
    for (ExceptionContextProvider exceptionContextProvider : muleContext.getExceptionContextProviders()) {
      for (Entry<String, Object> contextInfoEntry : exceptionContextProvider
          .getContextInfo(event, failingMessageProcessor, flowConstruct).entrySet()) {
        if (!messagingException.getInfo().containsKey(contextInfoEntry.getKey())) {
          messagingException.getInfo().put(contextInfoEntry.getKey(), contextInfoEntry.getValue());
        }
      }
    }
    return messagingException;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
    this.errorTypeLocator = context.getErrorTypeLocator();
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }
}
