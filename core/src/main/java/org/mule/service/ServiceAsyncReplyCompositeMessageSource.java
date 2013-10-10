/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
