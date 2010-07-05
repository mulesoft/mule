/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.dsl.routers;

import org.mule.DefaultMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.routing.outbound.AbstractOutboundRouter;

/**
 * TODO
 */
public class ContentBasedRouter extends AbstractOutboundRouter
{
    public MuleMessage route(MuleEvent theEvent) throws MessagingException
    {
        MuleMessage message = theEvent.getMessage();
        MuleSession session = theEvent.getSession();

        for (OutboundEndpoint endpoint : endpoints)
        {
            if (isMatch(message))
            {
                try
                {
                    DefaultMuleEvent event = new DefaultMuleEvent(message, endpoint, session, endpoint.isSynchronous());
                    if (endpoint.isSynchronous())
                    {
                        MuleEvent resultEvent;
                        resultEvent = endpoint.process(event);
                        if (resultEvent != null)
                        {
                            return resultEvent.getMessage();
                        }
                        else
                        {
                            return null;
                        }
                    }
                    else
                    {
                        endpoint.process(event);
                        return null;
                    }
                }
                catch (MuleException e)
                {
                    throw new MessagingException(e.getI18nMessage(), message, e);
                }
            }
        }
        //TODO
        throw new RuntimeException("Event not processed");
    }

    public boolean isMatch(MuleMessage message) throws MessagingException
    {
        for (OutboundEndpoint endpoint : endpoints)
        {
            if (endpoint.getFilter() == null || endpoint.getFilter().accept(message))
            {
                return true;
            }
        }
        return false;
    }
}
