/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.inbound;

import org.mule.DefaultMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.routing.EventCorrelator;
import org.mule.routing.EventCorrelatorCallback;

import javax.resource.spi.work.WorkException;

/**
 * <code>AbstractEventAggregator</code> will aggregate a set of messages into a
 * single message.
 */

public abstract class AbstractEventAggregator extends SelectiveConsumer
{
    protected EventCorrelator eventCorrelator;

    private int timeout = 0;

    @Override
    public void initialise() throws InitialisationException
    {
        eventCorrelator = new EventCorrelator(getCorrelatorCallback(), getMessageInfoMapping(), muleContext);
        if(timeout != 0)
        {
            eventCorrelator.setTimeout(timeout);
            try
            {
                eventCorrelator.enableTimeoutMonitor();
            }
            catch (WorkException e)
            {
                throw new InitialisationException(e, this);
            }
        }
        super.initialise();
    }

    protected abstract EventCorrelatorCallback getCorrelatorCallback();


    @Override
    public MuleEvent[] process(MuleEvent event) throws MessagingException
    {
        MuleMessage msg = eventCorrelator.process(event);
        if(msg==null)
        {
            return null;
        }
        MuleEvent[] result = new MuleEvent[]{new DefaultMuleEvent(msg, event)};
        return result;
    }
    
    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }
}
