/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.persistence.xmldb;

import org.mule.persistence.Persistable;
import org.mule.persistence.PersistenceException;
import org.mule.persistence.PersistenceStore;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;

/**
 */
public class XmlDbPersistenceStore implements PersistenceStore 
{
    /**
     * Indicates whether the store is ready to save stuff or not
     */
    private boolean ready = false;

    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(XmlDbPersistenceStore.class);

    private Database database = null;
    private final static String URI = "xmldb:exist:///db";
    private final static String DRIVER = "org.exist.xmldb.DatabaseImpl";

    public XmlDbPersistenceStore()
    {
    }

    /**
     * {@inheritDoc}
     */
    public void initialise() throws InitialisationException, RecoverableException
    {
        try 
        {
            System.setProperty("exist.initdb", "true");
            Class clazz = Class.forName("org.exist.xmldb.DatabaseImpl");
            logger.info("Before new Instance");
            Database database = (Database) clazz.newInstance();
            database.setProperty("create-database", "true");
            logger.info("Before registerDatabase");
            DatabaseManager.registerDatabase(database);
            logger.info("Before getCollection");
            Collection root = DatabaseManager.getCollection(URI, "admin", "");
            logger.info("XMLDB Root is " + root.toString());
        } catch (Exception e)
        {
            logger.error("Unable to initialize eXist: " + 
                    e.toString());
            database = null;
        }

    }

    /**
     * {@inheritDoc}
     */
    public void dispose()
    {
    }

    /**
     * {@inheritDoc}
     */
    public void store(Persistable object, boolean mayUpdate) throws PersistenceException
    {
    }

    /**
     * {@inheritDoc}
     */
    public void remove(Persistable object) throws PersistenceException
    {
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReady()
    {
        return ready;
    }

    /**
     * {@inheritDoc}
     */
    public int getState()
    {
        return 0;
    }
}

