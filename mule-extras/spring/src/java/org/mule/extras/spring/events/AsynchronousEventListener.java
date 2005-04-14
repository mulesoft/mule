/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.spring.events;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * <code>AsynchronousEventListener</code> will spawn a thread for each Event received.
 * The thread pool passed in the constructor will determine hown many threads can
 * be executed at any time.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class AsynchronousEventListener implements ApplicationListener
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(AsynchronousEventListener.class);

    /**
     * The listener to delegate to
     */
    private ApplicationListener listener;

    /**
     * the pool that manages the threads of execution
     */
    private PooledExecutor threadPool;

    public AsynchronousEventListener(PooledExecutor threadPool, ApplicationListener listener)
    {
        this.threadPool = threadPool;
        this.listener = listener;
    }

    public void onApplicationEvent(ApplicationEvent event)
    {
        try
        {
            threadPool.execute(new Worker(event));
        } catch (InterruptedException e)
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
}

