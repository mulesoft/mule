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

import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * <code>AsynchronousEventListener</code> will proces a received Event in a
 * separate Thread. The thread pool passed in the constructor will determine how many
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
            threadPool.execute(new Worker(listener, event));
        }
        catch (RejectedExecutionException e)
        {
            logger.error("Failed to execute worker for event: " + event.toString(), e);
        }
    }

    public ApplicationListener getListener()
    {
        return listener;
    }

    private static class Worker implements Runnable
    {
        private final ApplicationListener listener;
        private final ApplicationEvent event;

        public Worker(ApplicationListener listener, ApplicationEvent event)
        {
            this.listener = listener;
            this.event = event;
        }

        public void run()
        {
            try
            {
                listener.onApplicationEvent(event);
            }
            catch (Exception e)
            {
                logger.error("Failed to forward event: " + event.toString(), e);
            }
        }
    }

}
