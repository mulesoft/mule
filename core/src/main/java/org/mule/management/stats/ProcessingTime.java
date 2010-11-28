/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.stats;

import org.mule.api.MuleSession;
import org.mule.api.construct.FlowConstruct;

import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Accumulates the processing time for all branches of a flow
 */
public class ProcessingTime implements Serializable
{
    private static final Log logger = LogFactory.getLog(ProcessingTime.class);
    private static final long TIMER_PERIOD = 1000 * 30;
    private static volatile Timer timer;
    private static ReferenceQueue<ProcessingTime> queue = new ReferenceQueue<ProcessingTime>();
    private static Map refs = new ConcurrentHashMap();

    private AtomicLong accumulator = new AtomicLong();
    private FlowConstructStatistics statistics;


    /**
     * Create a ProcessingTime for the specified MuleSession.
     * @return ProcessingTime if the session has an enabled FlowConstructStatistics or null otherwise
     */
    public static ProcessingTime createProcessingTime(MuleSession session)
    {
        if (session != null)
        {
            FlowConstruct fc = session.getFlowConstruct();
            if (fc != null)
            {
                FlowConstructStatistics stats = fc.getStatistics();
                if (stats != null && fc.getStatistics().isEnabled())
                {
                    return new ProcessingTime(stats);
                }
            }
        }

        return null;
    }

    /**
     * Create a Processing Time
     * @param stats never null
     */
    private ProcessingTime(FlowConstructStatistics stats)
    {
        this.statistics = stats;
        if (timer == null)
        {
            startTimer();
        }
        refs.put(new Reference(this), refs);
    }

    /**
     * Add the execution time for this branch to the flow construct's statistics
     * @param startTime  time this branch started
     */
    public void addFlowExecutionBranchTime(long startTime)
    {
        if (statistics.isEnabled())
        {
            long elapsedTime = getEffectiveTime(System.currentTimeMillis() - startTime);
            statistics.addFlowExecutionBranchTime(elapsedTime, accumulator.addAndGet(elapsedTime));
        }
    }

    /**
     * Convert processing time to effective processing time.  If processing took less than a tick, we consider
     * it to have been one millisecond
     */
    public static long getEffectiveTime(long time)
    {
        return (time <= 0) ? 1L : time;
    }

    /**
     * Start timer that processes reference queue
     */
    public static synchronized void startTimer()
    {
        if (timer == null)
        {
            timer = new Timer("ProcessingTimeMonitor", true);
            timer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Reference ref;
                        do
                        {
                            ref = (Reference)queue.poll();
                            if (ref != null)
                            {
                                refs.remove(ref);
                                FlowConstructStatistics stats = ref.getStatistics();
                                if (stats.isEnabled())
                                {
                                    stats.addCompleteFlowExecutionTime(ref.getAccumulator().longValue());
                                }
                            }
                        }
                        while (ref != null);
                    }
                    catch (Exception ex)
                    {
                        // Don't let exception escape -- it kills the timer
                        logger.error(this, ex);
                    }
                }
            }, TIMER_PERIOD, TIMER_PERIOD);
        }
    }

    /**
     * Stop timer that processes reference queue
     */
    public synchronized static void stopTimer()
    {
        if (timer != null)
        {
            timer.cancel();
            timer = null;
        }
        refs.clear();
    }

    /**
     * Weak reference that includes flow statistics to be updated
     */
    static class Reference extends WeakReference<ProcessingTime>
    {
        private FlowConstructStatistics statistics;
        private AtomicLong accumulator;

        Reference(ProcessingTime time)
        {
            super(time, queue);
            this.statistics = time.statistics;
            this.accumulator = time.accumulator;
        }

        public AtomicLong getAccumulator()
        {
            return accumulator;
        }

        public FlowConstructStatistics getStatistics()
        {
            return statistics;
        }
    }
}
