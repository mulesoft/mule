/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.execution.ExceptionContextProvider;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.exception.ErrorMessageAwareException;
import org.mule.runtime.core.exception.ErrorTypeLocator;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.exception.WrapperErrorMessageAwareException;
import org.mule.runtime.core.message.ErrorBuilder;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import java.util.Map.Entry;
import java.util.Optional;

/**
 * Replace any exception thrown with a MessagingException
 */
public class ExceptionToMessagingExceptionExecutionInterceptor implements MessageProcessorExecutionInterceptor {

  private MuleContext muleContext;
  private FlowConstruct flowConstruct;
  private ErrorTypeLocator errorTypeLocator;

  @Override
  public Event execute(Processor messageProcessor, Event event) throws MessagingException {
    try {
      return messageProcessor.process(event);
    } catch (Exception exception) {
      MessagingException messagingException;
      if (exception instanceof MessagingException) {
        //Use same exception, but make sure whether a new error is needed
        messagingException = (MessagingException) exception;
        Throwable causeException = exception.getCause() != null ? exception.getCause() : exception;
        Optional<Error> error = messagingException.getEvent().getError();
        // TODO - MULE-10266 - Once we remove the usage of MessagingException from within the mule component we can get rid of the
        // messagingException.causedExactlyBy(..) condition.
        if (!error.isPresent() || !error.get().getCause().equals(causeException)
            || !messagingException.causedExactlyBy(error.get().getCause().getClass())) {
          ErrorType errorType = getErrorTypeFromFailingProcessor(messageProcessor, causeException);
          event = Event.builder(messagingException.getEvent())
              .error(ErrorBuilder.builder(causeException).errorType(errorType).build()).build();
          messagingException.setProcessedEvent(event);
        }
      } else {
        //Create a ME and an error, both using the exception
        Throwable causeException = exception instanceof ErrorMessageAwareException
            ? ((ErrorMessageAwareException) exception).getRootCause()
            : exception;
        messagingException = new MessagingException(event, causeException, messageProcessor);
        ErrorType errorType = getErrorTypeFromFailingProcessor(messageProcessor, causeException);
        Event exceptionEvent = messagingException.getEvent();
        event = Event.builder(exceptionEvent).error(ErrorBuilder.builder(exception).errorType(errorType).build()).build();
        messagingException.setProcessedEvent(event);
      }

      if (messagingException.getFailingMessageProcessor() == null) {
        throw putContext(messagingException, messageProcessor, event, flowConstruct, muleContext);
      } else {
        throw putContext(messagingException, messagingException.getFailingMessageProcessor(), event, flowConstruct, muleContext);
      }
    } catch (Throwable ex) {
      throw putContext(new MessagingException(event, ex, messageProcessor), messageProcessor, event, flowConstruct, muleContext);
    }
  }

  private ErrorType getErrorTypeFromFailingProcessor(Processor messageProcessor, Throwable exception) {
    ErrorType errorType;
    Throwable causeException =
        exception instanceof WrapperErrorMessageAwareException ? ((WrapperErrorMessageAwareException) exception).getRootCause()
            : exception;
    ComponentIdentifier componentIdentifier = null;
    if (AnnotatedObject.class.isAssignableFrom(messageProcessor.getClass())) {
      componentIdentifier =
          (ComponentIdentifier) ((AnnotatedObject) messageProcessor).getAnnotation(ComponentIdentifier.ANNOTATION_NAME);
    }
    if (componentIdentifier != null) {
      errorType = errorTypeLocator.lookupComponentErrorType(componentIdentifier, causeException);
    } else {
      errorType = errorTypeLocator.lookupErrorType(causeException);
    }
    return errorType;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
    this.errorTypeLocator = context.getErrorTypeLocator();
  }

  public static MessagingException putContext(MessagingException messagingException, Processor failingMessageProcessor,
                                              Event event, FlowConstruct flowConstruct, MuleContext muleContext) {
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
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }
}
