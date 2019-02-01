/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.correlation;

import static org.mule.MessageExchangePattern.ONE_WAY;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessageCollection;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.api.routing.RoutingException;
import org.mule.api.service.Service;
import org.mule.api.store.ObjectAlreadyExistsException;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.PartitionableObjectStore;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.RoutingNotification;
import org.mule.execution.ErrorHandlingExecutionTemplate;
import org.mule.routing.EventGroup;
import org.mule.routing.EventProcessingThread;
import org.mule.transport.NullPayload;
import org.mule.util.StringMessageUtils;
import org.mule.util.concurrent.ThreadNameHelper;
import org.mule.util.monitor.Expirable;
import org.mule.util.monitor.ExpiryMonitor;
import org.mule.util.store.DeserializationPostInitialisable;

public class EventCorrelator implements Startable, Stoppable, Disposable
{

    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(EventCorrelator.class);

    public static final String NO_CORRELATION_ID = "no-id";

    private static final long ONE_DAY_IN_MILLI = 1000 * 60 * 60 * 24;

    protected long groupTimeToLive = ONE_DAY_IN_MILLI;

    protected final Object groupsLock = new Object();

    // @GuardedBy groupsLock
    protected ObjectStore<Long> processedGroups = null;

    private long timeout = -1; // undefined

    private boolean failOnTimeout = true;

    private MessageInfoMapping messageInfoMapping;

    private MuleContext muleContext;

    private EventCorrelatorCallback callback;

    private MessageProcessor timeoutMessageProcessor;


    /**
     * A map of EventGroup objects in a partition. These represent one or more messages to be agregated, keyed by
     * message id. There will be one response message for every EventGroup.
     */
    private PartitionableObjectStore correlatorStore = null;
    private String storePrefix;

    private EventCorrelator.ExpiringGroupMonitoringThread expiringGroupMonitoringThread;
    private final String name;

    private final FlowConstruct flowConstruct;
    
    private volatile Lock lock;

    public EventCorrelator(EventCorrelatorCallback callback,
                           MessageProcessor timeoutMessageProcessor,
                           MessageInfoMapping messageInfoMapping,
                           MuleContext muleContext,
                           FlowConstruct flowConstruct,
                           PartitionableObjectStore correlatorStore,
                           String storePrefix,
                           ObjectStore<Long> processedGroups)
    {
        if (callback == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("EventCorrelatorCallback")
                                                       .getMessage());
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
        name = String.format("%s%s.event.correlator", ThreadNameHelper.getPrefix(muleContext),
                             flowConstruct.getName());
        this.flowConstruct = flowConstruct;

        this.correlatorStore = correlatorStore;
        this.storePrefix = storePrefix;
        this.processedGroups = processedGroups;
    }

    public void forceGroupExpiry(String groupId) throws MessagingException
    {
        try
        {
            if (correlatorStore.retrieve(groupId, getEventGroupsPartitionKey()) != null)
            {
                handleGroupExpiry(getEventGroup(groupId));
            }
            else
            {
                addProcessedGroup(groupId);
            }
        }
        catch (ObjectStoreException e)
        {
            // TODO improve this
            throw new MessagingException(null, e);
        }
    }

    public MuleEvent process(MuleEvent event) throws RoutingException
    {
        // the correlationId of the event's message
        String groupId = messageInfoMapping.getCorrelationId(event.getMessage());

        if (logger.isTraceEnabled())
        {
            try
            {
                logger.trace(String.format("Received async reply message for correlationID: %s%n%s%n%s",
                                           groupId, StringMessageUtils.truncate(
                        StringMessageUtils.toString(event.getMessage().getPayload()), 200, false),
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

        // spinloop for the EventGroup lookup
        while (true)
        {
            // check for an existing group first
            EventGroup group;
            try
            {
                group = this.getEventGroup(groupId);
            }
            catch (ObjectStoreException e)
            {
                throw new RoutingException(event, timeoutMessageProcessor, e);
            }
            
            // This verification has to be here to verify that after retrieval of the group
            // no invalid storage of the group in the correlation map is performed.
            try
            {
                if (isGroupAlreadyProcessed(groupId))
                {
                    fireMissedAggregationGroupEvent(event, groupId);
                    return null;
                }
            }
            catch (ObjectStoreException e)
            {
                throw new RoutingException(event, timeoutMessageProcessor, e);
            }

            // does the group exist?
            if (group == null)
            {
                // ..apparently not, so create a new one & add it
                try
                {
                    EventGroup eventGroup = callback.createEventGroup(event, groupId);
                    eventGroup.initEventsStore(correlatorStore);
                    group = this.addEventGroup(eventGroup);
                }
                catch (ObjectStoreException e)
                {
                    throw new RoutingException(event, timeoutMessageProcessor, e);
                }
            }

            // ensure that only one thread at a time evaluates this EventGroup
            try
            {
                getLock().lock();
                if (logger.isDebugEnabled())
                {
                    logger.debug("Adding event to aggregator group: " + groupId);
                }

                // add the incoming event to the group
                try
                {
                    event.getMessage().setCorrelationId(groupId + event.getMessageSourceName());
                    group.addEvent(event);
                }
                catch (ObjectStoreException e)
                {
                    throw new RoutingException(event, timeoutMessageProcessor, e);
                }

                // check to see if the event group is ready to be aggregated
                if (callback.shouldAggregateEvents(group))
                {
                    // create the response event
                    MuleEvent returnEvent = null;
                    try
                    {
                        returnEvent = callback.aggregateEvents(group);
                    }
                    catch (RoutingException routingException)
                    {
                        try
                        {
                            this.removeEventGroup(group);
                            group.clear();
                        }
                        catch (ObjectStoreException objectStoreException)
                        {
                            throw new RoutingException(event, timeoutMessageProcessor, objectStoreException);
                        }

                        throw routingException;
                    }
                    if (returnEvent != null && !returnEvent.equals(VoidMuleEvent.getInstance()))
                    {

                        returnEvent.getMessage().setCorrelationId(groupId);

                        String rootId = group.getCommonRootId();
                        if (rootId != null)
                        {
                            returnEvent.getMessage().setMessageRootId(rootId);
                        }
                    }

                    // remove the eventGroup as no further message will be received
                    // for this group once we aggregate
                    try
                    {

                        this.removeEventGroup(group);
                        group.clear();
                    }
                    catch (ObjectStoreException e)
                    {
                        throw new RoutingException(event, timeoutMessageProcessor, e);
                    }
                    return returnEvent;
                }
                else
                {
                    return null;
                }
            }
            finally
            {
                getLock().unlock();
            }
        }
    }

    private void fireMissedAggregationGroupEvent(MuleEvent event, final String groupId)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("An event was received for an event group that has already been processed, "
                         + "this is probably because the async-reply timed out. Correlation Id is: "
                         + groupId + ". Dropping event");
        }
        // Fire a notification to say we received this message
        muleContext.fireNotification(new RoutingNotification(event.getMessage(),
                                                             event.getMessageSourceURI().toString(),
                                                             RoutingNotification.MISSED_AGGREGATION_GROUP_EVENT));
    }

    protected EventGroup getEventGroup(Serializable groupId) throws ObjectStoreException
    {
        try
        {
            getLock().lock();
            return doGetEventGroup(groupId);
        }
        finally
        {
            getLock().unlock();
        }
    }

    private EventGroup doGetEventGroup(Serializable groupId) throws ObjectStoreException
    {
        try
        {
            EventGroup eventGroup = (EventGroup) correlatorStore.retrieve(groupId, getEventGroupsPartitionKey());
            if (!eventGroup.isInitialised())
            {
                try
                {
                    DeserializationPostInitialisable.Implementation.init(eventGroup, muleContext);
                }
                catch (Exception e)
                {
                    throw new ObjectStoreException(e);
                }
            }
            eventGroup.initEventsStore(correlatorStore);
            return eventGroup;
        }
        catch (ObjectDoesNotExistException e)
        {
            return null;
        }
    }

    protected EventGroup addEventGroup(EventGroup group) throws ObjectStoreException
    {
        try
        {
            correlatorStore.store((Serializable) group.getGroupId(), group, getEventGroupsPartitionKey());
            return group;
        }
        catch (ObjectAlreadyExistsException e)
        {
            return getEventGroup((String) group.getGroupId());
        }
    }

    protected void removeEventGroup(EventGroup group) throws ObjectStoreException
    {
        final Object groupId = group.getGroupId();
        try
        {
            getLock().lock();
            if (!isGroupAlreadyProcessed(groupId))
            {
                correlatorStore.remove((Serializable) groupId, getEventGroupsPartitionKey());
                addProcessedGroup(groupId);
            }
        }
        finally
        {
            getLock().unlock();
        }
    }

    protected void addProcessedGroup(Object id) throws ObjectStoreException
    {
        try
        {
            getLock().lock();
            processedGroups.store((Serializable) id, System.currentTimeMillis());
        }
        finally
        {
            getLock().unlock();
        }
    }

    protected boolean isGroupAlreadyProcessed(Object id) throws ObjectStoreException
    {
        try
        {
            getLock().lock();
            return processedGroups.contains((Serializable) id);
        }
        finally
        {
            getLock().unlock();
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

    protected void handleGroupExpiry(EventGroup group) throws MessagingException
    {
        try
        {
            removeEventGroup(group);
        }
        catch (ObjectStoreException e)
        {
            throw new MessagingException(group.getMessageCollectionEvent(), e);
        }

        if (isFailOnTimeout())
        {
            MuleMessageCollection messageCollection;
            try
            {
                messageCollection = group.toMessageCollection();
            }
            catch (ObjectStoreException e)
            {
                throw new MessagingException(group.getMessageCollectionEvent(), e);
            }
            muleContext.fireNotification(new RoutingNotification(messageCollection, null,
                                                                 RoutingNotification.CORRELATION_TIMEOUT));
            MuleEvent groupCollectionEvent = group.getMessageCollectionEvent();
            try
            {
                group.clear();
            }
            catch (ObjectStoreException e)
            {
                logger.warn("Failed to clear group with id " + group.getGroupId()
                            + " since underlying ObjectStore threw Exception:" + e.getMessage());
            }
            
            // This is intended to avoid an error in the handling of the correlation exception
            // in case no event group was created before the timeout (no message for aggregate
            // were received yet). A "custom" mule event is created so that a message is available.
            if (groupCollectionEvent instanceof VoidMuleEvent)
            {
                groupCollectionEvent =
                        new DefaultMuleEvent(new DefaultMuleMessage(NullPayload.getInstance(), muleContext), ONE_WAY, flowConstruct);
            }
            
            throw new CorrelationTimeoutException(CoreMessages.correlationTimedOut(group.getGroupId()), groupCollectionEvent);
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(MessageFormat.format(
                        "Aggregator expired, but ''failOnTimeOut'' is false. Forwarding {0} events out of {1} "
                        + "total for group ID: {2}", group.size(), group.expectedSize(),
                        group.getGroupId()));
            }

            try
            {
                if (!(group.getCreated() + groupTimeToLive < System.currentTimeMillis()))
                {
                    MuleEvent newEvent = callback.aggregateEvents(group);
                    group.clear();
                    newEvent.getMessage().setCorrelationId(group.getGroupId().toString());

                    if (!correlatorStore.contains((Serializable) group.getGroupId(), getExpiredAndDispatchedPartitionKey()))
                    {
                        // TODO which use cases would need a sync reply event
                        // returned?
                        if (timeoutMessageProcessor != null)
                        {
                            timeoutMessageProcessor.process(newEvent);
                        }
                        else
                        {
                            final FlowConstruct service = group.toArray(false)[0].getFlowConstruct();
                            if (!(service instanceof Service))
                            {
                                throw new UnsupportedOperationException(
                                        "EventAggregator is only supported with Service");
                            }

                            ((Service) service).dispatchEvent(newEvent);
                        }
                        correlatorStore.store((Serializable) group.getGroupId(),
                                                         group.getCreated(),
                                                         getExpiredAndDispatchedPartitionKey());
                    }
                    else
                    {
                        logger.warn(MessageFormat.format("Discarding group {0}", group.getGroupId()));
                    }
                }
            }
            catch (MessagingException me)
            {
                throw me;
            }
            catch (Exception e)
            {
                throw new MessagingException(group.getMessageCollectionEvent(), e);
            }
        }
    }

    @Override
    public void start() throws MuleException
    {
        logger.info("Starting event correlator: " + name);
        if (timeout != 0)
        {
            expiringGroupMonitoringThread = new ExpiringGroupMonitoringThread();
            expiringGroupMonitoringThread.start();
        }
    }

    @Override
    public void stop() throws MuleException
    {
        logger.info("Stopping event correlator: " + name);
        if (expiringGroupMonitoringThread != null)
        {
            expiringGroupMonitoringThread.stopProcessing();
        }
    }

    private final class ExpiringGroupMonitoringThread extends EventProcessingThread implements Expirable, Disposable
    {

        private ExpiryMonitor expiryMonitor;
        public static final long DELAY_TIME = 10;

        public ExpiringGroupMonitoringThread()
        {
            super(name, DELAY_TIME);
            this.expiryMonitor = new ExpiryMonitor(name, 1000 * 60, muleContext, true);
            // clean up every 30 minutes
            this.expiryMonitor.addExpirable(1000 * 60 * 30, TimeUnit.MILLISECONDS, this);
        }

        /**
         * Removes the elements in expiredAndDispatchedGroups when groupLife is
         * reached
         *
         * @throws ObjectStoreException
         */
        @Override
        public void expired()
        {
            try
            {
                for (Serializable o : (List<Serializable>) correlatorStore.allKeys(getExpiredAndDispatchedPartitionKey()))
                {
                    Long time = (Long) correlatorStore.retrieve(o, getExpiredAndDispatchedPartitionKey());
                    if (time + groupTimeToLive < System.currentTimeMillis())
                    {
                        correlatorStore.remove(o, getExpiredAndDispatchedPartitionKey());
                        logger.warn(MessageFormat.format("Discarding group {0}", o));
                    }
                }
            }
            catch (ObjectStoreException e)
            {
                logger.warn("Expiration of objects failed due to ObjectStoreException " + e + ".");
            }
        }

        @Override
        public void doRun()
        {

            ////TODO(pablo.kraan): is not good to have threads doing nothing in all the nodes but the primary. Need to
            ////start the thread on the primary node only, and then use a notification schema to start a new thread
            ////in a different node when the primary goes down.
            if (!muleContext.isPrimaryPollingInstance())
            {
                return;
            }

            List<EventGroup> expired = new ArrayList<EventGroup>(1);
            try
            {
                for (Serializable o : (List<Serializable>) correlatorStore.allKeys(getEventGroupsPartitionKey()))
                {
                    EventGroup group = getEventGroup(o);
                    // group may have been removed by another thread right after eventGroups.allKeys()
                    if (group != null && group.getCreated() + getTimeout() < System.currentTimeMillis())
                    {
                        expired.add(group);
                    }
                }
            }
            catch (ObjectStoreException e)
            {
                logger.warn("expiry failed dues to ObjectStoreException " + e);
            }
            for (final EventGroup group : expired)
            {
                ExecutionTemplate<MuleEvent> executionTemplate = ErrorHandlingExecutionTemplate.createErrorHandlingExecutionTemplate(muleContext, flowConstruct.getExceptionListener());
                try
                {
                    executionTemplate.execute(new ExecutionCallback<MuleEvent>()
                    {
                        @Override
                        public MuleEvent process() throws Exception
                        {
                            handleGroupExpiry(group);
                            return null;
                        }
                    });
                }
                catch (MessagingException e)
                {
                    // Already handled by TransactionTemplate
                }
                catch (Exception e)
                {
                    muleContext.getExceptionListener().handleException(e);
                }
            }
        }

        @Override
        public void dispose()
        {
            if (expiryMonitor != null)
            {
                expiryMonitor.dispose();
            }
        }
    }

    protected String getExpiredAndDispatchedPartitionKey()
    {
        return storePrefix + ".expiredAndDispatchedGroups";
    }

    protected String getEventGroupsPartitionKey()
    {
        return storePrefix + ".eventGroups";
    }

    @Override
    public void dispose()
    {
        disposeIfDisposable(expiringGroupMonitoringThread);
    }

    private void disposeIfDisposable(Object o)
    {
        if (o != null && o instanceof Disposable)
        {
            ((Disposable) o).dispose();
        }
    }
    
    private Lock getLock()
    {
        if (lock == null)
        {
            synchronized (this)
            {
                if (lock == null)
                {
                    lock = muleContext.getLockFactory().createLock("agregator_" + flowConstruct.getName());
                }
            }
        }
        
        return lock;
    }
}
