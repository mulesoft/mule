/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.routing.Aggregator;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.api.service.Service;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.routing.correlation.EventCorrelator;
import org.mule.routing.correlation.EventCorrelatorCallback;
import org.mule.util.concurrent.ThreadNameHelper;

/**
 * <code>AbstractEventAggregator</code> will aggregate a set of messages into a
 * single message. <b>EIP Reference:</b> <a
 * href="http://www.eaipatterns.com/Aggregator.html"
 * >http://www.eaipatterns.com/Aggregator.html</a>
 */

public abstract class AbstractAggregator extends AbstractInterceptingMessageProcessor
    implements Initialisable, MuleContextAware, FlowConstructAware, Aggregator, Startable, Stoppable, Disposable
{

    protected EventCorrelator eventCorrelator;
    protected MuleContext muleContext;
    protected FlowConstruct flowConstruct;
    protected MessageInfoMapping messageInfoMapping;

    private long timeout = 0;
    private boolean failOnTimeout = true;
    protected boolean persistentStores;
    protected String storePrefix = null;

    public void initialise() throws InitialisationException
    {
        if (messageInfoMapping == null)
        {
            messageInfoMapping = flowConstruct.getMessageInfoMapping();
        }
        if (storePrefix == null)
        {
            storePrefix = String.format("%s%s.%s.", ThreadNameHelper.getPrefix(muleContext),
                flowConstruct.getName(), this.getClass().getName());
        }
        eventCorrelator = new EventCorrelator(getCorrelatorCallback(muleContext), next, messageInfoMapping,
            muleContext, flowConstruct, persistentStores, storePrefix);

        // Inherit failOnTimeout from async-reply if this aggregator is being used
        // for async-reply
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
    }

    public void start() throws MuleException
    {
        if (timeout != 0)
        {
            eventCorrelator.start();
        }
    }

    public void stop() throws MuleException
    {
        eventCorrelator.stop();
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    protected abstract EventCorrelatorCallback getCorrelatorCallback(MuleContext muleContext);

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleEvent result = eventCorrelator.process(event);
        if (result == null || VoidMuleEvent.getInstance().equals(result))
        {
            return result;
        }
        return processNext(result);
    }

    public void expireAggregation(String groupId) throws MessagingException
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

    public void setMessageInfoMapping(MessageInfoMapping messageInfoMapping)
    {
        this.messageInfoMapping = messageInfoMapping;
    }

    public boolean isPersistentStores()
    {
        return persistentStores;
    }

    public void setPersistentStores(boolean persistentStores)
    {
        this.persistentStores = persistentStores;
    }

    public String getStorePrefix()
    {
        return storePrefix;
    }

    public void setStorePrefix(String storePrefix)
    {
        this.storePrefix = storePrefix;
    }

    public void dispose()
    {
        eventCorrelator.dispose();
    }

}
