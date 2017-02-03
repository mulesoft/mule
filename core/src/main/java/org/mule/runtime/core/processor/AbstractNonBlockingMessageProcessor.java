/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import org.mule.runtime.core.execution.CompletionHandler;
import org.mule.runtime.core.execution.ExceptionCallback;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.api.processor.NonBlockingMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;

/**
 * Abstract implementation of {@link Processor} that determines if processing
 * should be performed blocking or non-blocking..
 */
public abstract class AbstractNonBlockingMessageProcessor extends AbstractAnnotatedObject
    implements NonBlockingMessageProcessor, MessagingExceptionHandlerAware {

  private MessagingExceptionHandler messagingExceptionHandler;

  @Override
  public Event process(Event event) throws MuleException {
    return processBlocking(event);
  }

  @Override
  public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler) {
    this.messagingExceptionHandler = messagingExceptionHandler;
  }

  abstract protected void processNonBlocking(Event event, CompletionHandler completionHandler) throws MuleException;

  abstract protected Event processBlocking(Event event) throws MuleException;

  protected ExceptionCallback<? extends MessagingException> createCompletionExceptionCallback(Event event) {
    return (ExceptionCallback<MessagingException>) exception -> messagingExceptionHandler.handleException(exception, event);
  }

}
