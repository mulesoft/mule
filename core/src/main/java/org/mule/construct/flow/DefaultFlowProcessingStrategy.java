/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    protected AsyncInterceptingMessageProcessor createAsyncMessageProcessor(StageNameSource nameSource,
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
