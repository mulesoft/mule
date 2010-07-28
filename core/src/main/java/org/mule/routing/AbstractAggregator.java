/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.Aggregator;
import org.mule.api.service.Service;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.routing.correlation.EventCorrelator;
import org.mule.routing.correlation.EventCorrelatorCallback;

import javax.resource.spi.work.WorkException;

/**
 * <code>AbstractEventAggregator</code> will aggregate a set of messages into a
 * single message.
 */

public abstract class AbstractAggregator extends AbstractInterceptingMessageProcessor
    implements Initialisable, MuleContextAware, FlowConstructAware, Aggregator
{

    protected EventCorrelator eventCorrelator;
    protected MuleContext muleContext;
    protected FlowConstruct flowConstruct;

    private long timeout = 0;
    private boolean failOnTimeout = true;

    public void initialise() throws InitialisationException
    {
        eventCorrelator = new EventCorrelator(getCorrelatorCallback(muleContext), next,
            flowConstruct.getMessageInfoMapping(), muleContext);

        // Inherit failOnTimeout from async-reply if this aggregator is being used for async-reply
        if (flowConstruct instanceof Service)
        {
            Service service = (Service) flowConstruct;
            if (service.getAsyncReplyMessageSource().getMessageProcessors().contains(this))
            {
                failOnTimeout = service.getAsyncReplyMessageSource().isFailOnTimeout();
            }
        }
        
        eventCorrelator.setTimeout(timeout);
        eventCorrelator.setFailOnTimeout(isFailOnTimeout());
        if (timeout != 0)
        {
            try
            {
                eventCorrelator.enableTimeoutMonitor();
            }
            catch (WorkException e)
            {
                throw new InitialisationException(e, this);
            }
        }
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    protected abstract EventCorrelatorCallback getCorrelatorCallback(MuleContext muleContext);

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleEvent result = eventCorrelator.process(event);
        if (result == null)
        {
            return null;
        }
        return processNext(result);
    }

    public void expireAggregation(String groupId)
    {
        eventCorrelator.forceGroupExpiry(groupId);
    }

    public long getTimeout()
    {
        return timeout;
    }

    public void setTimeout(long timeout)
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

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }
}
