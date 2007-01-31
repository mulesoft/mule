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

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.routing.inbound.AbstractEventAggregator;
import org.mule.routing.inbound.EventGroup;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.routing.ResponseTimeoutException;
import org.mule.umo.routing.RoutingException;
import org.mule.util.MapUtils;
import org.mule.util.concurrent.Latch;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

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
     * A map of EventGroup objects. These represent one or more messages to be
     * agregated, keyed by message id. There will be one response message for every
     * EventGroup.
     */
    protected final ConcurrentMap eventGroups = new ConcurrentHashMap();

    /**
     * A map of locks used to wait for response messages for a given message id
     */
    protected final ConcurrentMap locks = new ConcurrentHashMap();

    /**
     * The collection of messages that are ready to be returned to the callee. Keyed
     * by Message ID
     */
    protected final ConcurrentMap responseMessages = new ConcurrentHashMap();

    public void process(UMOEvent event) throws RoutingException
    {
        // the correlationId of the event's message
        final Object groupId = this.getReplyAggregateIdentifier(event.getMessage());
        if (groupId == null || groupId.equals("-1"))
        {
            throw new RoutingException(new Message(Messages.NO_CORRELATION_ID), event.getMessage(), event
                .getEndpoint());
        }

        // indicates interleaved EventGroup removal (very rare)
        boolean lookupMiss = false;

        // spinloop for the EventGroup lookup
        while (true)
        {
            if (lookupMiss)
            {
                try
                {
                    // recommended over Thread.yield()
                    Thread.sleep(1);
                }
                catch (InterruptedException interrupted)
                {
                    Thread.currentThread().interrupt();
                }
            }

            // check for an existing group first
            EventGroup group = this.getEventGroup(groupId);

            // does the group exist?
            if (group == null)
            {
                // ..apparently not, so create a new one & add it
                group = this.addEventGroup(this.createEventGroup(event, groupId));
            }

            // ensure that only one thread at a time evaluates this EventGroup
            synchronized (group)
            {
                // make sure no other thread removed the group in the meantime
                if (group != this.getEventGroup(groupId))
                {
                    // if that is the (rare) case, spin
                    lookupMiss = true;
                    continue;
                }

                if (logger.isDebugEnabled())
                {
                    logger.debug("Adding event to response aggregator group: " + groupId);
                }

                // add the incoming event to the group
                group.addEvent(event);

                // check to see if the event group is ready to be aggregated
                if (this.shouldAggregateEvents(group))
                {
                    // create the response message
                    UMOMessage returnMessage = this.aggregateEvents(group);

                    // remove the eventGroup as no further message will be received
                    // for this group once we aggregate
                    this.removeEventGroup(group);

                    // add the new response message so that it can be collected by
                    // the response Thread
                    UMOMessage previousResult = (UMOMessage)responseMessages.putIfAbsent(groupId,
                        returnMessage);
                    if (previousResult != null)
                    {
                        // this would indicate that we need a better way to prevent
                        // continued aggregation for a group that is currently being
                        // processed. Can this actually happen?
                        throw new IllegalStateException(
                            "Detected duplicate aggregation result message with id: " + groupId);
                    }

                    // will get/create a latch for the response Message ID and
                    // release it, notifying other threads that the response message
                    // is available
                    Latch l = (Latch)locks.get(groupId);
                    if (l == null)
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Creating latch for " + groupId + " in " + this);
                        }

                        l = new Latch();
                        Latch previous = (Latch)locks.putIfAbsent(groupId, l);
                        if (previous != null)
                        {
                            l = previous;
                        }
                    }

                    l.countDown();
                }

                // result or not: exit spinloop
                break;
            }
        }
    }

    /**
     * @see AbstractEventAggregator#createEventGroup(UMOEvent, Object)
     */
    protected EventGroup createEventGroup(UMOEvent event, Object groupId)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Creating new event group: " + groupId + " in " + this);
        }

        return new EventGroup(groupId);
    }

    /**
     * @see AbstractEventAggregator#getEventGroup(Object)
     */
    protected EventGroup getEventGroup(Object groupId)
    {
        return (EventGroup)eventGroups.get(groupId);
    }

    /**
     * @see AbstractEventAggregator#addEventGroup(EventGroup)
     */
    protected EventGroup addEventGroup(EventGroup group)
    {
        EventGroup previous = (EventGroup)eventGroups.putIfAbsent(group.getGroupId(), group);
        // a parallel thread might have removed the EventGroup already,
        // therefore we need to validate our current reference
        return (previous != null ? previous : group);
    }

    /**
     * @see AbstractEventAggregator#removeEventGroup(EventGroup)
     */
    protected void removeEventGroup(EventGroup group)
    {
        eventGroups.remove(group.getGroupId());
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
        Object responseId = this.getCallResponseAggregateIdentifier(message);

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

        // indicates whether the result message could be obtained in the required
        // timeout interval
        boolean resultAvailable = false;

        // flag for catching the interrupted status of the Thread waiting for a
        // result
        boolean interruptedWhileWaiting = false;

        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Waiting for response to message: " + responseId);
            }

            // how long should we wait for the lock?
            if (this.getTimeout() <= 0)
            {
                l.await();
                resultAvailable = true;
            }
            else
            {
                resultAvailable = l.await(this.getTimeout(), TimeUnit.MILLISECONDS);
            }
        }
        catch (InterruptedException e)
        {
            interruptedWhileWaiting = true;
        }
        finally
        {
            locks.remove(responseId);
            result = (UMOMessage)responseMessages.remove(responseId);

            if (interruptedWhileWaiting)
            {
                Thread.currentThread().interrupt();
            }
        }

        if (!resultAvailable)
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Current responses are: \n" + MapUtils.toString(responseMessages, true));
            }

            throw new ResponseTimeoutException(new Message(Messages.RESPONSE_TIMED_OUT_X_WAITING_FOR_ID_X,
                String.valueOf(getTimeout()), responseId), message, null);
        }

        if (result == null)
        {
            // this should never happen, just using it as a safe guard for now
            throw new IllegalStateException("Response Message is null");
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("remaining locks  : " + locks.keySet());
            logger.debug("remaining results: " + responseMessages.keySet());
        }

        return result;
    }

    /**
     * @see AbstractEventAggregator#shouldAggregateEvents(EventGroup)
     */
    protected abstract boolean shouldAggregateEvents(EventGroup events);

    /**
     * @see AbstractEventAggregator#aggregateEvents(EventGroup)
     */
    protected abstract UMOMessage aggregateEvents(EventGroup events) throws RoutingException;

}
