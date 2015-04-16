/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.serialization.SerializationException;
import org.mule.api.store.ObjectStoreException;
import org.mule.routing.correlation.CollectionCorrelatorCallback;
import org.mule.routing.correlation.CorrelationSequenceComparator;
import org.mule.routing.correlation.EventCorrelatorCallback;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

public class MessageChunkAggregator extends AbstractAggregator
{
    public static final int DEFAULT_BUFFER_SIZE = 4096;

    protected Comparator eventComparator;

    public MessageChunkAggregator()
    {
        super();
        eventComparator = new CorrelationSequenceComparator();
    }

    @Override
    protected EventCorrelatorCallback getCorrelatorCallback(MuleContext muleContext)
    {
        return new CollectionCorrelatorCallback(muleContext, persistentStores, storePrefix)
        {
            /**
             * This method is invoked if the shouldAggregate method is called and
             * returns true. Once this method returns an aggregated message the event
             * group is removed from the router
             * 
             * @param events the event group for this request
             * @return an aggregated message
             * @throws org.mule.routing.AggregationException if the aggregation
             *             fails. in this scenario the whole event group is removed
             *             and passed to the exception handler for this componenet
             */
            @Override
            public MuleEvent aggregateEvents(EventGroup events) throws AggregationException
            {
                MuleEvent[] collectedEvents;
                try
                {
                    collectedEvents = events.toArray(false);
                }
                catch (ObjectStoreException e)
                {
                    throw new AggregationException(events, MessageChunkAggregator.this, e);
                }
                MuleEvent firstEvent = collectedEvents[0];
                Arrays.sort(collectedEvents, eventComparator);
                ByteArrayOutputStream baos = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);

                try
                {
                    for (MuleEvent event : collectedEvents)
                    {
                        baos.write(event.getMessageAsBytes());
                    }

                    MuleMessage message;

                    // try to deserialize message, since ChunkingRouter might have
                    // serialized
                    // the object...
                    try
                    {
                        // must deserialize in correct classloader
                        final Object deserialized = muleContext.getObjectSerializer().deserialize(baos.toByteArray());
                        message = new DefaultMuleMessage(deserialized, firstEvent.getMessage(), muleContext);
                    }
                    catch (SerializationException e)
                    {
                        message = new DefaultMuleMessage(baos.toByteArray(), firstEvent.getMessage(),
                            muleContext);
                    }

                    message.setCorrelationGroupSize(-1);
                    message.setCorrelationSequence(-1);

                    return new DefaultMuleEvent(message, firstEvent, getMergedSession(events.toArray()));
                }
                catch (Exception e)
                {
                    throw new AggregationException(events, MessageChunkAggregator.this, e);
                }
                finally
                {
                    IOUtils.closeQuietly(baos);
                }
            }
        };
    }

}
