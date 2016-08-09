/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.interceptor;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.management.stats.ProcessingTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>LoggingInterceptor</code> is a simple interceptor that logs a message before and after the event processing.
 */
public class LoggingInterceptor extends AbstractEnvelopeInterceptor {

  /**
   * logger used by this class
   */
  private static Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

  @Override
  public MuleEvent before(MuleEvent event) {
    if (logger.isDebugEnabled()) {
      logger.debug("Started event processing for " + event.getFlowConstruct().getName());
    }
    return event;

  }

  @Override
  public MuleEvent after(MuleEvent event) {
    if (logger.isDebugEnabled() && event != null) {
      logger.debug("Finished event processing for " + event.getFlowConstruct().getName());
    }
    return event;
  }

  @Override
  public MuleEvent last(MuleEvent event, ProcessingTime time, long startTime, boolean exceptionWasThrown) throws MuleException {
    return event;
  }
}
