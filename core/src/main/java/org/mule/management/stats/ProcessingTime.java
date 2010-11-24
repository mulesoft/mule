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

    public ProcessingTime(MuleSession session)
    {
        if (session != null)
        {
            FlowConstruct fc = session.getFlowConstruct();
            if (fc != null && fc.getStatistics() != null && fc.getStatistics().isEnabled())
            {
                if (timer == null)
                {
                    startTimer();
                }
                refs.put(new Reference(this, fc.getStatistics()), refs);
            }
        }
    }

    /**
     * Record the time it took one branch to complete its processing
     */
    public long recordExecutionBranchTime(long time)
    {
        return accumulator.addAndGet(getEffectiveTime(time));
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
    public void startTimer()
    {
        synchronized (ProcessingTime.class)
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
    }

    /**
     * Stop timer that processes reference queue
     */
    public void stopTimer()
    {
        synchronized (ProcessingTime.class)
        {
            if (timer != null)
            {
                timer.cancel();
                timer = null;
            }
        }
    }

    /**
     * Weak reference that includes flow statistics to be updated
     */
    static class Reference extends WeakReference<ProcessingTime>
    {
        private FlowConstructStatistics statistics;
        private AtomicLong accumulator;

        Reference(ProcessingTime time, FlowConstructStatistics statistics)
        {
            super(time, queue);
            this.statistics = statistics;
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
