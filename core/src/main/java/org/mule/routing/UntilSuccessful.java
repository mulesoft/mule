/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import org.apache.commons.lang.StringUtils;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.config.i18n.MessageFactory;
import org.mule.routing.filters.ExpressionFilter;
import org.mule.routing.outbound.AbstractOutboundRouter;

/**
 * UntilSuccessful attempts to route a message to the message processor it contains in an asynchronous manner. Routing
 * is considered successful if no exception has been raised and, optionally, if the response matches an expression.
 * UntilSuccessful can optionally be configured to synchronously return an acknowledgment message when it has scheduled
 * the event for processing. UntilSuccessful is backed by a {@link ListableObjectStore} for storing the events that are
 * pending (re)processing.
 */
public class UntilSuccessful extends AbstractOutboundRouter
{
    /**
     * Process a pending event, dealing with failures and rescheduling.
     */
    private class PendingEventWorker implements Work
    {
        private final MuleEvent event;

        private PendingEventWorker(final MuleEvent event)
        {
            this.event = threadSafeCopy(event);
        }

        private synchronized String acquireMutex()
        {
            String eventId = event.getId();
            if (eventProcessMutex.contains(eventId))
            {
                return null;
            }
            eventProcessMutex.add(eventId);
            return eventId;
        }

        public void run()
        {
            String mutex = acquireMutex();
            if (mutex == null)
            {
                // processing is already under way for this event
                return;
            }

            try
            {
                if (processEvent())
                {
                    deschedule(event);
                }
                else
                {
                    final boolean successfullyScheduled = scheduleForProcessing(event);
                    if (successfullyScheduled)
                    {
                        logger.warn("Reprocessing will be attempted again for event: " + event);
                    }
                    else
                    {
                        logger.error("Reprocessing has failed too many times and is aborted, droping event: "
                                     + event);
                    }
                }
            }
            catch (final ObjectStoreException ose)
            {
                logger.error("Error when dealing with the object store while processing event: " + event, ose);
            }
            finally
            {
                eventProcessMutex.remove(mutex);
            }
        }

        private boolean processEvent()
        {
            try
            {
                MuleEvent returnEvent = routes.get(0).process(event);
                if (returnEvent == null)
                {
                    return true;
                }

                MuleMessage msg = returnEvent.getMessage();
                if (msg == null)
                {
                    logger.warn("No message found in response to processing, which is therefore considered failed for event: "
                                + event);
                    return false;
                }

                boolean errorDetected = failureExpressionFilter.accept(msg);
                if (errorDetected)
                {
                    logger.warn("Failure as been detected when processing event: " + event);
                }

                return !errorDetected;
            }
            catch (final Exception e)
            {
                logger.warn("Processing failed for event: " + event, e);
                return false;
            }
        }

        public void release()
        {
            // NOOP
        }
    }

    /**
     * Checks all the accumulated events and schedule for processing the ones that belong to this MP, if they're ripe
     * for reprocessing.
     */
    private class PendingEventsScheduler implements Runnable
    {
        public void run()
        {
            try
            {
                scheduleEventsRipeForProcessing();
            }
            catch (Throwable t)
            {
                logger.error("Failed to schedule events for processing", t);
            }
        }

        private void scheduleEventsRipeForProcessing() throws ObjectStoreException, WorkException
        {
            for (final Serializable key : objectStore.allKeys())
            {
                if (isOwnedStoreKey((String) key))
                {
                    final MuleEvent event = objectStore.retrieve(key);

                    if (isDueForRedelivery(event))
                    {
                        triggerProcessing(event);
                    }
                }
            }
        }
    }

    public static final String DELIVERY_ATTEMPT_COUNT_PROPERTY_NAME = "delivery.attempt.count";
    public static final String NEXT_DELIVERY_ATTEMPT_TIME_PROPERTY_NAME = "delivery.next.attempt.time";

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Set<String> eventProcessMutex = new CopyOnWriteArraySet<String>();
    private String eventKeyPrefix;
    private ExpressionFilter failureExpressionFilter;

    private ListableObjectStore<MuleEvent> objectStore;
    private int maxProcessingAttempts = 5;
    private long secondsBetweenProcessingAttempts = 60L;
    private String failureExpression;
    private String ackExpression;

    @Override
    public void initialise() throws InitialisationException
    {
        if (routes.isEmpty())
        {
            throw new InitialisationException(
                MessageFactory.createStaticMessage("One message processor must be configured within UntilSuccessful."),
                this);
        }

        if (routes.size() > 1)
        {
            throw new InitialisationException(
                MessageFactory.createStaticMessage("Only one message processor is allowed within UntilSuccessful."
                                                   + " Use a Processor Chain to group several message processors into one."),
                this);
        }

        if (objectStore == null)
        {
            throw new InitialisationException(
                MessageFactory.createStaticMessage("A ListableObjectStore must be configured on UntilSuccessful."),
                this);
        }

        super.initialise();

        if (failureExpression != null)
        {
            failureExpressionFilter = new ExpressionFilter(failureExpression);
        }
        else
        {
            failureExpressionFilter = new ExpressionFilter("exception-type:");
        }
        failureExpressionFilter.setMuleContext(muleContext);

        if ((ackExpression != null) && (!muleContext.getExpressionManager().isExpression(ackExpression)))
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("Invalid ackExpression: "
                                                                                 + ackExpression), this);
        }

        eventKeyPrefix = flowConstruct.getName() + "@" + muleContext.getConfiguration().getClusterId() + ":";
    }

    @Override
    public void start() throws MuleException
    {
        super.start();

        // if secondsBetweenDeliveries is small, thrashing on object store will be high
        executor.scheduleWithFixedDelay(new PendingEventsScheduler(), 0L, secondsBetweenProcessingAttempts,
            TimeUnit.SECONDS);
    }

    @Override
    public void stop() throws MuleException
    {
        super.stop();
        executor.shutdown();
    }

    public boolean isMatch(MuleMessage message) throws MuleException
    {
        return true;
    }

    @Override
    protected MuleEvent route(MuleEvent event) throws MessagingException
    {
        try
        {
            ensurePayloadSerializable(event);
        }
        catch (final Exception e)
        {
            throw new MessagingException(
                MessageFactory.createStaticMessage("Failed to prepare message for processing"), event, e);
        }

        try
        {
            scheduleForProcessing(event);
            triggerProcessing(event);

            if (ackExpression == null)
            {
                return null;
            }

            Object ackResponsePayload = muleContext.getExpressionManager().evaluate(ackExpression,
                event.getMessage());

            return new DefaultMuleEvent(new DefaultMuleMessage(ackResponsePayload, event.getMessage(),
                muleContext), event);
        }
        catch (Exception e)
        {
            throw new MessagingException(
                MessageFactory.createStaticMessage("Failed to schedule the event for processing"), event, e);
        }
    }

    private boolean scheduleForProcessing(final MuleEvent event) throws ObjectStoreException
    {
        final MuleMessage message = event.getMessage();

        final Integer deliveryAttemptCount = message.getInvocationProperty(
            DELIVERY_ATTEMPT_COUNT_PROPERTY_NAME, 0);

        if (deliveryAttemptCount >= maxProcessingAttempts)
        {
            deschedule(event);
            return false;
        }

        if (deliveryAttemptCount > 0)
        {
            final long nextDeliveryAttemptTime = System.currentTimeMillis()
                                                 + secondsBetweenProcessingAttempts;
            event.getMessage().setInvocationProperty(NEXT_DELIVERY_ATTEMPT_TIME_PROPERTY_NAME,
                nextDeliveryAttemptTime);
        }

        message.setInvocationProperty(DELIVERY_ATTEMPT_COUNT_PROPERTY_NAME, deliveryAttemptCount + 1);

        // store a non-thread owned version of the message
        MuleEvent storableEvent = threadSafeCopy(event);

        String storeKey = getStoreKey(storableEvent);
        if (objectStore.contains(storeKey))
        {
            objectStore.remove(storeKey);
        }
        objectStore.store(storeKey, storableEvent);
        return true;
    }

    private MuleEvent threadSafeCopy(final MuleEvent event)
    {
        DefaultMuleMessage storableMessage = new DefaultMuleMessage(event.getMessage());
        MuleEvent storableEvent = new DefaultMuleEvent(storableMessage, event);
        storableMessage.resetAccessControl();
        return storableEvent;
    }

    private boolean isDueForRedelivery(final MuleEvent event)
    {
        final Long nextDeliveryAttemptTime = event.getMessage().getInvocationProperty(
            NEXT_DELIVERY_ATTEMPT_TIME_PROPERTY_NAME, 0L);

        return System.currentTimeMillis() >= nextDeliveryAttemptTime;
    }

    private void triggerProcessing(final MuleEvent event) throws WorkException
    {
        getMuleContext().getWorkManager().scheduleWork(new PendingEventWorker(event));
    }

    private void deschedule(final MuleEvent event) throws ObjectStoreException
    {
        objectStore.remove(getStoreKey(event));
    }

    private String getStoreKey(final MuleEvent event)
    {
        return eventKeyPrefix + event.getId();
    }

    private boolean isOwnedStoreKey(final String eventKey)
    {
        return StringUtils.startsWith(eventKey, eventKeyPrefix);
    }

    private void ensurePayloadSerializable(final MuleEvent event) throws Exception
    {
        final MuleMessage message = event.getMessage();
        if (message instanceof DefaultMuleMessage)
        {
            if (((DefaultMuleMessage) message).isConsumable())
            {
                message.getPayloadAsBytes();
            }
        }
        else
        {
            message.getPayloadAsBytes();
        }
    }

    public ListableObjectStore<MuleEvent> getObjectStore()
    {
        return objectStore;
    }

    public void setObjectStore(final ListableObjectStore<MuleEvent> objectStore)
    {
        this.objectStore = objectStore;
    }

    public int getMaxProcessingAttempts()
    {
        return maxProcessingAttempts;
    }

    public void setMaxProcessingAttempts(int maxProcessingAttempts)
    {
        this.maxProcessingAttempts = maxProcessingAttempts;
    }

    public long getSecondsBetweenProcessingAttempts()
    {
        return secondsBetweenProcessingAttempts;
    }

    public void setSecondsBetweenProcessingAttempts(long secondsBetweenProcessingAttempts)
    {
        this.secondsBetweenProcessingAttempts = secondsBetweenProcessingAttempts;
    }

    public String getFailureExpression()
    {
        return failureExpression;
    }

    public void setFailureExpression(String failureExpression)
    {
        this.failureExpression = failureExpression;
    }

    public String getAckExpression()
    {
        return ackExpression;
    }

    public void setAckExpression(String ackExpression)
    {
        this.ackExpression = ackExpression;
    }

    protected ScheduledExecutorService getExecutor()
    {
        return executor;
    }

    protected String getEventKeyPrefix()
    {
        return eventKeyPrefix;
    }

    protected ExpressionFilter getFailureExpressionFilter()
    {
        return failureExpressionFilter;
    }
}
