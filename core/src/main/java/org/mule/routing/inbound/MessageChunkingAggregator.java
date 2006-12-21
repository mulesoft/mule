/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.inbound;

import org.mule.impl.MuleMessage;
import org.mule.routing.AggregationException;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SerializationException;
import org.apache.commons.lang.SerializationUtils;

/**
 * todo document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MessageChunkingAggregator extends CorrelationAggregator
{

    /**
     * This method is invoked if the shouldAggregate method is called and returns
     * true. Once this method returns an aggregated message the event group is
     * removed from the router
     * 
     * @param events the event group for this request
     * @return an aggregated message
     * @throws org.mule.routing.AggregationException if the aggregation fails. in
     *             this scenario the whole event group is removed and passed to the
     *             exception handler for this componenet
     */
    protected UMOMessage aggregateEvents(EventGroup events) throws AggregationException
    {
        List eventList = IteratorUtils.toList(events.iterator(), events.size());
        UMOEvent firstEvent = (UMOEvent)eventList.get(0);
        Collections.sort(eventList, SequenceComparator.getInstance());
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        try
        {
            for (Iterator iterator = eventList.iterator(); iterator.hasNext();)
            {
                UMOEvent event = (UMOEvent)iterator.next();
                baos.write(event.getMessageAsBytes());
            }
            UMOMessage message;
            // try to deserialize message, since ChunkingRouter might have serialized
            // the object...
            try
            {
                message = new MuleMessage(SerializationUtils.deserialize(baos.toByteArray()),
                    firstEvent.getMessage());

            }
            catch (SerializationException e)
            {
                message = new MuleMessage(baos.toByteArray(), firstEvent.getMessage());
            }
            message.setCorrelationGroupSize(-1);
            message.setCorrelationSequence(-1);
            return message;
        }
        catch (Exception e)
        {
            throw new AggregationException(events, firstEvent.getEndpoint(), e);
        }
        finally
        {
            IOUtils.closeQuietly(baos);
        }
    }

    public static class SequenceComparator implements Comparator
    {
        private static SequenceComparator _instance = new SequenceComparator();

        public static SequenceComparator getInstance()
        {
            return _instance;
        }

        private SequenceComparator()
        {
            super();
        }

        public int compare(Object o1, Object o2)
        {
            UMOEvent event1 = (UMOEvent)o1;
            UMOEvent event2 = (UMOEvent)o2;
            if (event1.getMessage().getCorrelationSequence() > event2.getMessage()
                .getCorrelationSequence())
            {
                return 1;
            }
            else
            {
                return -1;
            }
        }
    }
}
