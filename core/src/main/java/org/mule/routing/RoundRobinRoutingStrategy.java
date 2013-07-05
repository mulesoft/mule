/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.api.*;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.config.i18n.CoreMessages;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Routing strategy that divides the messages it receives among its target routes in round-robin
 * fashion. The set of routes is obtained dynamically using a {@link org.mule.routing.DynamicRouteResolver}.
 *
 * This includes messages received on all threads, so there is no guarantee
 * that messages received from a splitter are sent to consecutively numbered targets.
 */
public class RoundRobinRoutingStrategy extends AbstractRoutingStrategy
{
    private final IdentifiableDynamicRouteResolver identifiableDynamicRouteResolver;
    private Map<String,Short> roundRobinState = new HashMap<String,Short>();
    private final MuleContext muleContext;

    public RoundRobinRoutingStrategy(final MuleContext muleContext, final IdentifiableDynamicRouteResolver identifiableDynamicRouteResolver)
    {
        super(muleContext);
        this.identifiableDynamicRouteResolver = identifiableDynamicRouteResolver;
        this.muleContext = muleContext;
    }

    @Override
    public MuleEvent route(MuleEvent event, List<MessageProcessor> messageProcessors) throws MessagingException
    {
        if(!messageProcessors.isEmpty())
        {
            String id = identifiableDynamicRouteResolver.getRouteIdentifier(event);
            Short nextMessageProcessor = 0;
            synchronized (this)
            {
                if(roundRobinState.containsKey(id))
                {
                    Short lastMessageProcessor = roundRobinState.get(id);
                    nextMessageProcessor = (short) (lastMessageProcessor + 1 >= messageProcessors.size()? 0 : lastMessageProcessor + 1);
                    roundRobinState.put(id, nextMessageProcessor);
                }
                else
                {
                    roundRobinState.put(id, (short) 0);
                }
            }

            MessageProcessor mp = messageProcessors.get(nextMessageProcessor);

            MuleEvent eventCopy = cloneEventForRouting(event, mp);
            try
            {
                return mp.process(eventCopy);
            }
            catch (MuleException me)
            {
                throw new RoutingFailedMessagingException(CoreMessages.createStaticMessage("Error processing event"), event, me);
            }

        }
        else
        {
            throw new RoutePathNotFoundException(CoreMessages.noEndpointsForRouter(), event, null);
        }
    }

    private MuleEvent cloneEventForRouting(MuleEvent event, MessageProcessor mp) throws MessagingException
    {
        return createEventToRoute(event, cloneMessage(event, event.getMessage(), muleContext), mp);
    }
}
