/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.interceptor;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.processor.InternalMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.management.stats.ProcessingTime;

/**
 * Calculate and record the processing time for a message processing chain
 */
public class ProcessingTimeInterceptor extends AbstractEnvelopeInterceptor implements InternalMessageProcessor {

  public ProcessingTimeInterceptor() {
    super();
  }

  public ProcessingTimeInterceptor(Processor next) {
    setListener(next);
  }

  @Override
  public Event before(Event event) throws MuleException {
    return event;
  }

  @Override
  public Event after(Event event) throws MuleException {
    return event;
  }


  @Override
  public Event last(Event event, ProcessingTime time, long startTime, boolean exceptionWasThrown) throws MuleException {
    if (time != null) {
      time.addFlowExecutionBranchTime(startTime);
    }
    return event;
  }
}
