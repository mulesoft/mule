/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.schedule;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.schedule.Scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.BeanNameAware;

public class MockScheduler implements Scheduler, BeanNameAware
{

    private AtomicInteger count = new AtomicInteger(0);
    private ScheduledExecutorService executorService;
    private String name;
    private Runnable task;

    @Override
    public void schedule() throws Exception
    {
        task.run();
    }

    @Override
    public void setName(String name)
    {
        // Do Nothing
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void start() throws MuleException
    {
        executorService = Executors.newSingleThreadScheduledExecutor();
        task = new Runnable()
        {

            @Override
            public void run()
            {
                count.incrementAndGet();
            }
        };
        executorService.scheduleAtFixedRate(task, 1000, 2000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() throws MuleException
    {
        executorService.shutdown();
    }

    @Override
    public void setBeanName(String name)
    {
        this.name = name;
    }

    public int getCount()
    {
        return count.get();
    }

    @Override
    public void dispose()
    {

    }

    @Override
    public void initialise() throws InitialisationException
    {

    }
}
