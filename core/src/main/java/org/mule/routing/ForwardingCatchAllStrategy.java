/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing;

import org.mule.api.MuleEvent;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.RoutingException;
import org.mule.config.i18n.CoreMessages;

/**
 * <code>ForwardingCatchAllStrategy</code> acts as a catch and forward router for
 * any events not caught by the router this strategy is associated with. Users can
 * assign an endpoint to this strategy to forward all events to. This can be used as
 * a dead letter/error queue.
 *
 */
public class ForwardingCatchAllStrategy extends AbstractCatchAllStrategy
{
    private boolean sendTransformed = false;

    protected OutboundEndpoint endpoint;

    public void setEndpoint(OutboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }
    
    public void setMessageProcessor(MessageProcessor processor)
    {
        this.endpoint = (OutboundEndpoint) processor;
    }

    public OutboundEndpoint getEndpoint()
    {
        return endpoint;
    }

    @Override
    public MuleEvent doCatchMessage(MuleEvent event) throws RoutingException
    {
        if (getEndpoint() == null)
        {
            throw new RoutingException(CoreMessages.noCatchAllEndpointSet(), event, getEndpoint());
        }
        try
        {
            OutboundEndpoint endpoint = getEndpoint();
            if (sendTransformed && endpoint.getTransformers() != null)
            {
                event.getMessage().applyTransformers(event, endpoint.getTransformers());
            }

            MuleEvent result = endpoint.process(event);
            if (statistics != null && statistics.isEnabled())
            {
                statistics.incrementRoutedMessage(getEndpoint());
            }
            return result;
        }
        catch (Exception e)
        {
            throw new RoutingException(event, getEndpoint(), e);

        }
    }

    public boolean isSendTransformed()
    {
        return sendTransformed;
    }

    public void setSendTransformed(boolean sendTransformed)
    {
        this.sendTransformed = sendTransformed;
    }
}
