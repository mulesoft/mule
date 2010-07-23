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

import org.mule.DefaultMuleEvent;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.routing.correlation.EventCorrelator;
import org.mule.routing.correlation.EventCorrelatorCallback;

import javax.resource.spi.work.WorkException;

/**
 * <code>AbstractEventAggregator</code> will aggregate a set of messages into a single message.
 */

public abstract class AbstractAggregator extends AbstractInterceptingMessageProcessor implements Initialisable, MuleContextAware
{

    protected EventCorrelator eventCorrelator;
    protected MuleContext muleContext;
    private MessageInfoMapping messageInfoMapping = new MuleMessageInfoMapping();

    private int timeout = 0;
    private boolean failOnTimeout = true;

    public void initialise() throws InitialisationException
    {
        eventCorrelator = new EventCorrelator(getCorrelatorCallback(muleContext), next, getMessageInfoMapping(), muleContext);
        if (timeout != 0)
        {
            eventCorrelator.setTimeout(timeout);
            eventCorrelator.setFailOnTimeout(isFailOnTimeout());
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
        MuleMessage msg = eventCorrelator.process(event);
        if (msg == null)
        {
            return null;
        }
        return processNext(new DefaultMuleEvent(msg, event));
    }

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout(int timeout)
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

    public MessageInfoMapping getMessageInfoMapping()
    {
        return messageInfoMapping;
    }

    public void setMessageInfoMapping(MessageInfoMapping messageInfoMapping)
    {
        this.messageInfoMapping = messageInfoMapping;
    }
}
