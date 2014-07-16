/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.transformer.TransformerException;
import org.mule.routing.AbstractAggregator;
import org.mule.routing.AggregationException;
import org.mule.routing.EventGroup;
import org.mule.routing.correlation.CollectionCorrelatorCallback;
import org.mule.routing.correlation.EventCorrelatorCallback;
import org.mule.util.concurrent.ThreadNameHelper;

import java.util.Iterator;

public class TestAggregator extends AbstractAggregator
{
    @Override
    protected EventCorrelatorCallback getCorrelatorCallback(MuleContext muleContext)
    {
        return new CollectionCorrelatorCallback(muleContext,false,storePrefix)
        {
            @Override
            public MuleEvent aggregateEvents(EventGroup events) throws AggregationException
            {
                StringBuilder buffer = new StringBuilder(128);

                try
                {
                    for (Iterator<MuleEvent> iterator = events.iterator(); iterator.hasNext();)
                    {
                        MuleEvent event = iterator.next();
                        try
                        {
                            buffer.append(event.transformMessageToString());
                        }
                        catch (TransformerException e)
                        {
                            throw new AggregationException(events, null, e);
                        }
                    }
                }
                catch (ObjectStoreException e)
                {
                    throw new AggregationException(events,null,e);
                }

                logger.debug("event payload is: " + buffer.toString());
                return new DefaultMuleEvent(new DefaultMuleMessage(buffer.toString(), muleContext), events.getMessageCollectionEvent());
            }
        };
    }
}
