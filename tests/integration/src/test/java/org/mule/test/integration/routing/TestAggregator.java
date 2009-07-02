/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.routing.AggregationException;
import org.mule.routing.CollectionCorrelatorCallback;
import org.mule.routing.EventCorrelatorCallback;
import org.mule.routing.inbound.EventGroup;
import org.mule.routing.response.ResponseCorrelationAggregator;

import java.util.Iterator;

public class TestAggregator extends ResponseCorrelationAggregator
{

    @Override
    protected EventCorrelatorCallback getCorrelatorCallback()
    {
        return new CollectionCorrelatorCallback(muleContext)
        {
            public MuleMessage aggregateEvents(EventGroup events) throws AggregationException
            {
                StringBuffer buffer = new StringBuffer(128);

                for (Iterator iterator = events.iterator(); iterator.hasNext();)
                {
                    MuleEvent event = (MuleEvent) iterator.next();
                    try
                    {
                        buffer.append(event.transformMessageToString());
                    }
                    catch (TransformerException e)
                    {
                        throw new AggregationException(events, event.getEndpoint(), e);
                    }
                }

                logger.debug("event payload is: " + buffer.toString());
                return new DefaultMuleMessage(buffer.toString(), muleContext);
            }
        };
    }


}
