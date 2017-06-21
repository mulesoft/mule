/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.interceptor;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.interceptor.Interceptor;
import org.mule.runtime.core.api.management.stats.ProcessingTime;
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
  public Event process(Event event) throws MuleException {
    long startTime = System.currentTimeMillis();
    ProcessingTime time = event.getContext().getProcessingTime();
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

}
