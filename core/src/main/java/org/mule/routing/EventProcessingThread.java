/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

            try
            {
                doRun();
            }
            catch (RuntimeException e)
            {
                logger.warn(String.format("Caught exception on event processing thread '%s'", getName()), e);
            }

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
