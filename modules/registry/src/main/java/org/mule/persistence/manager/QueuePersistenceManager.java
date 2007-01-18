/*
 * $Id: $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.persistence.manager;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.persistence.Persistable;
import org.mule.persistence.PersistenceStore;
import org.mule.persistence.PersistenceTimer;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.util.StringUtils;

/**
 * 
 *
 */
public class QueuePersistenceManager extends AbstractPersistenceManager
{
    private ConcurrentLinkedQueue queue = null;

    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(QueuePersistenceManager.class);

    public QueuePersistenceManager()
    {
        this.storeType = DEFAULT_STORE_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    public void initialise() throws InitialisationException, RecoverableException
    {
        queue = new ConcurrentLinkedQueue();
        persistenceTimer = new PersistenceTimer(this);
        lastRequest = System.currentTimeMillis();
        requestCount = 0;
        persistenceTimer.start();
        createStore();
        ready = true;
    }

    public void requestPersistence(Persistable source) 
    {
        logger.info("Got request to persist");

        synchronized (this)
        {
            this.lastRequest = System.currentTimeMillis();
            this.requestCount++;
            queue.add(source);
        }

    }

    public void persist()
    {
        if (store == null) return;
        Object source = queue.peek();

        while (source != null)
        {
            try {
                logger.info((new java.util.Date()).toString() + 
                    ": doing persistence");
                store.store((Persistable)source, false);
                queue.remove(source);
                source = queue.peek();
            } catch (Exception e)
            {
                logger.info("Unknown error persisting: " + e.toString());
                source = null;
            }

        }

        synchronized (this)
        {
            this.lastRequest = 0L;
            this.requestCount = 0;
        }
    }

}
