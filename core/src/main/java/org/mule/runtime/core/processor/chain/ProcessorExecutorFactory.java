/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.chain;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.ProcessorExecutor;
import org.mule.runtime.core.execution.MessageProcessorExecutionTemplate;
import org.mule.runtime.core.processor.BlockingProcessorExecutor;
import org.mule.runtime.core.processor.NonBlockingProcessorExecutor;

import java.util.List;

/**
 * Creates an appropriate instance of {@link org.mule.runtime.core.processor.BlockingProcessorExecutor} based on the current
 * {@link org.mule.runtime.core.api.MuleEvent} and {@link org.mule.runtime.core.api.construct.FlowConstruct}.
 */
public class ProcessorExecutorFactory {

  public ProcessorExecutor createProcessorExecutor(MuleEvent event, List<MessageProcessor> processors,
                                                   MessageProcessorExecutionTemplate executionTemplate, boolean copyOnVoidEvent) {
    if (event.isAllowNonBlocking()) {
      return new NonBlockingProcessorExecutor(event, processors, executionTemplate, copyOnVoidEvent);
    } else {
      return new BlockingProcessorExecutor(event, processors, executionTemplate, copyOnVoidEvent);
    }
  }

}
