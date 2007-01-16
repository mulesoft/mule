/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring.events;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionException;

/**
 * <code>AsynchronousEventListener</code> will spawn a thread for each Event
 * received. The thread pool passed in the constructor will determine hown many
 * threads can be executed at any time.
 */

public class AsynchronousEventListener implements MuleEventListener
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(AsynchronousEventListener.class);

    /**
     * The listener to delegate to
     */
    private final ApplicationListener listener;

    /**
     * the pool that manages the threads of execution
     */
    private final ExecutorService threadPool;

    public AsynchronousEventListener(ExecutorService threadPool, ApplicationListener listener)
    {
        this.threadPool = threadPool;
        this.listener = listener;
    }

    public void onApplicationEvent(ApplicationEvent event)
    {
        try
        {
            threadPool.execute(new Worker(event));
        }
        catch (RejectedExecutionException e)
        {
            logger.error("Failed to process event: " + event.toString(), e);
        }
    }

    private class Worker extends Thread
    {
        private ApplicationEvent event;

        public Worker(ApplicationEvent event)
        {
            this.event = event;
        }

        public void run()
        {
            listener.onApplicationEvent(event);
        }
    }

    public ApplicationListener getListener()
    {
        return listener;
    }
}
