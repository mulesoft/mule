/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.mule.runtime.core.api.Event.setCurrentEvent;

import org.mule.runtime.api.execution.CompletionHandler;
import org.mule.runtime.api.execution.ExceptionCallback;
import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.NonBlockingVoidMuleEvent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.exception.MessagingException;

/**
 * Abstract implementation of {@link org.mule.runtime.core.processor.NonBlockingMessageProcessor} that determines if processing
 * should be performed blocking or non-blocking..
 */
public abstract class AbstractNonBlockingMessageProcessor extends AbstractAnnotatedObject
    implements NonBlockingMessageProcessor, MessagingExceptionHandlerAware {

  private MessagingExceptionHandler messagingExceptionHandler;

  @Override
  public Event process(Event event) throws MuleException {
    if (isNonBlocking(event)) {
      processNonBlocking(event, createNonBlockingCompletionHandler(event));
      // Update RequestContext ThreadLocal for backwards compatibility. Clear event as we are done with this
      // thread.
      setCurrentEvent(null);
      return NonBlockingVoidMuleEvent.getInstance();
    } else {
      return processBlocking(event);
    }
  }

  protected boolean isNonBlocking(Event event) {
    return event.isAllowNonBlocking() && event.getReplyToHandler() != null;
  }

  @Override
  public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler) {
    this.messagingExceptionHandler = messagingExceptionHandler;
  }

  abstract protected void processNonBlocking(Event event, CompletionHandler completionHandler) throws MuleException;

  abstract protected Event processBlocking(Event event) throws MuleException;

  protected ExceptionCallback<Void, ? extends MessagingException> createCompletionExceptionCallback(Event event) {
    return (ExceptionCallback<Void, MessagingException>) exception -> {
      messagingExceptionHandler.handleException(exception, event);
      return null;
    };
  }

  private NonBlockingCompletionHandler createNonBlockingCompletionHandler(Event event) {
    return new NonBlockingCompletionHandler(event);
  }

  class NonBlockingCompletionHandler implements CompletionHandler<Event, MessagingException, Void> {

    final private ReplyToHandler replyToHandler;

    NonBlockingCompletionHandler(Event event) {
      this.replyToHandler = event.getReplyToHandler();
    }

    @Override
    public void onFailure(final MessagingException exception) {
      replyToHandler.processExceptionReplyTo(exception, null);
    }

    @Override
    public void onCompletion(Event result, ExceptionCallback<Void, Exception> exceptionCallback) {
      try {
        replyToHandler.processReplyTo(result, null, null);
      } catch (Exception e) {
        exceptionCallback.onException(e);
      }
    }
  }
}
