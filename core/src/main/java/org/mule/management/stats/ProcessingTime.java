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

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;
import org.mule.util.concurrent.ThreadNameHelper;

import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Accumulates the processing time for all branches of a flow
 */
public class ProcessingTime implements Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    private static final Log logger = LogFactory.getLog(ProcessingTime.class);
    private static volatile Thread referenceThread;
    private static ReferenceQueue<ProcessingTime> queue = new ReferenceQueue<ProcessingTime>();
    private static Map refs = new ConcurrentHashMap();

    private AtomicLong accumulator = new AtomicLong();
    private FlowConstructStatistics statistics;
    private String threadName;

    /**
     * Create a ProcessingTime for the specified MuleSession.
     * @return ProcessingTime if the session has an enabled FlowConstructStatistics or null otherwise
     */
    public static ProcessingTime newInstance(MuleEvent event)
    {
        if (event != null)
        {
            FlowConstruct fc = event.getFlowConstruct();
            if (fc != null)
            {
                FlowConstructStatistics stats = fc.getStatistics();
                if (stats != null && fc.getStatistics().isEnabled())
                {
                    return new ProcessingTime(stats, event.getMuleContext());
                }
            }
        }
        return null;
    }

    /**
     * Create a Processing Time
     * @param stats never null
     * @param muleContext
     */
    private ProcessingTime(FlowConstructStatistics stats, MuleContext muleContext)
    {
        this.statistics = stats;
        this.threadName = String.format("%sprocessing.time.monitor", ThreadNameHelper.getPrefix(muleContext));
        if (referenceThread == null)
        {
            startThread();
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
    public void startThread()
    {
        synchronized (ProcessingTime.class)
        {
            if (referenceThread == null)
            {
                referenceThread = new Thread(new Runnable()
                {
                    /**
                     * As weak references to completed ProcessingTimes are delivered, record them
                     */
                    public void run()
                    {

                        try
                        {
                            while (true)
                            {
                                if (Thread.currentThread() != referenceThread)
                                {
                                    break;
                                }
                                try
                                {
                                    // The next two lines look silly, but
                                    //       ref = (Reference) queue.poll();
                                    // fails on the IBM 1.5 compiler
                                    Object temp = queue.remove();
                                    Reference ref = (Reference) temp;
                                    refs.remove(ref);
                                    FlowConstructStatistics stats = ref.getStatistics();
                                    if (stats.isEnabled())
                                    {
                                        stats.addCompleteFlowExecutionTime(ref.getAccumulator().longValue());
                                    }
                                }
                                catch (InterruptedException ex )
                                {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                                catch (Exception ex)
                                {
                                    // Don't let exception escape -- it kills the thread
                                    logger.error(this, ex);
                                }
                            }
                        }
                        finally
                        {
                            referenceThread = null;
                        }
                    }
                }, this.threadName);
                referenceThread.setDaemon(true);
                referenceThread.start();
            }
        }
    }

    /**
     * Stop timer that processes reference queue
     */
    public synchronized static void stopTimer()
    {
        if (referenceThread != null)
        {
            referenceThread.interrupt();
            referenceThread = null;
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
