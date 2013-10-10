/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.processor.strategy;

import org.mule.api.MuleContext;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.processor.ProcessingStrategy;
import org.mule.config.ChainedThreadingProfile;
import org.mule.processor.AsyncInterceptingMessageProcessor;
import org.mule.util.concurrent.ThreadNameHelper;

import java.util.List;

import javax.resource.spi.work.WorkManager;

/**
 * This strategy uses a {@link WorkManager} to schedule the processing of the pipeline of message processors
 * in a single worker thread.
 */
public class AsynchronousProcessingStrategy implements ProcessingStrategy
{

    protected Integer maxThreads;
    protected Integer minThreads;
    protected Integer maxBufferSize;
    protected Long threadTTL;
    protected Long threadWaitTimeout;
    protected Integer poolExhaustedAction;
    protected ProcessingStrategy synchronousProcessingStrategy = new SynchronousProcessingStrategy();

    @Override
    public void configureProcessors(List<MessageProcessor> processors,
                                    StageNameSource nameSource,
                                    MessageProcessorChainBuilder chainBuilder,
                                    MuleContext muleContext)
    {
        if (processors.size() > 0)
        {
            chainBuilder.chain(createAsyncMessageProcessor(nameSource, muleContext));
            synchronousProcessingStrategy.configureProcessors(processors, nameSource, chainBuilder,
                muleContext);
        }
    }

    protected AsyncInterceptingMessageProcessor createAsyncMessageProcessor(StageNameSource nameSource,
                                                                            MuleContext muleContext)
    {
        return new AsyncInterceptingMessageProcessor(createThreadingProfile(muleContext), getThreadPoolName(
            nameSource.getName(), muleContext), muleContext.getConfiguration().getShutdownTimeout());
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
        if (poolExhaustedAction != null) threadingProfile.setPoolExhaustedAction(poolExhaustedAction);
        threadingProfile.setMuleContext(muleContext);
        return threadingProfile;
    }

    protected String getThreadPoolName(String stageName, MuleContext muleContext)
    {
        return ThreadNameHelper.flow(muleContext, stageName);
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

    public Integer getMaxBufferSize()
    {
        return maxBufferSize;
    }

    public Long getThreadTTL()
    {
        return threadTTL;
    }

    public Long getThreadWaitTimeout()
    {
        return threadWaitTimeout;
    }

    public Integer getPoolExhaustedAction()
    {
        return poolExhaustedAction;
    }

}
