/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.connector.NonBlockingReplyToHandler;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;

/**
 * {@link org.mule.runtime.core.api.connector.ReplyToHandler} implementation that uses a
 * {@link org.mule.runtime.core.api.exception .MessagingExceptionHandler} to handle errors before delegating to the delegate
 * ReplyToHandler instance.
 * <p/>
 * Invocations of {@link #processReplyTo(org.mule.runtime.core.api.MuleEvent, org.mule.runtime.core.api.MuleMessage, Object)} are
 * passed straight through to the delegate ReplyToHandler where as invocations of
 * {@link org.mule.runtime.core.api.connector.ReplyToHandler#processExceptionReplyTo(org.mule.runtime.core.api.MessagingException, Object)}
 * may result in a delegation to either
 * {@link #processReplyTo(org.mule.runtime.core.api.MuleEvent, org.mule.runtime.core.api.MuleMessage, Object)} or
 * {@link org.mule.runtime.core.api.connector.ReplyToHandler#processExceptionReplyTo(org.mule.runtime.core.api.MessagingException, Object)}
 * depending on the result of {@link org.mule.runtime.core.api.MessagingException#handled()} after the MessagingExceptionHandler
 * has been invoked.
 */
public class ExceptionHandlingReplyToHandlerDecorator implements NonBlockingReplyToHandler {

  private final MessagingExceptionHandler messagingExceptionHandler;
  private final ReplyToHandler delegate;

  public ExceptionHandlingReplyToHandlerDecorator(ReplyToHandler replyToHandler, MessagingExceptionHandler exceptionHandler) {
    this.delegate = replyToHandler;
    this.messagingExceptionHandler = exceptionHandler;
  }

  @Override
  public void processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) throws MuleException {
    delegate.processReplyTo(event, returnMessage, replyTo);
  }

  @Override
  public void processExceptionReplyTo(MessagingException exception, Object replyTo) {
    MuleEvent result;
    if (messagingExceptionHandler != null) {
      result = messagingExceptionHandler.handleException(exception, exception.getEvent());
    } else {
      result = exception.getEvent().getFlowConstruct().getExceptionListener().handleException(exception, exception.getEvent());
    }
    exception.setProcessedEvent(result);
    if (!exception.handled()) {
      delegate.processExceptionReplyTo(exception, replyTo);
    } else {
      try {
        delegate.processReplyTo(exception.getEvent(), exception.getEvent().getMessage(), replyTo);
      } catch (MuleException e) {
        delegate.processExceptionReplyTo(new MessagingException(exception.getEvent(), e), replyTo);
      }
    }
  }
}
