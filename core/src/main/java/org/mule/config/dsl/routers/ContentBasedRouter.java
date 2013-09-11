/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.dsl.routers;

import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.routing.outbound.AbstractOutboundRouter;

/**
 * TODO
 */
@Deprecated
public class ContentBasedRouter extends AbstractOutboundRouter
{
    @Override
    public MuleEvent route(MuleEvent theEvent) throws MessagingException
    {
        MuleMessage message = theEvent.getMessage();

        for (MessageProcessor target : routes)
        {
            try
            {
                if (isMatch(message))
                {
                    MuleEvent event = RequestContext.setEvent(theEvent);
                    return target.process(event);
                }
            }
            catch (MessagingException e)
            {
                throw e;
            }
            catch (MuleException e)
            {
                throw new MessagingException(e.getI18nMessage(), theEvent, e, this);
            }
        }
        //TODO
        throw new RuntimeException("Event not processed");
    }

    public boolean isMatch(MuleMessage message) throws MuleException
    {
        for (MessageProcessor target : routes)
        {
            if (target instanceof ImmutableEndpoint)
            {
                ImmutableEndpoint endpoint = (ImmutableEndpoint)target;
                if (endpoint.getFilter() == null || endpoint.getFilter().accept(message))
                {
                    return true;
                }
            }
            else
            {
                return true;
            }
        }
        return false;
    }
}
