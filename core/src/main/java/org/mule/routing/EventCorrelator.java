/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleSession;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.api.routing.ResponseTimeoutException;
import org.mule.api.routing.RoutingException;
import org.mule.api.service.Service;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.RoutingNotification;
import org.mule.routing.inbound.EventGroup;
import org.mule.util.MapUtils;
import org.mule.util.concurrent.Latch;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import edu.emory.mathcs.backport.java.util.concurrent.helpers.Utils;
import org.apache.commons.collections.buffer.BoundedFifoBuffer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 */
public class EventCorrelator
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(EventCorrelator.class);

    public static final String NO_CORRELATION_ID = "no-id";
    
    public static final int MAX_PROCESSED_GROUPS = 50000;

    protected static final long MILLI_TO_NANO_MULTIPLIER = 1000000L;

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

    protected final Object groupsLock = new Object();

    // @GuardedBy groupsLock
    protected final BoundedFifoBuffer processedGroups = new BoundedFifoBuffer(MAX_PROCESSED_GROUPS);

    private int timeout = -1; // undefined

    private boolean failOnTimeout = true;

    private MessageInfoMapping messageInfoMapping;

    private MuleContext context;

    private EventCorrelatorCallback callback;

    private AtomicBoolean timerStarted = new AtomicBoolean(false);


    public EventCorrelator(EventCorrelatorCallback callback, MessageInfoMapping messageInfoMapping, MuleContext context)
    {
        if (callback == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("EventCorrelatorCallback").getMessage());
        }
        if (messageInfoMapping == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("MessageInfoMapping").getMessage());
        }
        if (context == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("MuleContext").getMessage());
        }
        this.callback = callback;
        this.messageInfoMapping = messageInfoMapping;
        this.context = context;


    }

    public void enableTimeoutMonitor() throws WorkException
    {
        if (timerStarted.get())
        {
            return;
        }

        this.context.getWorkManager().scheduleWork(new Work()
        {
            public void release()
            {
                //no op
            }


            public void run()
            {
                while (true)
                {
                    List<EventGroup> expired = new ArrayList<EventGroup>(1);
                    for (Object o : eventGroups.values())
                    {
                        EventGroup group = (EventGroup) o;
                        if ((group.getCreated() + getTimeout() * MILLI_TO_NANO_MULTIPLIER) < Utils.nanoTime())
                        {
                            expired.add(group);
                        }
                    }
                    if (expired.size() > 0)
                    {
                        for (Object anExpired : expired)
                        {
                            EventGroup group = (EventGroup) anExpired;
                            eventGroups.remove(group.getGroupId());
                            locks.remove(group.getGroupId());

                            final Service service = group.toArray()[0].getService();

                            if (isFailOnTimeout())
                            {
                                context.fireNotification(new RoutingNotification(group.toMessageCollection(), null,
                                                                                 RoutingNotification.CORRELATION_TIMEOUT));
                                service.getExceptionListener().exceptionThrown(
                                        new CorrelationTimeoutException(CoreMessages.correlationTimedOut(group.getGroupId()),
                                                                        group.toMessageCollection()));
                            }
                            else
                            {
                                if (logger.isDebugEnabled())
                                {
                                    logger.debug(MessageFormat.format(
                                            "Aggregator expired, but ''failOnTimeOut'' is false. Forwarding {0} events out of {1} " +
                                            "total for group ID: {2}", group.size(), group.expectedSize(), group.getGroupId()
                                    ));
                                }

                                try
                                {
                                    MuleMessage msg = callback.aggregateEvents(group);
                                    MuleEvent newEvent = new DefaultMuleEvent(msg, group.toArray()[0].getEndpoint(),
                                                                              new DefaultMuleSession(service, context), false);

                                    // TODO which use cases would need a sync reply event returned? 
                                    service.getComponent().invoke(newEvent);
                                }
                                catch (Exception e)
                                {
                                    service.getExceptionListener().exceptionThrown(e);
                                }
                            }
                        }
                    }
                    try
                    {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException e)
                    {
                        break;
                    }
                }
            }
        }

        );
    }

    /**
     * @return
     * @deprecated this is used by a test, but I would like to remove this method
     */
    public Map getResponseMessages()
    {
        return Collections.unmodifiableMap(responseMessages);
    }

    public MuleMessage process(MuleEvent event) throws RoutingException
    {
        addEvent(event);
        Object correlationId = messageInfoMapping.getCorrelationId(event.getMessage());
        if (locks.get(correlationId) != null)
        {
            locks.remove(correlationId);
            return (MuleMessage) responseMessages.remove(correlationId);
        }
        else
        {
            return null;
        }
    }

    public void addEvent(MuleEvent event) throws RoutingException
    {
        // the correlationId of the event's message
        final Object groupId = messageInfoMapping.getCorrelationId(event.getMessage());

        if (groupId == null || groupId.equals("-1"))
        {
            throw new RoutingException(CoreMessages.noCorrelationId(), event.getMessage(), event
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

            if (isGroupAlreadyProcessed(groupId))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("An event was received for an event group that has already been processed, " +
                            "this is probably because the async-reply timed out. Correlation Id is: " + groupId +
                            ". Dropping event");
                }
                //Fire a notification to say we received this message
                context.fireNotification(new RoutingNotification(event.getMessage(),
                        event.getEndpoint().getEndpointURI().toString(),
                        RoutingNotification.MISSED_ASYNC_REPLY));
                return;
            }
            // check for an existing group first
            EventGroup group = this.getEventGroup(groupId);

            // does the group exist?
            if (group == null)
            {
                // ..apparently not, so create a new one & add it
                group = this.addEventGroup(callback.createEventGroup(event, groupId));
            }

            // ensure that only one thread at a time evaluates this EventGroup
            // TODO this sync is useless (local var), need to lock on a field, possibly use lock striping
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
                    logger.debug("Adding event to aggregator group: " + groupId);
                }

                // add the incoming event to the group
                group.addEvent(event);

                // check to see if the event group is ready to be aggregated
                if (callback.shouldAggregateEvents(group))
                {
                    // create the response message
                    MuleMessage returnMessage = callback.aggregateEvents(group);

                    // remove the eventGroup as no further message will be received
                    // for this group once we aggregate
                    this.removeEventGroup(group);

                    // add the new response message so that it can be collected by
                    // the response Thread
                    MuleMessage previousResult = (MuleMessage) responseMessages.putIfAbsent(groupId,
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
                    Latch l = (Latch) locks.get(groupId);
                    if (l == null)
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Creating latch for " + groupId + " in " + this);
                        }

                        l = new Latch();
                        Latch previous = (Latch) locks.putIfAbsent(groupId, l);
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

    protected EventGroup getEventGroup(Object groupId)
    {
        return (EventGroup) eventGroups.get(groupId);
    }

    protected EventGroup addEventGroup(EventGroup group)
    {
        EventGroup previous = (EventGroup) eventGroups.putIfAbsent(group.getGroupId(), group);
        // a parallel thread might have removed the EventGroup already,
        // therefore we need to validate our current reference
        return (previous != null ? previous : group);
    }

    protected void removeEventGroup(EventGroup group)
    {
        final Object groupId = group.getGroupId();
        eventGroups.remove(groupId);
        addProcessedGroup(groupId);
    }

    protected void addProcessedGroup(Object id)
    {
        synchronized (groupsLock)
        {
            if (processedGroups.isFull())
            {
                processedGroups.remove();
            }
            processedGroups.add(id);
        }
    }

    protected boolean isGroupAlreadyProcessed(Object id)
    {
        synchronized (groupsLock)
        {
            return processedGroups.contains(id);
        }
    }

    /**
     * This method is called by the responding callee thread and should return the
     * aggregated response message
     *
     * @param message
     * @return
     * @throws RoutingException
     */
    public MuleMessage getResponse(MuleMessage message) throws RoutingException
    {
        return getResponse(message, getTimeout());
    }

    /**
     * This method is called by the responding callee thread and should return the
     * aggregated response message
     *
     * @param message
     * @return
     * @throws RoutingException
     */
    public MuleMessage getResponse(MuleMessage message, int timeout) throws RoutingException
    {
        Object responseId = messageInfoMapping.getMessageId(message);

        if (logger.isDebugEnabled())
        {
            logger.debug("Waiting for response for message id: " + responseId + " in " + this);
        }

        Latch l = (Latch) locks.get(responseId);
        if (l == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Got response but no one is waiting for it yet. Creating latch for "
                        + responseId + " in " + this);
            }

            l = new Latch();
            Latch previous = (Latch) locks.putIfAbsent(responseId, l);
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
        MuleMessage result;

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
                resultAvailable = l.await(timeout, TimeUnit.MILLISECONDS);
            }
        }
        catch (InterruptedException e)
        {
            interruptedWhileWaiting = true;
        }
        finally
        {
            locks.remove(responseId);
            result = (MuleMessage) responseMessages.remove(responseId);

            if (interruptedWhileWaiting)
            {
                Thread.currentThread().interrupt();
            }
        }

        if (!resultAvailable)
        {
            if (isFailOnTimeout())
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace("Current responses are: \n" + MapUtils.toString(responseMessages, true));
                }
                context.fireNotification(new RoutingNotification(message, null,
                        RoutingNotification.ASYNC_REPLY_TIMEOUT));

                throw new ResponseTimeoutException(
                        CoreMessages.responseTimedOutWaitingForId(
                                this.getTimeout(), responseId), message, null);
            }
            else
            {
                EventGroup group = this.getEventGroup(responseId);
                if (group == null)
                {
                    //Unlikely this will ever happen
                    if (logger.isTraceEnabled())
                    {
                        logger.trace("There is no current event Group. Current responses are: \n" + MapUtils.toString(responseMessages, true));
                    }
                    return null;
                }
                else
                {
                    this.removeEventGroup(group);
                    // create the response message
                    MuleMessage msg = callback.aggregateEvents(group);
                    return msg;
                }
            }
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


    public boolean isFailOnTimeout()
    {
        return failOnTimeout;
    }

    public void setFailOnTimeout(boolean failOnTimeout)
    {
        this.failOnTimeout = failOnTimeout;
    }

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }
}
