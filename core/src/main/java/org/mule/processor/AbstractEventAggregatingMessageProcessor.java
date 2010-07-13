/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.DefaultMuleEvent;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.routing.EventCorrelator;
import org.mule.routing.EventCorrelatorCallback;
import org.mule.routing.MuleMessageInfoMapping;

import javax.resource.spi.work.WorkException;

/**
 * <code>AbstractEventAggregator</code> will aggregate a set of messages into a
 * single message.
 */

public abstract class AbstractEventAggregatingMessageProcessor implements MessageProcessor
{

    protected EventCorrelator eventCorrelator;
    private MessageInfoMapping messageInfoMapping = new MuleMessageInfoMapping();
    private int timeout = 0;

    private boolean failOnTimeout = true;

    protected void ensureInitialised(MuleEvent event) throws MuleException
    {
        if (eventCorrelator == null)
        {
            eventCorrelator = new EventCorrelator(getCorrelatorCallback(event), getMessageInfoMapping(), event.getMuleContext());
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
                    throw new DefaultMuleException(e);
                }
            }
        }
    }

    protected abstract EventCorrelatorCallback getCorrelatorCallback(MuleEvent event);


    public MuleEvent process(MuleEvent event) throws MuleException
    {
        ensureInitialised(event);
        MuleMessage msg = eventCorrelator.process(event);
        if (msg == null)
        {
            return null;
        }
        return new DefaultMuleEvent(msg, event);
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