/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor.chain;

import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.ProcessorExecutor;
import org.mule.api.service.Service;
import org.mule.execution.MessageProcessorExecutionTemplate;
import org.mule.processor.BlockingProcessorExecutor;
import org.mule.processor.NonBlockingProcessorExecutor;
import org.mule.service.processor.ServiceProcessorExecutor;

import java.util.List;

/**
 * Creates an appropriate instance of {@link org.mule.processor.BlockingProcessorExecutor} based on the current
 * {@link org.mule.api.MuleEvent} and {@link org.mule.api.construct.FlowConstruct}.
 */
public class ProcessorExecutorFactory
{

    public ProcessorExecutor createProcessorExecutor(MuleEvent event,
                                                     List<MessageProcessor> processors,
                                                     MessageProcessorExecutionTemplate executionTemplate,
                                                     boolean copyOnVoidEvent)
    {
        if (event.isAllowNonBlocking() && event.getReplyToHandler() != null)
        {
            return new NonBlockingProcessorExecutor(event, processors, executionTemplate, copyOnVoidEvent);
        }
        else if (event.getFlowConstruct() instanceof Service)
        {
            return new ServiceProcessorExecutor(event, processors, executionTemplate, copyOnVoidEvent);
        }
        else
        {
            return new BlockingProcessorExecutor(event, processors, executionTemplate, copyOnVoidEvent);
        }
    }

}
