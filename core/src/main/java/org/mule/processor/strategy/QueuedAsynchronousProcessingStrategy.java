/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor.strategy;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.store.QueueStore;
import org.mule.config.QueueProfile;
import org.mule.management.stats.QueueStatistics;
import org.mule.management.stats.QueueStatisticsAware;
import org.mule.processor.AsyncInterceptingMessageProcessor;
import org.mule.processor.SedaStageInterceptingMessageProcessor;
import org.mule.util.concurrent.ThreadNameHelper;
import org.mule.util.queue.QueueManager;

import java.io.Serializable;

import javax.resource.spi.work.WorkManager;

/**
 * This strategy uses a {@link QueueManager} to decouple receipt and processing of messages. The queue is
 * polled and a {@link WorkManager} is used to schedule processing of the pipeline of message processors in a
 * single worker thread.
 */
public class QueuedAsynchronousProcessingStrategy extends AsynchronousProcessingStrategy
    implements QueueStatisticsAware
{

    protected Integer queueTimeout;
    protected Integer maxQueueSize = 0;
    protected QueueStore<Serializable> queueStore = null;
    protected QueueStatistics queueStatistics;

    @Override
    protected AsyncInterceptingMessageProcessor createAsyncMessageProcessor(org.mule.api.processor.StageNameSource nameSource,
                                                                            MuleContext muleContext)
    {
        Integer timeout = queueTimeout != null ? queueTimeout : muleContext.getConfiguration()
            .getDefaultQueueTimeout();

        initQueueStore(muleContext);

        QueueProfile queueProfile = new QueueProfile(maxQueueSize, queueStore);
        ThreadingProfile threadingProfile = createThreadingProfile(muleContext);
        String stageName = nameSource.getName();
        return new SedaStageInterceptingMessageProcessor(ThreadNameHelper.flow(muleContext, stageName),
            stageName, queueProfile, timeout, threadingProfile, queueStatistics, muleContext);
    }

    protected void initQueueStore(MuleContext muleContext)
    {
        if (queueStore == null)
        {
            queueStore = muleContext.getRegistry().lookupObject(MuleProperties.QUEUE_STORE_DEFAULT_IN_MEMORY_NAME);
        }
    }

    public Integer getQueueTimeout()
    {
        return queueTimeout;
    }

    public void setQueueTimeout(Integer queueTimeout)
    {
        this.queueTimeout = queueTimeout;
    }

    public Integer getMaxQueueSize()
    {
        return maxQueueSize;
    }

    public void setMaxQueueSize(Integer maxQueueSize)
    {
        this.maxQueueSize = maxQueueSize;
    }

    public QueueStore<Serializable> getQueueStore()
    {
        return queueStore;
    }

    public void setQueueStore(QueueStore<Serializable> queueStore)
    {
        this.queueStore = queueStore;
    }

    public QueueStatistics getQueueStatistics()
    {
        return queueStatistics;
    }

    @Override
    public void setQueueStatistics(QueueStatistics queueStatistics)
    {
        this.queueStatistics = queueStatistics;
    }
}
