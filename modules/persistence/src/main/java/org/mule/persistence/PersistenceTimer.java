/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.persistence;

/**
 * The PersistenceTimer optionally helps the PersistenceManager schedule
 * persistence.
 *
 * Possibly this can be replaced by the util/timer
 */
public class PersistenceTimer extends Thread
{

    // How long to wait for before checking if we need to persist
    private int checkInterval = 20000;
    private int sleepInterval = 500;
    // Minimum interval between persist() calls
    private int minInterval = 20000;
    // Maximum interval between persist() calls
    private int maxInterval = 40000;
    //private int maxInterval = 600000;
    private boolean doStop = false;
    // Manager to which this Timer belongs
    private PersistenceManager manager = null;

    public PersistenceTimer(PersistenceManager manager)
    {
        super();
        this.manager = manager;
    }

    public void run()
    {
        int currInterval = 0;

        while (true) 
        {
            if (doStop) break;

            if (currInterval >= checkInterval) 
            {
                currInterval = 0;
                if (mustPersist())
                {
                   manager.persist();
                }
            }

            try 
            {
                currInterval += sleepInterval;
                sleep(sleepInterval);
            } 
            catch (InterruptedException e) 
            {
            }
        }
    }

    private boolean mustPersist()
    {
        long now = System.currentTimeMillis();
        /*
        logger.info("Checking if has to persist");
        logger.info("" + now);
        logger.info("" + lastRequest);
        logger.info("" + (now - lastRequest));
        logger.info("" + requestCount);
        */

        if (manager.isReady())
        if (now -manager.getLastRequestTime() > minInterval &&manager.getRequestCount() > 0)
            return true;
        else if (now -manager.getLastRequestTime() > maxInterval)
            return true;

        return false;
    }

    public int getSleepInterval()
    {
        return sleepInterval;
    }

    public void setDoStop(boolean doStop) 
    {
        this.doStop = doStop;
    }

}


