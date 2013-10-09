/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
