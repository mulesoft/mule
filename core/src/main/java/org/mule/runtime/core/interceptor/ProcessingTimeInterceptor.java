/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.interceptor;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.management.stats.ProcessingTime;

/**
 * Calculate and record the processing time for a message processing chain
 */
public class ProcessingTimeInterceptor extends AbstractEnvelopeInterceptor {

  public ProcessingTimeInterceptor() {
    super();
  }

  public ProcessingTimeInterceptor(MessageProcessor next, FlowConstruct fc) {
    setListener(next);
    setFlowConstruct(fc);
  }

  @Override
  public MuleEvent before(MuleEvent event) throws MuleException {
    return event;
  }

  @Override
  public MuleEvent after(MuleEvent event) throws MuleException {
    return event;
  }


  @Override
  public MuleEvent last(MuleEvent event, ProcessingTime time, long startTime, boolean exceptionWasThrown) throws MuleException {
    if (time != null) {
      time.addFlowExecutionBranchTime(startTime);
    }
    return event;
  }
}
