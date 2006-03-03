/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.routing.inbound;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import org.mule.impl.MuleEvent;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.RoutingException;

import java.util.Map;

/**
 * <code>AbstractEventAggregator</code> will aggregate a set of messages into
 * a single message
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractEventAggregator extends SelectiveConsumer
{
    protected static final String NO_CORRELATION_ID = "no-id";

    protected Map eventGroups = new ConcurrentHashMap();
    private Object lock = new Object();

    public UMOEvent[] process(UMOEvent event) throws MessagingException
    {
    	AtomicBoolean doAggregate = new AtomicBoolean(false);
        EventGroup eg = null;
        // synchronized (lock)
        // {
        if (isMatch(event)) {
            eg = addEvent(event);
            doAggregate.compareAndSet(false, shouldAggregate(eg));
        }
        // }
        if (doAggregate.get()) {
            synchronized (lock) {
                UMOMessage returnMessage = aggregateEvents(eg);
                removeGroup(eg.getGroupId());
                UMOEndpoint endpoint = new MuleEndpoint(event.getEndpoint());
                endpoint.setTransformer(null);
                endpoint.setName(getClass().getName());
                UMOEvent returnEvent = new MuleEvent(returnMessage, endpoint, event.getComponent(), event);
                return new UMOEvent[] { returnEvent };
            }
        }
        return null;
    }

    /**
     * Adds the event to an event group. Groups are defined by the correlationId
     * on the message. If no correlationId is set a default group is created for
     * all events without a correlationId. If there is no group for the current
     * correlationId one will be created and added to the router.
     * 
     * @param event
     * @return
     */
    protected EventGroup addEvent(UMOEvent event)
    {
        String cId = event.getMessage().getCorrelationId();
        if (cId == null) {
            cId = NO_CORRELATION_ID;
        }
        EventGroup eg = (EventGroup) eventGroups.get(cId);
        if (eg == null) {
            eg = new EventGroup(cId);
            eg.addEvent(event);
            eventGroups.put(eg.getGroupId(), eg);
        } else {
            eg.addEvent(event);
        }
        return eg;
    }

    protected void removeGroup(Object id)
    {
        // synchronized (eventGroups)
        // {
        eventGroups.remove(id);
        // }
    }

    /**
     * Determines if the event group is ready to be aggregated. if the group is
     * ready to be aggregated (this is entirely up to the application. it could
     * be determined by volume, last modified time or some oher criteria based
     * on the last event received)
     * 
     * @param events
     * @return
     */
    protected abstract boolean shouldAggregate(EventGroup events);

    /**
     * This method is invoked if the shouldAggregate method is called and
     * returns true. Once this method returns an aggregated message the event
     * group is removed from the router
     * 
     * @param events the event group for this request
     * @return an aggregated message
     * @throws RoutingException if the aggregation fails. in this scenario the
     *             whole event group is removed and passed to the exception
     *             handler for this componenet
     */
    protected abstract UMOMessage aggregateEvents(EventGroup events) throws RoutingException;
}
