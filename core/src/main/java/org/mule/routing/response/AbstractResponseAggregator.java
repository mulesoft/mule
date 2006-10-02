/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.response;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.routing.inbound.EventGroup;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.routing.ResponseTimeoutException;
import org.mule.umo.routing.RoutingException;
import org.mule.util.PropertiesUtils;
import org.mule.util.concurrent.Latch;

/**
 * <code>AbstractResponseAggregator</code> provides a base class for implementing
 * response aggregator routers. This provides a thread-safe implemenetation and
 * allows developers to customise how and when events are grouped and collated.
 * Response Agrregators are used to collect responses that are usually sent to
 * replyTo endpoints set on outbound routers. When an event is sent out via an
 * outbound router, the response router will block the response flow on an
 * UMOComponent until the Response Router resolves a reply or times out.
 */
public abstract class AbstractResponseAggregator extends AbstractResponseRouter
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(getClass());

    /**
     * The collection of messages that are ready to be returned to the callee. Keyed
     * by Message ID
     */
    protected ConcurrentMap responseEvents = new ConcurrentHashMap();

    /**
     * A map of locks used to synchronize operations on an eventGroup or response
     * message for a given Message ID.
     */
    private ConcurrentMap locks = new ConcurrentHashMap();

    /**
     * Map of EventGroup objects. These represent one or mre messages to be
     * agregated. These are keyed on Message ID. There will be one responseEvent for
     * every EventGroup.
     */
    protected final ConcurrentMap eventGroups = new ConcurrentHashMap();

    public void process(UMOEvent event) throws RoutingException
    {
        // add new event to an event group (it will create a new group if one does
        // not exist for the event correlation ID)
        EventGroup eg = addEvent(event);

        // check to see if the event group is ready to be aggregated
        if (shouldAggregate(eg))
        {
            // create the response message
            UMOMessage returnMessage = aggregateEvents(eg);
            Object id = eg.getGroupId();

            // remove the eventGroup as no further message will be received for this
            // group once we aggregate
            removeEventGroup(id);

            // add the new response message so that it can be collected by the
            // response Thread
            UMOMessage previousResult = (UMOMessage)responseEvents.putIfAbsent(id, returnMessage);
            if (previousResult != null)
            {
                // this would indicate that we need a better way to prevent continued
                // aggregation for a group that is currently being processed. Can
                // this actually happen?
                throw new IllegalStateException("Detected duplicate aggregation result message with id: "
                                                + id);
            }

            // will get/create a latch for the response Message ID and release it,
            // notifying other threads that the response message is available
            Latch l = (Latch)locks.get(id);
            if (l == null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Creating latch for " + id + " in " + this);
                }

                l = new Latch();
                Latch previous = (Latch)locks.putIfAbsent(id, l);
                if (previous != null)
                {
                    l = previous;
                }
            }

            l.countDown();
        }
    }

    /**
     * Adds the event to an event group. Groups are defined by the correlationId on
     * the message. If no 'correlation Id' is returned from calling
     * <code>getReplyAggregateIdentifier()</code> a routing exception will be
     * thrown
     * 
     * @param event the reply event received by the response router
     * @return The event group for the current event or a new group if the current
     *         event doesn't belong to an existing group
     */
    protected EventGroup addEvent(UMOEvent event) throws RoutingException
    {
        Object cId = getReplyAggregateIdentifier(event.getMessage());

        if (cId == null || cId.equals("-1"))
        {
            throw new RoutingException(new Message(Messages.NO_CORRELATION_ID), event.getMessage(),
                event.getEndpoint());
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Adding event to response aggregator group: " + cId);
        }

        EventGroup eg = (EventGroup)eventGroups.get(cId);
        if (eg == null)
        {
            eg = createEventGroup(cId, event);
            EventGroup previous = (EventGroup)eventGroups.putIfAbsent(eg.getGroupId(), eg);
            if (previous != null)
            {
                eg = previous;
            }
        }

        eg.addEvent(event);

        return eg;
    }

    /**
     * Creates a new event group with the given Id and can use other properties on
     * the event Custom implementations can even overload the eventGroup object here
     * 
     * @param id The Event group Id for the new Group
     * @param event the current event
     * @return a New event group for the incoming event
     */
    protected EventGroup createEventGroup(Object id, UMOEvent event)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Creating new event group: " + id + " in " + this);
        }
        return new EventGroup(id);
    }

    protected void removeEventGroup(Object id)
    {
        eventGroups.remove(id);
    }

    /**
     * This method is called by the responding callee thread and should return the
     * aggregated response message
     * 
     * @param message
     * @return
     * @throws RoutingException
     */
    public UMOMessage getResponse(UMOMessage message) throws RoutingException
    {
        Object responseId = getCallResponseAggregateIdentifier(message);

        if (logger.isDebugEnabled())
        {
            logger.debug("Waiting for response for message id: " + responseId + " in " + this);
        }

        Latch l = (Latch)locks.get(responseId);
        if (l == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Got response but no one is waiting for it yet. Creating latch for "
                             + responseId + " in " + this);
            }

            l = new Latch();
            Latch previous = (Latch)locks.putIfAbsent(responseId, l);
            if (previous != null)
            {
                l = previous;
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Got latch for message: " + responseId);
        }

        // the final result message
        UMOMessage result;

        // indicates whether waiting for the result timed out
        boolean b = false;

        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Waiting for response to message: " + responseId);
            }

            // how long should we wait for the lock?
            if (getTimeout() <= 0)
            {
                l.await();
                b = true;
            }
            else
            {
                b = l.await(this.getTimeout(), TimeUnit.MILLISECONDS);
            }
        }
        catch (InterruptedException e)
        {
            logger.error(e.getMessage(), e);
        }
        finally
        {
            locks.remove(responseId);
            result = (UMOMessage)responseEvents.remove(responseId);
        }

        if (!b)
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Current responses are: \n"
                             + PropertiesUtils.propertiesToString(responseEvents, true));
            }

            throw new ResponseTimeoutException(new Message(Messages.RESPONSE_TIMED_OUT_X_WAITING_FOR_ID_X,
                String.valueOf(getTimeout()), responseId), message, null);
        }

        if (result == null)
        {
            // this should never happen, just using it as a safe guard for now
            throw new IllegalStateException("Response Message is null");
        }

        return result;
    }

    /**
     * Determines if the event group is ready to be aggregated. if the group is ready
     * to be aggregated (this is entirely up to the application. it could be
     * determined by volume, last modified time or some oher criteria based on the
     * last event received)
     * 
     * @param events
     * @return true if the event gorep is ready for aggregation
     */
    protected abstract boolean shouldAggregate(EventGroup events);

    /**
     * This method is invoked if the shouldAggregate method is called and returns
     * true. Once this method returns an aggregated message the event group is
     * removed from the router
     * 
     * @param events the event group for this request
     * @return an aggregated message
     * @throws RoutingException if the aggregation fails. in this scenario the whole
     *             event group is removed and passed to the exception handler for
     *             this componenet
     */
    protected abstract UMOMessage aggregateEvents(EventGroup events) throws RoutingException;

}
