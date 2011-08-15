/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * A thread that detects and processes events.
 */
public abstract class EventProcessingThread extends Thread
{
    protected final Log logger = LogFactory.getLog(EventProcessingThread.class);

    protected volatile boolean stopRequested;
    protected long delayTime;
    protected Object lock = new Object();

    public EventProcessingThread(String name, long delayTime)
    {
        setName(name);
        this.delayTime = delayTime;
    }

    public void processNow()
    {
        synchronized (lock)
        {
            lock.notifyAll();
        }
    }
    
    /**
     * Stops the monitoring of the expired groups.
     */
    public void stopProcessing()
    {
        logger.debug("Stopping expiring group monitoring");
        stopRequested = true;
        processNow();
        
        try
        {
            this.join();
        }
        catch (InterruptedException e)
        {
            // Ignoring
        }
    }

    protected boolean delay(long timeToDelay)
    {
        try
        {
            synchronized (lock)
            {
                lock.wait(timeToDelay);
            }
        }
        catch (InterruptedException e)
        {
            return true;
        }
        return false;
    }

    public final void run()
    {
        while (true)
        {
            if (stopRequested)
            {
                logger.debug("Received request to stop processing events");
                break;
            }
            doRun();
            if (delay(delayTime))
            {
                break;
            }
        }

        logger.debug("Expiring group monitoring fully stopped");
    }

    /**
     * Detect and process events
     */
    protected abstract void doRun();

}
