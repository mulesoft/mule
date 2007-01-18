/*
 * $Id: $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.persistence.file;

import java.io.File;
import java.io.IOException;

import com.thoughtworks.xstream.XStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.mule.MuleManager;
import org.mule.MuleException;
import org.mule.persistence.Persistable;
import org.mule.persistence.PersistenceException;
import org.mule.persistence.PersistenceStore;
import org.mule.transformers.xml.XStreamFactory;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.util.FileUtils;

/**
 */
public class FilePersistenceStore implements PersistenceStore 
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
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(FilePersistenceStore.class);

    private XStreamFactory xstreamFactory = null;

    public FilePersistenceStore()
    {
    }

    /**
     * {@inheritDoc}
     */
    public void initialise() throws InitialisationException, RecoverableException
    {
        logger.info("Initialising");
        MuleManager manager = (MuleManager)MuleManager.getInstance();
        String workDir = manager.getConfiguration().getWorkingDirectory();
        String fileName = workDir + "/" + (fileStoreDirectory != null ? fileStoreDirectory : DEFAULT_FILESTORE_DIRECTORY);

        try 
        {
            logger.info("Creating directory " + fileName);
            storeDir = FileUtils.openDirectory(fileName);
            ready = true;
        } 
        catch (IOException ieo)
        {
            String msg = "Unable to find or create the file store directory: " + fileName;
            ready = false;
            storeDir = null;
            throw new InitialisationException(new MuleException(msg), this);
        }

        try 
        {
            xstreamFactory = new XStreamFactory();
        } catch (Exception e)
        {
            String msg = "Unable to initialize the XStreamFactory: " + 
                    e.toString();
            logger.error(msg);
            xstreamFactory = null;
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
                XStream xstream = xstreamFactory.getInstance();
                String fileName = storeDir.getCanonicalPath() + "/" +
                    DEFAULT_FILESTORE_FILE_PREFIX + ".xml";
                logger.info("Persisting to file " + fileName);
                FileUtils.stringToFile(fileName, xstream.toXML(data));
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


