/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.store.ObjectStoreException;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * A Simple object store that stores String objects by key to a text file. This store
 * is only suitable for storing simple key value pair strings. This store is backed
 * by an in-memory store and supports the ability to expire and apply TTL to objects
 * in the store.
 */
public class TextFileObjectStore extends InMemoryObjectStore<String>
{
    protected File fileStore;
    protected String directory;
    protected String encoding;

    private FileOutputStream output;


    /**
     * {@inheritDoc}
     */
    public boolean isPersistent()
    {
        return true;
    }

    @Override
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

        for (Map.Entry<Object, Object> entry : props.entrySet())
        {
            super.store(entry.getKey().toString(), entry.getValue().toString());
        }
    }

    @Override
    public void store(Serializable id, String item) throws ObjectStoreException
    {
        super.store(id, item);

        try
        {
            if (output == null)
            {
                output = new FileOutputStream(fileStore, true);
            }

            StringBuilder buf = new StringBuilder();
            buf.append(id).append("=").append(item).append(IOUtils.LINE_SEPARATOR);
            output.write(buf.toString().getBytes());
        }
        catch (IOException iox)
        {
            throw new ObjectStoreException(iox);
        }
    }
    
    @Override
    public void clear()
    {
        throw new UnsupportedOperationException();
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

    @Override
    public synchronized void dispose()
    {
        Properties props = new Properties();

        for (Iterator<?> iterator = super.store.values().iterator(); iterator.hasNext();)
        {
            StoredObject<?> storedObject = (StoredObject<?>) iterator.next();
            props.put(storedObject.getId(), storedObject.getItem());
        }

        if (output == null)
        {
            try
            {
                output = new FileOutputStream(fileStore, false);
                props.store(output, StringUtils.EMPTY);
                IOUtils.closeQuietly(output);
            }
            catch (IOException e)
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
