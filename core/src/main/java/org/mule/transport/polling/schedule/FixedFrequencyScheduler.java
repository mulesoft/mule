/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.polling.schedule;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.schedule.Scheduler;
import org.mule.lifecycle.DefaultLifecycleManager;
import org.mule.lifecycle.SimpleLifecycleManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * <p>
 * {@link Scheduler} that runs a task giving a fixed period of time.
 * </p>
 *
 * @since 3.5.0
 */
public class FixedFrequencyScheduler<T extends Runnable> extends PollScheduler<T>
{

    protected transient Log logger = LogFactory.getLog(getClass());

    /**
     * <p>Thread executor service</p>
     */
    private ExecutorService executor;


    /**
     * <p>The {@link TimeUnit} of the scheduler</p>
     */
    private TimeUnit timeUnit;

    /**
     * <p>The frequency of the scheduler in timeUnit</p>
     */
    private long frequency;

    /**
     * <p>The time in timeUnit that it has to wait before executing the first task</p>
     */
    private long startDelay;



    /**
     * <p>
     * A {@link SimpleLifecycleManager} to manage the {@link Scheduler} lifecycle.
     * </p>
     */
    private final SimpleLifecycleManager<Scheduler> lifecycleManager;

    public FixedFrequencyScheduler(String name, long frequency, long startDelay, T job, TimeUnit timeUnit)
    {
        super(name, job);
        this.frequency = frequency;
        this.startDelay = startDelay;
        this.job = job;
        this.timeUnit = timeUnit;
        lifecycleManager = new DefaultLifecycleManager<Scheduler>(name, this);
    }

    /**
     * <p>
     * Creates the {@link FixedFrequencyScheduler#executor} that is going to be used to launch schedules
     * </p>
     */
    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            lifecycleManager.fireInitialisePhase(new LifecycleCallback<Scheduler>()
            {
                @Override
                public void onTransition(String phaseName, Scheduler object) throws MuleException
                {
                    executor = Executors.newSingleThreadScheduledExecutor();
                }
            });
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }

    }

    /**
     * <p>
     * Starts the Scheduling of a Task. Can be called several times, if the {@link Scheduler} is already started or
     * if it is starting then the start request is omitted
     * </p>
     */
    @Override
    public void start() throws MuleException
    {
        if (isNotStarted())
        {
            lifecycleManager.fireStartPhase(new LifecycleCallback<Scheduler>()
            {
                @Override
                public void onTransition(String phaseName, Scheduler object) throws MuleException
                {
                    executor.shutdown();
                    executor = Executors.newSingleThreadScheduledExecutor();
                    ((ScheduledExecutorService) executor).scheduleAtFixedRate(job, startDelay, frequency, timeUnit);

                }
            });
        }
    }


    /**
     * <p>
     * Stops the Scheduling of a Task. Can be called several times, if the {@link Scheduler} is already stopped or
     * if it is stopping then the stop request is omitted
     * </p>
     */
    @Override
    public synchronized void stop() throws MuleException
    {
        if (isNotStopped())
        {
            lifecycleManager.fireStopPhase(new LifecycleCallback<Scheduler>()
            {
                @Override
                public void onTransition(String phaseName, Scheduler object) throws MuleException
                {
                    executor.shutdown();
                    executor = Executors.newSingleThreadExecutor();
                }
            });
        }
    }


    /**
     * <p>
     * Executes the the {@link Scheduler} task
     * </p>
     */
    @Override
    public void schedule() throws MuleException
    {

        executor.submit(job);
    }

    /**
     * <p>
     * Checks that the {@link FixedFrequencyScheduler#executor} is terminated and, if not, it terminates the
     * scheduling abruptly
     * </p>
     */
    @Override
    public void dispose()
    {
        try
        {
            lifecycleManager.fireDisposePhase(new LifecycleCallback<Scheduler>()
            {
                @Override
                public void onTransition(String phaseName, Scheduler object) throws MuleException
                {
                    try
                    {
                        executor.shutdown();
                        executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
                    }
                    catch (InterruptedException e)
                    {
                        executor.shutdownNow();
                    }
                    finally
                    {
                        if (!executor.isTerminated())
                        {
                            executor.shutdownNow();
                        }
                    }
                }
            });
        }
        catch (MuleException e)
        {
            logger.error("The Scheduler " + name + " could not be disposed");
        }
    }

    private boolean isNotStopped()
    {
        return !lifecycleManager.getState().isStopped() && !lifecycleManager.getState().isStopping();
    }

    private boolean isNotStarted()
    {
        return !lifecycleManager.getState().isStarted() && !lifecycleManager.getState().isStarting();
    }

    public long getFrequency()
    {
        return frequency;
    }

    public TimeUnit getTimeUnit()
    {
        return timeUnit;
    }
}
