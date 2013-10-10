/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutingException;
import org.mule.routing.outbound.AbstractOutboundRouter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * RoundRobin divides the messages it receives among its target routes in round-robin
 * fashion. This includes messages received on all threads, so there is no guarantee
 * that messages received from a splitter are sent to consecutively numbered targets.
 */
public class RoundRobin extends AbstractOutboundRouter
{
    /** Index of target route to use */
    AtomicInteger index = new AtomicInteger(0);

    /**
     *  Process the event using the next target route in sequence
     */
    @Override
    public MuleEvent route(MuleEvent event) throws MessagingException
    {
        int modulo = getAndIncrementModuloN(routes.size());
        if (modulo < 0)
        {
            throw new CouldNotRouteOutboundMessageException(event, this);
        }
        
        MessageProcessor mp = routes.get(modulo);
        try
        {
            return mp.process(event);
        }
        catch (MuleException ex)
        {
            throw new RoutingException(event, this, ex);
        }
    }

    /**
     * Get the index of the processor to use
     */
    private int getAndIncrementModuloN(int modulus)
    {
        if (modulus == 0)
        {
            return -1;
        }
        while (true)
        {
            int lastIndex = index.get();
            int nextIndex = (lastIndex + 1) % modulus;
            if (index.compareAndSet(lastIndex, nextIndex))
            {
                return nextIndex;
            }
        }
    }

    @Override
    public boolean isMatch(MuleMessage message) throws MuleException
    {
        return true;
    }
}
