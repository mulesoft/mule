/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * A Simple object store that stores String objects by key to a text file. This store is only suitable for storing
 * simple key value pair strings.
 *
 * This store is backed by an in-memory store and supports the ability to expire and apply TTL to objects in the store.
 */
public class TextFileObjectStore extends InMemoryObjectStore
{

    protected File fileStore;
    protected String directory;
    protected String encoding;

    private FileOutputStream output;


    public void initialise() throws InitialisationException
    {
        super.initialise();
        if (encoding == null)
        {
            encoding = context.getConfiguration().getDefaultEncoding();
        }

        if (directory == null)
        {
            directory = context.getConfiguration().getWorkingDirectory() + "/objectstore";
        }

        try
        {
            File dir = FileUtils.openDirectory(directory);
            fileStore = new File(dir, name + ".dat");
            if (fileStore.exists())
            {
                loadFromStore();
            }
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }

    }

    protected synchronized void loadFromStore() throws Exception
    {
        Properties props = new Properties();
        props.load(new FileInputStream(fileStore));
        for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry entry = (Map.Entry) iterator.next();
            super.storeObject(entry.getKey().toString(), entry.getValue());
        }
    }

    /**
     * Store the given Object.
     *
     * @param id the ID to store
     * @return <code>true</code> if the ID was stored properly, or <code>false</code>
     *         if it already existed
     * @throws IllegalArgumentException if the given ID cannot be stored or is
     *                                  <code>null</code>
     * @throws Exception                if the store is not available or any other
     *                                  implementation-specific error occured
     */
    //@Override
    public boolean storeObject(String id, Object item) throws Exception
    {
        if (!(item instanceof String))
        {
            throw new IllegalArgumentException("TextFile store can only be used for storing text entries");
        }
        boolean result = super.storeObject(id, item);
        if (output == null)
        {
            output = new FileOutputStream(fileStore, true);
        }
        StringBuffer buf = new StringBuffer();
        buf.append(id).append("=").append(item.toString()).append(IOUtils.LINE_SEPARATOR);
        output.write(buf.toString().getBytes());
        return result;
    }

    public String getDirectory()
    {
        return directory;
    }

    public void setDirectory(String directory)
    {
        this.directory = directory;
    }

    public String getEncoding()
    {
        return encoding;
    }

    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }

    public synchronized void dispose()
    {
        Properties props = new Properties();

        for (Iterator iterator = super.store.values().iterator(); iterator.hasNext();)
        {
            StoredObject storedObject = (StoredObject) iterator.next();
            props.put(storedObject.getId(), storedObject.getItem());
        }

        if (output == null)
        {
            try
            {
                output = new FileOutputStream(fileStore, false);
                props.save(output, StringUtils.EMPTY);
                IOUtils.closeQuietly(output);
            }
            catch (FileNotFoundException e)
            {
                logger.error(e.getMessage(), e);
            }
        }
        else 
        {
            IOUtils.closeQuietly(output);
        }
        
        
        super.dispose();
    }
}
