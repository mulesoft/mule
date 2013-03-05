/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.store;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.store.ObjectAlreadyExistsException;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.PartitionableExpirableObjectStore;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.util.FileUtils;
import org.mule.util.SerializationUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.SerializationException;

public class PartitionedPersistentObjectStore<T extends Serializable> extends
    AbstractPartitionedObjectStore<T> implements MuleContextAware, PartitionableExpirableObjectStore<T>
{
    MuleContext muleContext;
    public static final String DEFAULT_OBJECT_STORE = "objectstore";
    private static final String FILE_EXTENSION = ".obj";
    private File storeDirectory;

    public PartitionedPersistentObjectStore()
    {
        super();
    }

    public PartitionedPersistentObjectStore(MuleContext context)
    {
        super();
        muleContext = context;
    }

    @Override
    public void open(String partitionName) throws ObjectStoreException
    {
        initStoreDirectory();
        if (!storeDirectory.exists())
        {
            createStoreDirectory(storeDirectory);
        }
    }

    @Override
    public void close(String partitionName) throws ObjectStoreException
    {
        // Nothing to do
    }

    @Override
    public boolean isPersistent()
    {
        return true;
    }

    @Override
    public boolean contains(Serializable key, String partitionName) throws ObjectStoreException
    {
        File storeFile = createStoreFile(key, partitionName);
        return storeFile.exists();
    }

    @Override
    public void store(Serializable key, T value, String partitionName) throws ObjectStoreException
    {
        File outputFile = createStoreFile(key, partitionName);
        ensureStoreDirectoryExists(outputFile);
        boolean isNewFile;
        try
        {
            isNewFile = outputFile.createNewFile();
        }
        catch (IOException e)
        {
            throw new ObjectStoreException(e);
        }
        if (!isNewFile)
        {
            throw new ObjectAlreadyExistsException();
        }
        serialize(value, outputFile);
    }

    @Override
    public T retrieve(Serializable key, String partitionName) throws ObjectStoreException
    {
        File file = createStoreFile(key, partitionName);
        return deserialize(file);
    }

    @Override
    public T remove(Serializable key, String partitionName) throws ObjectStoreException
    {
        File storeFile = createStoreFile(key, partitionName);
        T result = deserialize(storeFile);
        deleteStoreFile(storeFile);
        return result;
    }

    @Override
    public List<Serializable> allKeys(String partitionName) throws ObjectStoreException
    {
        if (storeDirectory == null)
        {
            return Collections.emptyList();
        }

        return collectAllKeys(partitionName);
    }

    protected List<Serializable> collectAllKeys(String partitionName) throws ObjectStoreException
    {
        try
        {
            List<Serializable> keys = new ArrayList<Serializable>();
            listStoredFiles(createStorePartition(partitionName), keys);
            return keys;
        }
        catch (ClassNotFoundException e)
        {
            String message = String.format("Could not restore from %1s", storeDirectory.getAbsolutePath());
            throw new ObjectStoreException(CoreMessages.createStaticMessage(message));
        }
        catch (IOException e)
        {
            String message = String.format("Could not restore from %1s", storeDirectory.getAbsolutePath());
            throw new ObjectStoreException(CoreMessages.createStaticMessage(message));
        }
    }

    protected void listStoredFiles(File directory, List<Serializable> keys)
        throws IOException, ClassNotFoundException
    {
        File[] files = directory.listFiles();
        if (files == null)
        {
            return;
        }

        // sort the files so they are in the order in which their ids were generated
        // in store()
        Arrays.sort(files);

        for (int i = 0; i < files.length; i++)
        {
            if (files[i].isDirectory())
            {
                listStoredFiles(files[i], keys);
            }
            else if (files[i].getName().endsWith(FILE_EXTENSION))
            {
                String id = files[i].getCanonicalPath();

                int beginIndex = storeDirectory.getCanonicalPath().length() + 1;
                int length = id.length() - FILE_EXTENSION.length();
                id = id.substring(beginIndex, length);

                String partition = id.substring(0, id.indexOf(File.separator));
                id = id.substring(partition.length() + 1);

                keys.add(id);
            }
        }
    }

    @Override
    public List<String> allPartitions() throws ObjectStoreException
    {
        File[] files = storeDirectory.listFiles();
        if (files == null)
        {
            return new ArrayList<String>();
        }

        // sort the files so they are in the order in which their ids were generated
        // in store()
        Arrays.sort(files);
        List<String> partitions = new ArrayList<String>();

        for (int i = 0; i < files.length; i++)
        {
            if (files[i].isDirectory())
            {
                partitions.add(files[i].getName());
            }
        }
        return partitions;
    }

    private void initStoreDirectory() throws ObjectStoreException
    {
        try
        {
            String workingDirectory = muleContext.getConfiguration().getWorkingDirectory();
            String path = workingDirectory + File.separator + DEFAULT_OBJECT_STORE;
            storeDirectory = FileUtils.newFile(path);
        }
        catch (MuleRuntimeException mre)
        {
            // FileUtils throws a MuleRuntimeException if something goes wrong when
            // creating the
            // path. To fully conform to the ObjectStore contract we cannot just let
            // it bubble
            // through but rather catch it and re-throw as ObjectStoreException
            throw new ObjectStoreException(mre);
        }
    }

    protected synchronized void createStoreDirectory(File directory) throws ObjectStoreException
    {
        // To support concurrency we need to check if directory exists again inside
        // synchronized method
        if (!directory.exists() && !directory.mkdirs())
        {
            Message message = CoreMessages.failedToCreate("object store directory "
                                                          + directory.getAbsolutePath());
            throw new ObjectStoreException(message);
        }
    }

    protected File createStorePartition(String partitionName) throws ObjectStoreException
    {
        try
        {
            return FileUtils.newFile(storeDirectory, partitionName);
        }
        catch (MuleRuntimeException mre)
        {
            // FileUtils throws a MuleRuntimeException if something goes wrong when
            // creating the
            // path. To fully conform to the ObjectStore contract we cannot just let
            // it bubble
            // through but rather catch it and re-throw as ObjectStoreException
            throw new ObjectStoreException(mre);
        }
    }

    protected File createStoreFile(Serializable key, String partitionName) throws ObjectStoreException
    {
        String filename = key + FILE_EXTENSION;
        String path = partitionName + File.separator + filename;

        try
        {
            return FileUtils.newFile(storeDirectory, path);
        }
        catch (MuleRuntimeException mre)
        {
            // FileUtils throws a MuleRuntimeException if something goes wrong when
            // creating the
            // path. To fully conform to the ObjectStore contract we cannot just let
            // it bubble
            // through but rather catch it and re-throw as ObjectStoreException
            throw new ObjectStoreException(mre);
        }
    }

    protected void ensureStoreDirectoryExists(File outputFile) throws ObjectStoreException
    {
        File directory = outputFile.getParentFile();
        if (!directory.exists())
        {
            createStoreDirectory(directory);
        }
    }

    protected void serialize(T value, File outputFile) throws ObjectStoreException
    {
        FileOutputStream out = null;
        try
        {
            out = new FileOutputStream(outputFile);
            SerializationUtils.serialize(value, out);
        }
        catch (SerializationException se)
        {
            throw new ObjectStoreException(se);
        }
        catch (FileNotFoundException fnfe)
        {
            throw new ObjectStoreException(fnfe);
        }
        finally
        {
            if (out != null)
            {
                try
                {
                    out.close();
                }
                catch (Exception e)
                {
                    logger.warn("error closing file " + outputFile.getAbsolutePath());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected T deserialize(File file) throws ObjectStoreException
    {
        FileInputStream in = null;
        try
        {
            in = new FileInputStream(file);

            return (T) SerializationUtils.deserialize(in, muleContext);
        }
        catch (SerializationException se)
        {
            throw new ObjectStoreException(se);
        }
        catch (FileNotFoundException fnfe)
        {
            throw new ObjectDoesNotExistException(fnfe);
        }
        finally
        {
            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (Exception e)
                {
                    logger.warn("error closing opened file " + file.getAbsolutePath());
                }
            }
        }
    }

    protected void deleteStoreFile(File file) throws ObjectStoreException
    {
        if (file.exists())
        {
            if (!file.delete())
            {
                Message message = CoreMessages.createStaticMessage("Deleting " + file.getAbsolutePath()
                                                                   + " failed");
                throw new ObjectStoreException(message);
            }
        }
        else
        {
            throw new ObjectDoesNotExistException();
        }
    }

    public void open() throws ObjectStoreException
    {
        initStoreDirectory();
        if (!storeDirectory.exists())
        {
            createStoreDirectory(storeDirectory);
        }
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    @Override
    public void expire(int entryTTL, int maxEntries) throws ObjectStoreException
    {
        expire(entryTTL, maxEntries, DEFAULT_PARTITION);
    }

    @Override
    public void expire(int entryTTL, int maxEntries, String partitionName) throws ObjectStoreException
    {
        File partitionFolder = FileUtils.newFile(storeDirectory, partitionName);
        File[] files = partitionFolder.listFiles();
        if (files == null)
        {
            return;
        }
        Arrays.sort(files, new Comparator<File>()
        {
            public int compare(File f1, File f2)
            {
                int result=Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                if(result==0)
                {
                    result=f1.getName().compareTo(f2.getName());
                }
                return result;
            }
        });
        int startIndex = trimToMaxSize(files, maxEntries);

        final long now = System.currentTimeMillis();

        for (int i = startIndex; i < files.length; i++)
        {
            if (files[i].getName().endsWith(FILE_EXTENSION))
            {
                Long lastModified = files[i].lastModified();
                if ((now - lastModified) >= entryTTL)
                {
                    deleteStoreFile(files[i]);
                }
                else
                {
                    break;
                }
            }
        }

    }

    private int trimToMaxSize(File[] files, int maxEntries) throws ObjectStoreException
    {
        if (maxEntries < 0)
        {
            return 0;
        }
        int expired = 0;
        int excess = (files.length - maxEntries);
        if (excess > 0)
        {
            for (int i = 0; i < excess; i++)
            {
                deleteStoreFile(files[i]);
                expired++;
            }
        }
        return expired;
    }

    @Override
    public void disposePartition(String partitionName) throws ObjectStoreException
    {
        File partitionFolder = FileUtils.newFile(storeDirectory, partitionName);
        FileUtils.deleteQuietly(partitionFolder);
    }

}
