/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor.strategy;

import org.mule.api.MuleContext;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.WorkManager;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.work.MuleWorkManager;

import java.util.List;

/**
 * Allows Mule to use non-blocking execution model where possible and free up threads when performing IO
 * operations.
 *
 * @since 3.7
 */
public class NonBlockingProcessingStrategy extends AbstractThreadingProfileProcessingStrategy
{

    private static final int DEFAULT_MAX_THREADS = 128;

    public NonBlockingProcessingStrategy()
    {
        maxThreads = DEFAULT_MAX_THREADS;
    }

    @Override
    public void configureProcessors(List<MessageProcessor> processors, org.mule.api.processor.StageNameSource
            nameSource, MessageProcessorChainBuilder chainBuilder, MuleContext muleContext)
    {
        for (MessageProcessor processor : processors)
        {
            chainBuilder.chain((MessageProcessor) processor);
        }
    }

    public WorkManager createWorkManager(FlowConstruct flowConstruct)
    {
        MuleContext muleContext = flowConstruct.getMuleContext();
        MuleWorkManager workManager = (MuleWorkManager) createThreadingProfile(muleContext).createWorkManager
                (getThreadPoolName(flowConstruct.getName(), muleContext), muleContext.getConfiguration()
                        .getShutdownTimeout());
        return workManager;
    }

}
