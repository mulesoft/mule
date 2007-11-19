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

import org.mule.impl.MuleEvent;
import org.mule.impl.MuleSession;
import org.mule.impl.NullSessionHandler;
import org.mule.impl.RequestContext;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;

/**
 * An inbound router that can forward every message to another destination as defined
 * in the "endpoint" property. This can be a logical destination of a URI. <p/> A
 * filter can be applied to this router so that only events matching a criteria will
 * be tapped.
 */
public class WireTap extends SelectiveConsumer
{
    private volatile UMOImmutableEndpoint tap;

    public boolean isMatch(UMOEvent event) throws MessagingException
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

    public UMOEvent[] process(UMOEvent event) throws MessagingException
    {
        RequestContext.setEvent(null);
        try
        {
            //We have to create a new session for this dispatch, since the session may get altered
            //using this call, changing the behaviour of the request
            UMOSession session = new MuleSession(event.getMessage(), new NullSessionHandler());
            tap.dispatch(new MuleEvent(event.getMessage(), tap, session, false));
        }
        catch (MessagingException e)
        {
            throw e;
        }
        catch (UMOException e)
        {
            throw new DispatchException(event.getMessage(), tap, e);
        }
        return super.process(event);
    }

    public UMOImmutableEndpoint getEndpoint()
    {
        return tap;
    }

    public void setEndpoint(UMOImmutableEndpoint endpoint) throws UMOException
    {
        this.tap = endpoint;
    }
}
