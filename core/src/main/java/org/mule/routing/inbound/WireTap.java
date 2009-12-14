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
import org.mule.DefaultMuleSession;
import org.mule.NullSessionHandler;
import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.DispatchException;

/**
 * An inbound router that can forward every message to another destination as defined
 * in the "endpoint" property. This can be a logical destination of a URI. <p/> A
 * filter can be applied to this router so that only events matching a criteria will
 * be tapped.
 */
public class WireTap extends SelectiveConsumer
{
    private volatile OutboundEndpoint tap;

    public boolean isMatch(MuleEvent event) throws MessagingException
    {
        if (tap != null)
        {
            return super.isMatch(event);
        }
        else
        {
            logger.warn("No endpoint identifier is set on this wire tap");
            return false;
        }
    }

    public MuleEvent[] process(MuleEvent event) throws MessagingException
    {
        RequestContext.setEvent(null);
        try
        {
            //We have to create a new session for this dispatch, since the session may get altered
            //using this call, changing the behaviour of the request
            MuleSession session = new DefaultMuleSession(getMuleContext());
            tap.dispatch(new DefaultMuleEvent(event.getMessage(), tap, session, false));
        }
        catch (MessagingException e)
        {
            throw e;
        }
        catch (MuleException e)
        {
            throw new DispatchException(event.getMessage(), tap, e);
        }
        return super.process(event);
    }

    public OutboundEndpoint getEndpoint()
    {
        return tap;
    }

    public void setEndpoint(OutboundEndpoint endpoint) throws MuleException
    {
        this.tap = endpoint;
    }
}
