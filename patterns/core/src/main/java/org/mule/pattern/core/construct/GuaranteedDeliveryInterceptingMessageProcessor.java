/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.pattern.core.construct;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.resource.spi.work.Work;

import org.apache.commons.lang.StringUtils;
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.config.i18n.MessageFactory;
import org.mule.processor.AbstractInterceptingMessageProcessor;

/**
 * An intercepting message processor that can re-try processing an event until it is accepted. It relies on an injected
 * policy for configuring its behavior.
 */
public class GuaranteedDeliveryInterceptingMessageProcessor extends AbstractInterceptingMessageProcessor
    implements Initialisable, FlowConstructAware
{
    /**
     * Defines the behavior of the {@link GuaranteedDeliveryInterceptingMessageProcessor} so the same configuration can
     * be applied to several instances of the MP.
     */
    public static class GuaranteedDeliveryPolicy
    {
        private ListableObjectStore<MuleEvent> objectStore;
        private int maxDeliveryAttempts = 5;
        private long millisecondsBetweenDeliveries = 10000L;

        public ListableObjectStore<MuleEvent> getObjectStore()
        {
            return objectStore;
        }

        public void setObjectStore(final ListableObjectStore<MuleEvent> objectStore)
        {
            this.objectStore = objectStore;
        }

        public int getMaxDeliveryAttempts()
        {
            return maxDeliveryAttempts;
        }

        public void setMaxDeliveryAttempts(final int maxDeliveryAttempts)
        {
            this.maxDeliveryAttempts = maxDeliveryAttempts;
        }

        public long getMillisecondsBetweenDeliveries()
        {
            return millisecondsBetweenDeliveries;
        }

        public void setMillisecondsBetweenDeliveries(final long millisecondsBetweenDeliveries)
        {
            this.millisecondsBetweenDeliveries = millisecondsBetweenDeliveries;
        }
    }

    /**
     * Process a pending event, dealing with failures and rescheduling.
     */
    private class PendingEventWorker implements Work
    {
        private final MuleEvent event;

        private PendingEventWorker(final MuleEvent event)
        {
            this.event = event;
        }

        public void run()
        {
            try
            {
                processNext(event);
                // TODO (DDO) consider adding a result validating expression to allow rejecting processes that don't
                // fail with an exception
                deschedule(event);
            }
            catch (final Throwable t)
            {
                try
                {
                    final boolean successfullyScheduled = scheduleForProcessing(event);
                    if (successfullyScheduled)
                    {
                        logger.warn(
                            "Event processing raised an exception, redelivery will be attempted again: "
                                            + event, t);
                    }
                    else
                    {
                        logger.error(
                            "Event processing raised an exception, redelivery has failed to many times and is aborted: "
                                            + event, t);
                    }
                }
                catch (final ObjectStoreException ose)
                {
                    logger.error("Error when dealing with the object store while processing event: " + event,
                        ose);
                }
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
    private class PendingEventsScheduler implements Callable<Void>
    {
        public Void call() throws Exception
        {
            for (final Serializable key : guaranteedDeliveryPolicy.getObjectStore().allKeys())
            {
                if (isOwnedStoreKey((String) key))
                {
                    final MuleEvent event = guaranteedDeliveryPolicy.getObjectStore().retrieve(key);

                    if (isDueForRedelivery(event))
                    {
                        getMuleContext().getWorkManager().scheduleWork(new PendingEventWorker(event));
                    }
                }
            }
            return null;
        }
    }

    public static final String DELIVERY_ATTEMPT_COUNT_PROPERTY_NAME = "delivery.attempt.count";
    public static final String NEXT_DELIVERY_ATTEMPT_TIME_PROPERTY_NAME = "delivery.next.attempt.time";

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private FlowConstruct flowConstruct;
    private GuaranteedDeliveryPolicy guaranteedDeliveryPolicy;
    private String eventKeyPrefix;

    public void initialise() throws InitialisationException
    {
        eventKeyPrefix = getFlowConstruct().getName() + "@" + muleContext.getConfiguration().getClusterId()
                         + ":";

        executor.schedule(new PendingEventsScheduler(),
            guaranteedDeliveryPolicy.getMillisecondsBetweenDeliveries(), TimeUnit.MILLISECONDS);
    }

    public MuleEvent process(final MuleEvent event) throws MuleException
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

        scheduleForProcessing(event);

        // no response is expected from this MP
        return null;
    }

    private boolean scheduleForProcessing(final MuleEvent event) throws ObjectStoreException
    {
        final MuleMessage message = event.getMessage();

        final Integer deliveryAttemptCount = message.getInvocationProperty(
            DELIVERY_ATTEMPT_COUNT_PROPERTY_NAME, 0);

        if (deliveryAttemptCount >= getGuaranteedDeliveryPolicy().getMaxDeliveryAttempts())
        {
            deschedule(event);
            return false;
        }

        if (deliveryAttemptCount > 0)
        {
            final long nextDeliveryAttemptTime = System.currentTimeMillis()
                                                 + getGuaranteedDeliveryPolicy().getMillisecondsBetweenDeliveries();
            event.getMessage().setInvocationProperty(NEXT_DELIVERY_ATTEMPT_TIME_PROPERTY_NAME,
                nextDeliveryAttemptTime);
        }

        message.setInvocationProperty(DELIVERY_ATTEMPT_COUNT_PROPERTY_NAME, deliveryAttemptCount + 1);
        getGuaranteedDeliveryPolicy().getObjectStore().store(getStoreKey(event), event);
        return true;
    }

    private boolean isDueForRedelivery(final MuleEvent event)
    {
        final Long nextDeliveryAttemptTime = event.getMessage().getInvocationProperty(
            NEXT_DELIVERY_ATTEMPT_TIME_PROPERTY_NAME, 0L);

        return System.currentTimeMillis() >= nextDeliveryAttemptTime;
    }

    private void deschedule(final MuleEvent event) throws ObjectStoreException
    {
        getGuaranteedDeliveryPolicy().getObjectStore().remove(getStoreKey(event));
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

    public void setGuaranteedDeliveryPolicy(final GuaranteedDeliveryPolicy guaranteedDeliveryPolicy)
    {
        this.guaranteedDeliveryPolicy = guaranteedDeliveryPolicy;
    }

    public GuaranteedDeliveryPolicy getGuaranteedDeliveryPolicy()
    {
        return guaranteedDeliveryPolicy;
    }

    public void setFlowConstruct(final FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    public FlowConstruct getFlowConstruct()
    {
        return flowConstruct;
    }
}
