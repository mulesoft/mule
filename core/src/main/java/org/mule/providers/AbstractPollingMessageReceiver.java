/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.config.i18n.CoreMessages;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.CreateException;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.ObjectUtils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.ScheduledFuture;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

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
    protected final List schedules = new LinkedList();

    public AbstractPollingMessageReceiver(UMOConnector connector,
                                          UMOComponent component,
                                          final UMOEndpoint endpoint) throws CreateException
    {
        super(connector, component, endpoint);
    }

    protected void doStart() throws UMOException
    {
        try
        {
            synchronized (schedules)
            {
                // we use scheduleWithFixedDelay to prevent queue-up of tasks when
                // polling takes longer than the specified frequency, e.g. when the
                // polled database or network is slow or returns large amounts of
                // data.
                ScheduledFuture schedule = connector.getScheduler().scheduleWithFixedDelay(
                    new PollingReceiverWorkerSchedule(this.createWork()), DEFAULT_STARTUP_DELAY,
                    this.getFrequency(), this.getTimeUnit());
                schedules.add(schedule);

                if (logger.isDebugEnabled())
                {
                    logger.debug(ObjectUtils.identityToShortString(this) + " scheduled "
                                 + ObjectUtils.identityToShortString(schedule) + " with " + frequency
                                 + " " + getTimeUnit() + " polling frequency");
                }
            }
        }
        catch (Exception ex)
        {
            this.stop();
            throw new CreateException(CoreMessages.failedToScheduleWork(), ex, this);
        }
    }

    protected void doStop() throws UMOException
    {
        synchronized (schedules)
        {
            // cancel our schedules gently: do not interrupt when polling is in
            // progress
            for (Iterator i = schedules.iterator(); i.hasNext();)
            {
                ScheduledFuture schedule = (ScheduledFuture)i.next();
                schedule.cancel(false);
                i.remove();

                if (logger.isDebugEnabled())
                {
                    logger.debug(ObjectUtils.identityToShortString(this) + " cancelled polling schedule: "
                                 + ObjectUtils.identityToShortString(schedule));
                }
            }
        }
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

    public abstract void poll() throws Exception;

}
