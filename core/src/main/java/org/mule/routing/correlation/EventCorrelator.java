/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.correlation;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessageCollection;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.api.routing.RoutingException;
import org.mule.api.service.Service;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.RoutingNotification;
import org.mule.routing.EventGroup;
import org.mule.util.StringMessageUtils;
import org.mule.util.concurrent.ThreadNameHelper;
import org.mule.util.monitor.Expirable;
import org.mule.util.monitor.ExpiryMonitor;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.apache.commons.collections.buffer.BoundedFifoBuffer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 */
public class EventCorrelator implements Startable, Stoppable
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(EventCorrelator.class);

    public static final String NO_CORRELATION_ID = "no-id";

    public static final int MAX_PROCESSED_GROUPS = 50000;

    protected static final long MILLI_TO_NANO_MULTIPLIER = 1000000L;

    private static final long ONE_DAY_IN_MILLI = 1000 * 60 * 60 * 24;

    protected long groupTimeToLive = ONE_DAY_IN_MILLI;

    /**
     * A map of EventGroup objects. These represent one or more messages to be
     * agregated, keyed by message id. There will be one response message for every
     * EventGroup.
     */
    protected final ConcurrentMap eventGroups = new ConcurrentHashMap();

    protected final Object groupsLock = new Object();

    // @GuardedBy groupsLock
    protected final BoundedFifoBuffer processedGroups = new BoundedFifoBuffer(MAX_PROCESSED_GROUPS);

    private long timeout = -1; // undefined

    private boolean failOnTimeout = true;

    private MessageInfoMapping messageInfoMapping;

    private MuleContext muleContext;

    private EventCorrelatorCallback callback;

    private MessageProcessor timeoutMessageProcessor;

    /**
     * A map with keys = group id and values = group creation time
     */
    private Map expiredAndDispatchedGroups = new ConcurrentHashMap();

    private EventCorrelator.ExpiringGroupMonitoringThread expiringGroupMonitoringThread;
    private final String name;

    public EventCorrelator(EventCorrelatorCallback callback, MessageProcessor timeoutMessageProcessor, MessageInfoMapping messageInfoMapping, MuleContext muleContext, String flowConstructName)
    {
        if (callback == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("EventCorrelatorCallback").getMessage());
        }
        if (messageInfoMapping == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("MessageInfoMapping").getMessage());
        }
        if (muleContext == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("MuleContext").getMessage());
        }
        this.callback = callback;
        this.messageInfoMapping = messageInfoMapping;
        this.muleContext = muleContext;
        this.timeoutMessageProcessor = timeoutMessageProcessor;
        name = String.format("%s%s.event.correlator", ThreadNameHelper.getPrefix(muleContext), flowConstructName);
    }

    public void forceGroupExpiry(String groupId)
    {
        if (eventGroups.get(groupId) != null)
        {
            handleGroupExpiry((EventGroup) eventGroups.get(groupId));
        }
        else
        {
            addProcessedGroup(groupId);
        }
    }

    public MuleEvent process(MuleEvent event) throws RoutingException
    {
        // the correlationId of the event's message
        final String groupId = messageInfoMapping.getCorrelationId(event.getMessage());

        if (logger.isTraceEnabled())
        {
            try
            {
                logger.trace(String.format("Received async reply message for correlationID: %s%n%s%n%s",
                                           groupId,
                                           StringMessageUtils.truncate(StringMessageUtils.toString(event.getMessage().getPayload()), 200, false),
                                           StringMessageUtils.headersToString(event.getMessage())));
            }
            catch (Exception e)
            {
                // ignore
            }
        }
        if (groupId == null || groupId.equals("-1"))
        {
            throw new RoutingException(CoreMessages.noCorrelationId(), event, timeoutMessageProcessor);
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
                muleContext.fireNotification(new RoutingNotification(event.getMessage(),
                                                                     event.getEndpoint().getEndpointURI().toString(),
                                                                     RoutingNotification.MISSED_AGGREGATION_GROUP_EVENT));
                return null;
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
            synchronized (groupsLock)
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
                    // create the response event
                    MuleEvent returnEvent = callback.aggregateEvents(group);
                    returnEvent.getMessage().setCorrelationId(groupId);

                    // remove the eventGroup as no further message will be received
                    // for this group once we aggregate
                    this.removeEventGroup(group);

                    return returnEvent;
                }
                else
                {
                    return null;
                }
            }
        }
    }

    protected EventGroup getEventGroup(String groupId)
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

    public boolean isFailOnTimeout()
    {
        return failOnTimeout;
    }

    public void setFailOnTimeout(boolean failOnTimeout)
    {
        this.failOnTimeout = failOnTimeout;
    }

    public long getTimeout()
    {
        return timeout;
    }

    public void setTimeout(long timeout)
    {
        this.timeout = timeout;
    }

    protected void handleGroupExpiry(EventGroup group)
    {
        removeEventGroup(group);

        final FlowConstruct service = group.toArray()[0].getFlowConstruct();

        if (isFailOnTimeout())
        {
            final MuleMessageCollection messageCollection = group.toMessageCollection();
            muleContext.fireNotification(new RoutingNotification(messageCollection, null,
                                                                 RoutingNotification.CORRELATION_TIMEOUT));
            service.getExceptionListener().handleException(
                    new CorrelationTimeoutException(CoreMessages.correlationTimedOut(group.getGroupId()),
                                                    group.getMessageCollectionEvent()), group.getMessageCollectionEvent());
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
                if (!(group.getCreated() + groupTimeToLive < System.currentTimeMillis()))
                {
                    MuleEvent newEvent = callback.aggregateEvents(group);
                    newEvent.getMessage().setCorrelationId(group.getGroupId().toString());


                    if (!expiredAndDispatchedGroups.containsKey(group.getGroupId()))
                    {
                        // TODO which use cases would need a sync reply event returned?
                        if (timeoutMessageProcessor != null)
                        {
                            timeoutMessageProcessor.process(newEvent);
                        }
                        else
                        {
                            if (!(service instanceof Service))
                            {
                                throw new UnsupportedOperationException("EventAggregator is only supported with Service");
                            }

                            ((Service) service).dispatchEvent(newEvent);
                        }
                        expiredAndDispatchedGroups.put(group.getGroupId(),
                                                       group.getCreated());
                    }
                    else
                    {
                        logger.warn(MessageFormat.format("Discarding group {0}", group.getGroupId()));
                    }
                }
            }
            catch (Exception e)
            {
                service.getExceptionListener().handleException(e, group.getMessageCollectionEvent());
            }
        }
    }

    public void start() throws MuleException
    {
        logger.info("Starting event correlator: " + name);
        if (timeout != 0)
        {
            expiringGroupMonitoringThread = new ExpiringGroupMonitoringThread();
            expiringGroupMonitoringThread.start();
        }
    }

    public void stop() throws MuleException
    {
        logger.info("Stopping event correlator: " + name);
        if (expiringGroupMonitoringThread != null)
        {
            expiringGroupMonitoringThread.stopProcessing();
        }
    }


    private final class ExpiringGroupMonitoringThread extends Thread implements Expirable
    {
        private ExpiryMonitor expiryMonitor;

        private volatile boolean stopRequested;

        public ExpiringGroupMonitoringThread()
        {
            setName(name);
            this.expiryMonitor = new ExpiryMonitor(name, 1000 * 60);
            //clean up every 30 minutes
            this.expiryMonitor.addExpirable(1000 * 60 * 30, TimeUnit.MILLISECONDS, this);
        }

        /**
         * Removes the elements in expiredAndDispatchedGroups when groupLife is reached
         */
        public void expired()
        {
            for (Object o : expiredAndDispatchedGroups.keySet())
            {
                Long time = (Long) expiredAndDispatchedGroups.get(o);
                if (time + groupTimeToLive < System.currentTimeMillis())
                {
                    expiredAndDispatchedGroups.remove(o);
                    logger.warn(MessageFormat.format("Discarding group {0}", o));
                }
            }
        }

        public void release()
        {
            //no op
        }

        public void run()
        {
            while (true)
            {
                if (stopRequested)
                {
                    logger.debug("Received request to stop expiring group monitoring");
                    break;
                }

                List<EventGroup> expired = new ArrayList<EventGroup>(1);
                for (Object o : eventGroups.values())
                {
                    EventGroup group = (EventGroup) o;
                    if ((group.getCreated() + getTimeout() * MILLI_TO_NANO_MULTIPLIER) < System.nanoTime())
                    {
                        expired.add(group);
                    }
                }
                if (expired.size() > 0)
                {
                    for (Object anExpired : expired)
                    {
                        EventGroup group = (EventGroup) anExpired;
                        handleGroupExpiry(group);
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

            logger.debug("Expiring group monitoring fully stopped");
        }

        /**
         * Stops the monitoring of the expired groups.
         */
        public void stopProcessing()
        {
            logger.debug("Stopping expiring group monitoring");
            stopRequested = true;

            try
            {
                this.join();
            }
            catch (InterruptedException e)
            {
                // Ignoring
            }
        }
    }
}
