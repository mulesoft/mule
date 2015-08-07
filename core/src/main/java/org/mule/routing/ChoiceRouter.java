/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import java.util.Collection;
import java.util.Collections;

import org.mule.api.MuleEvent;
import org.mule.api.NonBlockingSupported;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.api.routing.filter.Filter;
import org.mule.processor.NonBlockingMessageProcessor;

/**
 * Routes the event to a single<code>MessageProcessor</code> using a {@link Filter}
 * to evaluate the event being processed and find the first route that can be used.
 * <p>
 * If a default route has been configured and no match has been found, the default
 * route will be used. Otherwise it throws a {@link RoutePathNotFoundException}.
 */
public class ChoiceRouter extends AbstractSelectiveRouter implements NonBlockingMessageProcessor
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
