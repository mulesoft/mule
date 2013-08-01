/*
 * $Id\$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.polling.schedule;


import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.SchedulerFactory;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Implementation of {@link org.mule.api.schedule.SchedulerFactory} for a {@link org.mule.transport.polling.schedule.FixedFrequencyScheduler}.
 * </p>
 *
 * @since 3.5.0
 */
public class FixedFrequencySchedulerFactory extends SchedulerFactory<Runnable>
{

    /**
     * <p>The {@link TimeUnit} of the scheduler</p>
     */
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    /**
     * <p>The frequency of the scheduler in timeUnit</p>
     */
    private long frequency;

    /**
     * <p>The time in timeUnit that it has to wait before executing the first task</p>
     */
    private long startDelay = 0l;


    @Override
    protected Scheduler doCreate(String name, final Runnable job)
    {
        FixedFrequencyScheduler fixedFrequencyScheduler = new FixedFrequencyScheduler(name,
                                                                                      frequency, startDelay, job, timeUnit);
        return fixedFrequencyScheduler;
    }

    public void setTimeUnit(TimeUnit timeUnit)
    {
        this.timeUnit = timeUnit;
    }

    public void setFrequency(long frequency)
    {
        this.frequency = frequency;
    }

    public void setStartDelay(long startDelay)
    {
        this.startDelay = startDelay;
    }

}
