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
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>TimerInterceptor</code> simply times and displays the time taken to process an event.
 */
public class TimerInterceptor extends AbstractInterceptingMessageProcessor implements Interceptor {

  /**
   * logger used by this class
   */
  private static Logger logger = LoggerFactory.getLogger(TimerInterceptor.class);

  @Override
  public Event process(Event event) throws MuleException {
    long startTime = System.currentTimeMillis();

    Event resultEvent = processNext(event);

    if (logger.isInfoEnabled()) {
      long executionTime = System.currentTimeMillis() - startTime;
      logger.info(flowConstruct.getName() + " took " + executionTime + "ms to process event [" + resultEvent + "]");
    }

    return resultEvent;
  }
}
