/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.interceptor;

import static org.mule.runtime.core.message.DefaultEventBuilder.EventImplementation.setCurrentEvent;

import org.mule.runtime.core.NonBlockingVoidMuleEvent;
import org.mule.runtime.core.api.CoreEventContext;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.connector.NonBlockingReplyToHandler;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.interceptor.Interceptor;
import org.mule.runtime.core.management.stats.ProcessingTime;
import org.mule.runtime.core.processor.AbstractRequestResponseMessageProcessor;

/**
 * <code>EnvelopeInterceptor</code> is an intercepter that will fire before and after an event is received.
 */
public abstract class AbstractEnvelopeInterceptor extends AbstractRequestResponseMessageProcessor
    implements Interceptor {

  /**
   * This method is invoked before the event is processed
   */
  public abstract Event before(Event event) throws MuleException;

  /**
   * This method is invoked after the event has been processed, unless an exception was thrown
   */
  public abstract Event after(Event event) throws MuleException;

  /**
   * This method is always invoked after the event has been processed,
   */
  public abstract Event last(Event event, ProcessingTime time, long startTime, boolean exceptionWasThrown)
      throws MuleException;

  @Override
  protected Event processBlocking(Event event) throws MuleException {
    long startTime = System.currentTimeMillis();
    ProcessingTime time = ((CoreEventContext) event.getContext()).getProcessingTime();
    boolean exceptionWasThrown = true;
    Event resultEvent = event;
    try {
      resultEvent = after(processNext(before(resultEvent)));
      exceptionWasThrown = false;
    } finally {
      resultEvent = last(resultEvent, time, startTime, exceptionWasThrown);
    }
    return resultEvent;
  }

  @Override
  protected Event processNonBlocking(final Event event) throws MuleException {
    final long startTime = System.currentTimeMillis();
    final ProcessingTime time = ((CoreEventContext) event.getContext()).getProcessingTime();
    Event responseEvent = event;

    final ReplyToHandler originalReplyToHandler = event.getReplyToHandler();
    responseEvent =
        Event.builder(event).replyToHandler(new ResponseReplyToHandler(originalReplyToHandler, time, startTime)).build();
    // Update RequestContext ThreadLocal for backwards compatibility
    setCurrentEvent(responseEvent);

    try {
      responseEvent = processNext(processRequest(responseEvent));
      if (!(responseEvent instanceof NonBlockingVoidMuleEvent)) {
        responseEvent = processResponse(responseEvent, event);
      }
    } catch (Exception exception) {
      last(responseEvent, time, startTime, true);
      throw exception;
    }
    return responseEvent;
  }

  class ResponseReplyToHandler implements NonBlockingReplyToHandler {

    private final ReplyToHandler originalReplyToHandler;
    private final ProcessingTime time;
    private final long startTime;

    public ResponseReplyToHandler(ReplyToHandler originalReplyToHandler, ProcessingTime time, long startTime) {
      this.originalReplyToHandler = originalReplyToHandler;
      this.time = time;
      this.startTime = startTime;
    }

    @Override
    public Event processReplyTo(final Event event, InternalMessage returnMessage, Object replyTo) throws MuleException {
      Event response = event;
      boolean exceptionWasThrown = true;
      try {
        response = after(event);
        response = originalReplyToHandler.processReplyTo(response, null, replyTo);
        exceptionWasThrown = false;
      } finally {
        return last(response, time, startTime, false);
      }
    }

    @Override
    public void processExceptionReplyTo(MessagingException exception, Object replyTo) {
      try {
        originalReplyToHandler.processExceptionReplyTo(exception, replyTo);
      } finally {
        try {
          last(exception.getEvent(), time, startTime, true);
        } catch (MuleException muleException) {
          throw new MuleRuntimeException(muleException);
        }
      }
    }
  }
}
