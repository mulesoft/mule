/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor.chain;

import org.mule.OptimizedRequestContext;
import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.execution.MessageProcessorExecutionTemplate;

import java.util.List;

/**
 * ProcessorIterator specifically for use with instances of {@link org.mule.api.service.Service}
 */
public class ServiceProcessorIterator extends ProcessorIterator
{

    public ServiceProcessorIterator(MuleEvent event,List<MessageProcessor> processors,
                                    MessageProcessorExecutionTemplate messageProcessorExecutionTemplate, boolean copyOnVoidEvent)
    {
        super(event, processors, messageProcessorExecutionTemplate, copyOnVoidEvent);
    }

    public MuleEvent processNext() throws MessagingException
    {
        MuleEvent result = messageProcessorExecutionTemplate.execute(nextProcessor(), event);

        if (VoidMuleEvent.getInstance().equals(result) && copyOnVoidEvent)
        {
            return null;
        }
        else
        {
            return result;
        }
    }

}
