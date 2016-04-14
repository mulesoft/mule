/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.processor;

import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.execution.MessageProcessorExecutionTemplate;
import org.mule.processor.BlockingProcessorExecutor;

import java.util.List;

/**
 * {@link org.mule.processor.BlockingProcessorExecutor} specifically for use with instances of {@link org.mule.api.service.Service}
 */
public class ServiceProcessorExecutor extends BlockingProcessorExecutor
{

    public ServiceProcessorExecutor(MuleEvent event, List<MessageProcessor> processors,
                                    MessageProcessorExecutionTemplate messageProcessorExecutionTemplate,
                                    boolean copyOnVoidEvent)
    {
        super(event, processors, messageProcessorExecutionTemplate, copyOnVoidEvent);
    }

    @Override
    protected MuleEvent executeNext() throws MessagingException
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
