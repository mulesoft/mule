/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor.strategy;

import org.mule.api.MuleContext;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.WorkManager;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.processor.ProcessingStrategy;
import org.mule.api.processor.StageNameSource;
import org.mule.config.ChainedThreadingProfile;
import org.mule.processor.AsyncInterceptingMessageProcessor;
import org.mule.util.concurrent.ThreadNameHelper;
import org.mule.work.MuleWorkManager;

import java.util.List;

/**
 * A abstract {@link org.mule.api.processor.ProcessingStrategy} implementation that provides a
 * {@link org.mule.api.config.ThreadingProfile} for you in extensions configured via setters for each of the threading
 * profile attributes.
 */
public abstract class AbstractThreadingProfileProcessingStrategy implements ProcessingStrategy
{

    protected Integer maxThreads;
    protected Integer minThreads;
    protected Integer maxBufferSize;
    protected Long threadTTL;
    protected Long threadWaitTimeout;
    protected Integer poolExhaustedAction;

    protected ThreadingProfile createThreadingProfile(MuleContext muleContext)
    {
        ThreadingProfile threadingProfile = new ChainedThreadingProfile(muleContext.getDefaultThreadingProfile());
        if (maxThreads != null)
        {
            threadingProfile.setMaxThreadsActive(maxThreads);
        }
        if (minThreads != null)
        {
            threadingProfile.setMaxThreadsIdle(minThreads);
        }
        if (maxBufferSize != null)
        {
            threadingProfile.setMaxBufferSize(maxBufferSize);
        }
        if (threadTTL != null)
        {
            threadingProfile.setThreadTTL(threadTTL);
        }
        if (threadWaitTimeout != null)
        {
            threadingProfile.setThreadWaitTimeout(threadWaitTimeout);
        }
        if (poolExhaustedAction != null)
        {
            threadingProfile.setPoolExhaustedAction(poolExhaustedAction);
        }
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
