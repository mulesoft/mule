/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.polling.schedule;

import org.mule.api.schedule.Scheduler;


/**
 * <p>
 *     Abstract definition of a Scheduler for poll.
 * </p>
 *
 * @since 3.5.0
 */
public abstract class PollScheduler<T extends Runnable> implements Scheduler
{

    protected T job;

    /**
     * <p>The {@link org.mule.api.schedule.Scheduler} name used as an identifier in the {@link org.mule.api.registry.MuleRegistry}</p>
     */
    protected String name;

    protected PollScheduler(String name, T job)
    {
        this.name = name;
        this.job = job;
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String getName()
    {
        return name;
    }
}
