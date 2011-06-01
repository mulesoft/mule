/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct;

import org.mule.api.MuleContext;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.construct.Pipeline;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.config.ChainedThreadingProfile;
import org.mule.processor.AsyncInterceptingMessageProcessor;
import org.mule.processor.OptionalAsyncInterceptingMessageProcessor;
import org.mule.util.concurrent.ThreadNameHelper;

import javax.resource.spi.work.WorkManager;

/**
 * This strategy uses a {@link WorkManager} to schedule the processing of the pipeline of message processors
 * in a single worker thread.
 */
public class AsynchronousProcessingStrategy extends SynchronousProcessingStrategy
{

    protected Integer maxThreads;
    protected Integer minThreads;
    protected Integer maxBufferSize;
    protected Long threadTTL;
    protected Long threadWaitTimeout;
    protected Integer poolExhaustedAction;

    @Override
    public void configureProcessors(Pipeline pipeline, MessageProcessorChainBuilder chainBuilder)
    {
        if (pipeline.getMessageProcessors().size() > 0)
        {
            chainBuilder.chain(createAsyncMessageProcessor(pipeline));
            super.configureProcessors(pipeline, chainBuilder);
        }
    }

    protected AsyncInterceptingMessageProcessor createAsyncMessageProcessor(Pipeline pipeline)
    {
        MuleContext muleContext = pipeline.getMuleContext();
        return new OptionalAsyncInterceptingMessageProcessor(createThreadingProfile(muleContext),
            ThreadNameHelper.flow(muleContext, pipeline.getName()), muleContext.getConfiguration()
                .getShutdownTimeout());
    }

    protected ThreadingProfile createThreadingProfile(MuleContext muleContext)
    {
        ThreadingProfile threadingProfile = new ChainedThreadingProfile(
            muleContext.getDefaultThreadingProfile());
        if (maxThreads != null) threadingProfile.setMaxThreadsActive(maxThreads);
        if (minThreads != null) threadingProfile.setMaxThreadsIdle(minThreads);
        if (maxBufferSize != null) threadingProfile.setMaxBufferSize(maxBufferSize);
        if (threadTTL != null) threadingProfile.setThreadTTL(threadTTL);
        if (threadWaitTimeout != null) threadingProfile.setThreadWaitTimeout(threadWaitTimeout);
        if (poolExhaustedAction != null) threadingProfile.setMaxThreadsActive(poolExhaustedAction);
        threadingProfile.setMuleContext(muleContext);
        return threadingProfile;
    }

    public Integer getMaxThreads()
    {
        return maxThreads;
    }

    public void setMaxThreads(Integer maxThreads)
    {
        this.maxThreads = maxThreads;
    }

    public Integer getMinThreads()
    {
        return minThreads;
    }

    public void setMinThreads(Integer minThreads)
    {
        this.minThreads = minThreads;
    }

    public void setMaxBufferSize(Integer maxBufferSize)
    {
        this.maxBufferSize = maxBufferSize;
    }

    public void setThreadTTL(Long threadTTL)
    {
        this.threadTTL = threadTTL;
    }

    public void setThreadWaitTimeout(Long threadWaitTimeout)
    {
        this.threadWaitTimeout = threadWaitTimeout;
    }

    public void setPoolExhaustedAction(Integer poolExhaustedAction)
    {
        this.poolExhaustedAction = poolExhaustedAction;
    }

}
