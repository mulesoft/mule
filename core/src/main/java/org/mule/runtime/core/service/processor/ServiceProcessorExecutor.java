/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.service.processor;

import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.execution.MessageProcessorExecutionTemplate;
import org.mule.runtime.core.processor.BlockingProcessorExecutor;

import java.util.List;

/**
 * {@link org.mule.runtime.core.processor.BlockingProcessorExecutor} specifically for use with instances of
 * {@link org.mule.runtime.core.api.service.Service}
 */
public class ServiceProcessorExecutor extends BlockingProcessorExecutor {

  public ServiceProcessorExecutor(Event event, List<Processor> processors,
                                  MessageProcessorExecutionTemplate messageProcessorExecutionTemplate, boolean copyOnVoidEvent) {
    super(event, processors, messageProcessorExecutionTemplate, copyOnVoidEvent);
  }

  @Override
  protected Event executeNext() throws MessagingException {
    Event result = messageProcessorExecutionTemplate.execute(nextProcessor(), event);

    if (VoidMuleEvent.getInstance().equals(result) && copyOnVoidEvent) {
      return null;
    } else {
      return result;
    }
  }

}
