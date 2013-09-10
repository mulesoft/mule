/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service;

import org.mule.api.MessagingException;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.Aggregator;
import org.mule.management.stats.RouterStatistics;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.routing.AbstractAggregator;
import org.mule.source.StartableCompositeMessageSource;

/**
 * Extension of {@link StartableCompositeMessageSource} which adds message processors between the composite
 * source and the target listener
 */
@Deprecated
public class ServiceAsyncReplyCompositeMessageSource extends ServiceCompositeMessageSource
{
    protected Long timeout;
    protected boolean failOnTimeout = true;

    public ServiceAsyncReplyCompositeMessageSource()
    {
        statistics = new RouterStatistics(RouterStatistics.TYPE_RESPONSE);
    }

    protected void createMessageProcessorChain() throws MuleException
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder(flowConstruct);
        builder.chain(processors);
        builder.chain(listener);
        listener = builder.build();
    }

    public Long getTimeout()
    {
        return timeout;
    }

    public void setTimeout(Long timeout)
    {
        this.timeout = timeout;
    }

    public boolean isFailOnTimeout()
    {
        return failOnTimeout;
    }

    public void setFailOnTimeout(boolean failOnTimeout)
    {
        this.failOnTimeout = failOnTimeout;
    }

    public void expireAggregation(String groupId) throws MessagingException
    {
        for (MessageProcessor processor : processors)
        {
            if (processor instanceof Aggregator)
            {
                ((AbstractAggregator) processor).expireAggregation(groupId);
            }
        }
    }

}
