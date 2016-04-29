/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.lifecycle.CreateException;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.ObjectUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * <code>AbstractPollingMessageReceiver</code> implements a base class for polling
 * message receivers. The receiver provides a {@link #poll()} method that implementations
 * must implement to execute their custom code. Note that the receiver will not poll if
 * the associated connector is not started.
 */
public abstract class AbstractPollingMessageReceiver extends AbstractMessageReceiver
{
    public static final long DEFAULT_POLL_FREQUENCY = 1000;
    public static final TimeUnit DEFAULT_POLL_TIMEUNIT = TimeUnit.MILLISECONDS;

    public static final long DEFAULT_STARTUP_DELAY = 1000;

    private long frequency = DEFAULT_POLL_FREQUENCY;
    private TimeUnit timeUnit = DEFAULT_POLL_TIMEUNIT;

    // @GuardedBy(itself)
    protected final Map<ScheduledFuture, PollingReceiverWorker> schedules = new HashMap<ScheduledFuture, PollingReceiverWorker>();

    public AbstractPollingMessageReceiver(Connector connector,
                                          FlowConstruct flowConstruct,
                                          final InboundEndpoint endpoint) throws CreateException
    {
        super(connector, flowConstruct, endpoint);
    }

    @Override
    protected void doStart() throws MuleException
    {
        try
        {
            this.schedule();
        }
        catch (Exception ex)
        {
            this.stop();
            throw new CreateException(CoreMessages.failedToScheduleWork(), ex, this);
        }
    }

    @Override
    protected void doStop() throws MuleException
    {
        super.doStop();
        this.unschedule();
    }

    /**
     * This method registers this receiver for periodic polling ticks with the connectors
     * scheduler. Subclasses can override this in case they want to handle their polling
     * differently.
     *
     * @throws RejectedExecutionException
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @see ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)
     */
    protected void schedule()
            throws RejectedExecutionException, NullPointerException, IllegalArgumentException
    {
        synchronized (schedules)
        {
            // we use scheduleWithFixedDelay to prevent queue-up of tasks when
            // polling takes longer than the specified frequency, e.g. when the
            // polled database or network is slow or returns large amounts of
            // data.
            PollingReceiverWorker pollingReceiverWorker = this.createWork();
            ScheduledFuture schedule = connector.getScheduler().scheduleWithFixedDelay(
                    new PollingReceiverWorkerSchedule(pollingReceiverWorker), DEFAULT_STARTUP_DELAY,
                    this.getFrequency(), this.getTimeUnit());
            schedules.put(schedule, pollingReceiverWorker);

            if (logger.isDebugEnabled())
            {
                logger.debug(ObjectUtils.identityToShortString(this) + " scheduled "
                             + ObjectUtils.identityToShortString(schedule) + " with " + frequency
                             + " " + getTimeUnit() + " polling frequency");
            }
        }
    }

    /**
     * This method cancels the schedules which were created in {@link #schedule()}.
     *
     * @see Future#cancel(boolean)
     */
    protected void unschedule()
    {
        synchronized (schedules)
        {
            // cancel our schedules gently: do not interrupt when polling is in progress
            for (Iterator<ScheduledFuture> i = schedules.keySet().iterator(); i.hasNext();)
            {
                ScheduledFuture schedule = i.next();
                schedule.cancel(false);
                // Wait until in-progress PollingRecevierWorker completes.
                int shutdownTimeout = endpoint.getMuleContext().getConfiguration().getShutdownTimeout();
                PollingReceiverWorker worker = schedules.get(schedule);
                for (int elapsed = 0; worker.isRunning() && elapsed < shutdownTimeout; elapsed += 50)
                {
                    try
                    {
                        Thread.sleep(50);
                    }
                    catch (InterruptedException e)
                    {
                        logger.warn(
                                ObjectUtils.identityToShortString(this) + "  interrupted while waiting for poll() to complete as part of message receiver stop.",
                                e);
                        break;
                    }
                }
                i.remove();

                if (logger.isDebugEnabled())
                {
                    logger.debug(ObjectUtils.identityToShortString(this) + " cancelled polling schedule: "
                                 + ObjectUtils.identityToShortString(schedule));
                }
            }
        }
    }

    public void disableNativeScheduling()
    {
        this.unschedule();
    }

    protected PollingReceiverWorker createWork()
    {
        return new PollingReceiverWorker(this);
    }

    public long getFrequency()
    {
        return frequency;
    }

    // TODO a nifty thing would be on-the-fly adjustment (via JMX?) of the
    // polling frequency by rescheduling without explicit stop()
    public void setFrequency(long value)
    {
        if (value <= 0)
        {
            frequency = DEFAULT_POLL_FREQUENCY;
        }
        else
        {
            frequency = value;
        }
    }

    public TimeUnit getTimeUnit()
    {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit)
    {
        this.timeUnit = timeUnit;
    }
    
    /**
     * The preferred number of messages to process in the current batch. We need to
     * drain the queue quickly, but not by slamming the workManager too hard. It is
     * impossible to determine this more precisely without proper load
     * statistics/feedback or some kind of "event cost estimate". Therefore we just
     * try to use half of the receiver's workManager, since it is shared with
     * receivers for other endpoints. TODO make this user-settable
     * 
     * @param available the number if messages currently available to be processed
     */
    protected int getBatchSize(int available)
    {
        if (available <= 0)
        {
            return 0;
        }

        int maxThreads = connector.getReceiverThreadingProfile().getMaxThreadsActive();
        return Math.max(1, Math.min(available, ((maxThreads / 2) - 1)));
    }

    /**
     * Check whether polling should take place on this instance.
     */
    public final void performPoll() throws Exception
    {
        if (!pollOnPrimaryInstanceOnly() || flowConstruct.getMuleContext().isPrimaryPollingInstance())
        {
            poll();   
        }
    }

    /**
     * If this returns true for a transport, polling for that transport takes place only on the primary instance.
     */
    protected boolean pollOnPrimaryInstanceOnly()
    {
        return false;
    }


    protected abstract void poll() throws Exception;
}
