/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing;

import java.util.Collection;
import java.util.Collections;

import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.api.routing.filter.Filter;

/**
 * Routes the event to a single<code>MessageProcessor</code> using a {@link Filter}
 * to evaluate the event being processed and find the first route that can be used.
 * <p>
 * If a default route has been configured and no match has been found, the default
 * route will be used. Otherwise it throws a {@link RoutePathNotFoundException}.
 */
public class ChoiceRouter extends AbstractSelectiveRouter
{
    @Override
    protected Collection<MessageProcessor> selectProcessors(MuleEvent event)
    {
        for (MessageProcessorFilterPair mpfp : getConditionalMessageProcessors())
        {
            if (mpfp.getFilter().accept(event.getMessage()))
            {
                return Collections.singleton(mpfp.getMessageProcessor());
            }
        }

        return Collections.emptySet();
    }
}
