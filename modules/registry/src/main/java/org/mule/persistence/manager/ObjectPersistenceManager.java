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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.persistence.Persistable;
import org.mule.persistence.PersistenceException;
import org.mule.persistence.PersistenceManager;
import org.mule.persistence.PersistenceStore;
import org.mule.persistence.PersistenceTimer;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.util.StringUtils;

/**
 * 
 *
 */
public class ObjectPersistenceManager extends AbstractPersistenceManager
{
    private Persistable source = null;

    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(ObjectPersistenceManager.class);

    public ObjectPersistenceManager()
    {
        this.storeType = DEFAULT_STORE_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws UMOException
    {
        super.start();
        logger.info("Started");
    }

    /**
     * {@inheritDoc}
     */
    public void stop() throws UMOException
    {
        super.stop();
        logger.info("Stopped");
    }

    /**
     * {@inheritDoc}
     */
    public void initialise() throws InitialisationException, RecoverableException
    {
        persistenceTimer = new PersistenceTimer(this);
        lastRequest = System.currentTimeMillis();
        requestCount = 0;
        createStore();
    }

    public void requestPersistence(Persistable source) 
    {
        logger.info("Got request to persist");

        synchronized (this)
        {
            this.lastRequest = System.currentTimeMillis();
            this.requestCount++;

            /* We save the source as the only source, because
             * this version of the Manager only handles one.
             *
             * If you want the PersistencesManager to handle multiple
             * sources, use the QueuedPersistenceManager
             */

            this.source = source;
        }

    }

    public void persist()
    {
        if (store == null) return;

        synchronized (this)
        {
            try 
            {
                store.store(source, false);
            }
            catch (PersistenceException pe)
            {
                logger.error("Unable to persist: " + pe.toString());
            }

            this.lastRequest = 0L;
            this.requestCount = 0;
        }
    }

}

