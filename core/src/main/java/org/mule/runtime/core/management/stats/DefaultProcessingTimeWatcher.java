/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.stats;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.util.concurrent.ThreadNameHelper;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultProcessingTimeWatcher implements ProcessingTimeWatcher, MuleContextAware
{

    private static final Log logger = LogFactory.getLog(DefaultProcessingTimeWatcher.class);

    private final ReferenceQueue<ProcessingTime> queue = new ReferenceQueue<ProcessingTime>();
    private final Map<ProcessingTimeReference, Object> refs = new ConcurrentHashMap<ProcessingTimeReference, Object>();
    private Thread watcherThread;
    private MuleContext muleContext;

    @Override
    public void addProcessingTime(ProcessingTime processingTime)
    {
        refs.put(new ProcessingTimeReference(processingTime, queue), refs);
    }

    @Override
    public void start() throws MuleException
    {
        String threadName = String.format("%sprocessing.time.monitor", ThreadNameHelper.getPrefix(muleContext));
        watcherThread = new Thread(new ProcessingTimeChecker(), threadName);
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    @Override
    public void stop() throws MuleException
    {
        if (watcherThread != null)
        {
            watcherThread.interrupt();
        }
        refs.clear();
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    private class ProcessingTimeChecker implements Runnable
    {

        /**
         * As weak references to completed ProcessingTimes are delivered, record them
         */
        public void run()
        {
            while (true)
            {
                try
                {
                    ProcessingTimeReference ref = (ProcessingTimeReference) queue.remove();
                    refs.remove(ref);

                    FlowConstructStatistics stats = ref.getStatistics();
                    if (stats.isEnabled())
                    {
                        stats.addCompleteFlowExecutionTime(ref.getAccumulator().longValue());
                    }
                }
                catch (InterruptedException ex)
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
    }

    /**
     * Weak reference that includes flow statistics to be updated
     */
    static class ProcessingTimeReference extends WeakReference<ProcessingTime>
    {

        private FlowConstructStatistics statistics;
        private AtomicLong accumulator;

        ProcessingTimeReference(ProcessingTime time, ReferenceQueue<ProcessingTime> queue)
        {
            super(time, queue);
            this.statistics = time.getStatistics();
            this.accumulator = time.getAccumulator();
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
