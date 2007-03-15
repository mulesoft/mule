/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.persistence.manager;

import org.mule.persistence.PersistenceManager;
import org.mule.persistence.PersistenceStore;
import org.mule.persistence.PersistenceTimer;
import org.mule.persistence.file.FilePersistenceStore;
import org.mule.persistence.xmldb.XmlDbPersistenceStore;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.util.StringUtils;

/**
 * 
 *
 */
public abstract class AbstractPersistenceManager implements PersistenceManager
{
    protected String DEFAULT_STORE_TYPE = "file";
    protected String storeType = null;
    protected PersistenceStore store = null;
    protected long lastRequest = 0L;
    protected int requestCount = 0;
    protected boolean inPersistence = false;
    protected PersistenceTimer persistenceTimer = null;
    protected boolean ready = false;

    protected void createStore() throws InitialisationException, RecoverableException
    {
        if (StringUtils.equals(storeType, "file"))
        {
           store = new FilePersistenceStore();
        }
        else if (StringUtils.equals(storeType, "xmldb"))
        {
            store = new XmlDbPersistenceStore();
        }
        
        if (store != null)
        {
            store.initialise();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws UMOException
    {
        if (persistenceTimer != null) persistenceTimer.start();
        ready = true;
    }

    /**
     * {@inheritDoc}
     */
    public void stop() throws UMOException
    {
        if (persistenceTimer != null)
        {
            persistenceTimer.setDoStop(true);

            try 
            {
                // Wait for the timer to stop
                Thread.sleep(persistenceTimer.getSleepInterval());
            } 
            catch (InterruptedException ie) { }
        }

        ready = false;
    }

    public void dispose()
    {
    }

    public boolean isReady()
    {
        return ready;
    }

    public void setStoreType(String storeType)
    {
        this.storeType = storeType;
    }

    public String getStoreType()
    {
        return storeType;
    }

    public int getRequestCount()
    {
        return requestCount;
    }

    public long getLastRequestTime()
    {
        return lastRequest;
    }

}

