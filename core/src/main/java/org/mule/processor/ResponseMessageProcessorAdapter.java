/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;

public class ResponseMessageProcessorAdapter extends AbstractResponseMessageProcessor implements Lifecycle, FlowConstructAware
{

    protected MessageProcessor responseProcessor;
    protected FlowConstruct flowConstruct;

    public ResponseMessageProcessorAdapter()
    {
        super();
    }

    public ResponseMessageProcessorAdapter(MessageProcessor responseProcessor)
    {
        super();
        this.responseProcessor = responseProcessor;
    }

    public void setProcessor(MessageProcessor processor)
    {
        this.responseProcessor = processor;
    }

    @Override
    protected MuleEvent processResponse(MuleEvent event) throws MuleException
    {
        if (responseProcessor == null || !isEventValid(event))
        {
            return event;
        }
        else
        {
            MuleEvent copy = (MuleEvent) ((ThreadSafeAccess) event).newThreadCopy();
            MuleEvent result = responseProcessor.process(event);
            if (result == null || VoidMuleEvent.getInstance().equals(result))
            {
                // If <response> returns null then it acts as an implicit branch like in flows, the different
                // here is that what's next, it's not another message processor that follows this one in the
                // configuration file but rather the response phase of the inbound endpoint, or optionally
                // other response processing on the way back to the inbound endpoint.
                return copy;
            }
            else
            {
                return result;
            }
        }
    }

    public void initialise() throws InitialisationException
    {
        if (responseProcessor instanceof MuleContextAware)
        {
            ((MuleContextAware) responseProcessor).setMuleContext(muleContext);
        }
        if (responseProcessor instanceof FlowConstructAware)
        {
            ((FlowConstructAware) responseProcessor).setFlowConstruct(flowConstruct);
        }
        if (responseProcessor instanceof Initialisable)
        {
            ((Initialisable) responseProcessor).initialise();
        }
    }

    public void start() throws MuleException
    {
        if (responseProcessor instanceof Startable)
        {
            ((Startable) responseProcessor).start();
        }
    }

    public void stop() throws MuleException
    {
        if (responseProcessor instanceof Stoppable)
        {
            ((Stoppable) responseProcessor).stop();
        }
    }

    public void dispose()
    {
        if (responseProcessor instanceof Disposable)
        {
            ((Disposable) responseProcessor).dispose();
        }
    }

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

}
