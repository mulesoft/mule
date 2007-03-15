/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.persistence.file;

import org.mule.MuleException;
import org.mule.RegistryContext;
import org.mule.impl.ManagementContextAware;
import org.mule.persistence.Persistable;
import org.mule.persistence.PersistenceException;
import org.mule.persistence.PersistenceHelper;
import org.mule.persistence.PersistenceSerializer;
import org.mule.persistence.PersistenceStore;
import org.mule.persistence.serializers.XStreamSerializer;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 */
public class FilePersistenceStore implements PersistenceStore, ManagementContextAware
{
    /**
     * Indicates whether the store is ready to save stuff or not
     */
    private boolean ready = false;

    /**
     * Default directory for storing files
     */
    private String DEFAULT_FILESTORE_DIRECTORY = "runtime";

    /**
     * Prefix used for naming files
     */
    private String DEFAULT_FILESTORE_FILE_PREFIX = "pstore";

    /**
     * Current directory for storing files
     */
    private String fileStoreDirectory = null;

    /**
     * File object pointing to the storage directory
     */
    private File storeDir = null;

    /**
     * Serializer to use for writing the object out
     */
    private PersistenceSerializer serializer = null;

    private UMOManagementContext managementContext;

    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(FilePersistenceStore.class);

    public FilePersistenceStore()
    {
    }


    public void setManagementContext(UMOManagementContext context)
    {
        this.managementContext = context;
    }

    /**
     * {@inheritDoc}
     */
    public void initialise() throws InitialisationException
    {
        logger.info("Initialising");
        String workDir = RegistryContext.getConfiguration().getWorkingDirectory();
        String fileName = workDir + "/" + (fileStoreDirectory != null ? fileStoreDirectory : DEFAULT_FILESTORE_DIRECTORY);
        serializer = new XStreamSerializer();

        try 
        {
            logger.info("Creating directory " + fileName);
            storeDir = FileUtils.openDirectory(fileName);
            serializer.initialise();
            ready = true;
        } 
        catch (IOException ieo)
        {
            String msg = "Unable to find or create the file store directory: " + fileName;
            ready = false;
            storeDir = null;
            throw new InitialisationException(new MuleException(msg), this);
        }

    }

    /**
     * {@inheritDoc}
     */
    public void dispose()
    {
        storeDir = null;
        ready = false;
    }

    /**
     * {@inheritDoc}
     */
    public void store(Persistable object, boolean mayUpdate) throws PersistenceException
    {
        synchronized (this)
        {
            try {
                logger.info((new java.util.Date()).toString() + 
                    ": doing persistence");

                if (object == null)
                {
                    logger.error("Persistable is null!");
                    return;
                }

                Object data = object.getPersistableObject();
                PersistenceHelper helper = object.getPersistenceHelper();
                String fileName = storeDir.getCanonicalPath() + "/" +
                    DEFAULT_FILESTORE_FILE_PREFIX + ".xml";
                logger.info("Persisting to file " + fileName);
                serializer.serialize(FileUtils.createFile(fileName), data,
                        helper);
                //FileUtils.stringToFile(fileName, xstream.toXML(data));
            } catch (Exception e)
            {
                logger.error("Unable to persist: " + e.toString());
            }
        }
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

    /**
     * Returns the current file store directory
     */
    public String getFileStoreDirectory()
    {
        return (fileStoreDirectory != null ? fileStoreDirectory : DEFAULT_FILESTORE_DIRECTORY);
    }

    /**
     * Sets the file store directory
     */
    public void setFileStoreDirectory(String fileStoreDirectory)
    {
        this.fileStoreDirectory = fileStoreDirectory;
    }

}


