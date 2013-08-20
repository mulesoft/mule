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

import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.schedule.Scheduler;
import org.mule.lifecycle.DefaultLifecycleManager;
import org.mule.lifecycle.SimpleLifecycleManager;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.PollingReceiverWorker;

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
public class FixedFrequencyScheduler extends PollScheduler
{

    protected transient Log logger = LogFactory.getLog(getClass());

    /**
     * <p>Thread executor service</p>
     */
    private ScheduledExecutorService automaticExecutor;

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

    private ExecutorService onDemandExecutor;

    public FixedFrequencyScheduler(String name, long frequency, long startDelay, PollingReceiverWorker job, TimeUnit timeUnit)
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
     * Creates the {@link FixedFrequencyScheduler#automaticExecutor} that is going to be used to launch schedules
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
                    automaticExecutor = Executors.newSingleThreadScheduledExecutor();
                    onDemandExecutor = Executors.newSingleThreadExecutor();
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
                    onDemandExecutor.shutdown();
                    automaticExecutor.scheduleAtFixedRate(job, startDelay, frequency, timeUnit);

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
                    automaticExecutor.shutdown();
                    onDemandExecutor = Executors.newSingleThreadExecutor();
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
        if ( automaticExecutor.isShutdown() ){
            onDemandExecutor.submit(job);
        }
        else {
            automaticExecutor.submit(job);
        }

    }

    /**
     * <p>
     * Checks that the {@link FixedFrequencyScheduler#automaticExecutor} is terminated and, if not, it terminates the
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
                    if (!automaticExecutor.isTerminated())
                    {
                        try
                        {
                            automaticExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
                        }
                        catch (InterruptedException e)
                        {
                            automaticExecutor.shutdownNow();
                        }
                        finally
                        {
                            if (!automaticExecutor.isTerminated())
                            {
                                automaticExecutor.shutdownNow();
                            }
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
