/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.construct.flow;

import org.mule.api.MuleContext;
import org.mule.api.config.ThreadingProfile;
import org.mule.config.QueueProfile;
import org.mule.processor.AsyncInterceptingMessageProcessor;
import org.mule.processor.LaxSedaStageInterceptingMessageProcessor;
import org.mule.processor.strategy.QueuedAsynchronousProcessingStrategy;

/**
 * This processing strategy uses the 'queued-asynchronous' strategy where possible, but if an event is
 * synchronous it processes it synchronously rather than failing.
 */
public class DefaultFlowProcessingStrategy extends QueuedAsynchronousProcessingStrategy
{

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
        return new LaxSedaStageInterceptingMessageProcessor(getThreadPoolName(stageName, muleContext),
            stageName, queueProfile, timeout, threadingProfile, queueStatistics, muleContext);
    }

}
