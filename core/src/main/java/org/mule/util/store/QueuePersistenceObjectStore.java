/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.serialization.SerializationException;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStoreException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;
import org.mule.util.FileUtils;
import org.mule.util.queue.objectstore.QueueKey;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * <p>
 * This is an {@link ObjectStore} implementation that is to be used to persist
 * messages on Mule's internal queues. Note that this is a specialized implementation
 * of the {@link ObjectStore} interface which hard-codes the location of the
 * persistence folder to <code>$MULE_HOME/.mule/queuestore</code>.
 * </p>
 * <p>
 * This implementation uses <a href=
 * "http://download.oracle.com/javase/1.5.0/docs/guide/serialization/spec/serialTOC.html"
 * > Java serialization</a> to implement persistence.
 * </p>
 */

/**
 * @deprecated this class will be removed in Mule 4.0 in favor of the new queue implementation
 */
@Deprecated
public class QueuePersistenceObjectStore<T extends Serializable> extends AbstractObjectStore<T>
    implements ListableObjectStore<T>, MuleContextAware
{
    /**
     * The default queueStore directory for persistence
     */
    public static final String DEFAULT_QUEUE_STORE = "queuestore";

    private static final String FILE_EXTENSION = ".msg";

    private MuleContext muleContext;

    /**
     * This is the base directory into which all queues will be persisted
     */
    private File storeDirectory;

    /**
     * Default constructor for Spring.
     */
    public QueuePersistenceObjectStore()
    {
        super();
    }

    public QueuePersistenceObjectStore(MuleContext context)
    {
        super();
        muleContext = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPersistent()
    {
        return true;
    }

    @Override
    public void open() throws ObjectStoreException
    {
        initStoreDirectory();
        if (!storeDirectory.exists())
        {
            createStoreDirectory(storeDirectory);
        }
        else
        {
            removeUnhealthyFiles();
        }
    }

    @Override
    public void clear() throws ObjectStoreException
    {
        if (this.storeDirectory == null)
        {
            throw new IllegalStateException("ObjectStore cannot be cleared bacause it's not opened");
        }

        try
        {
            FileUtils.cleanDirectory(this.storeDirectory);
        }
        catch (IOException e)
        {
            throw new ObjectStoreException(
                MessageFactory.createStaticMessage("Could not clear object store"), e);
        }
    }

    private void initStoreDirectory() throws ObjectStoreException
    {
        try
        {
            String workingDirectory = muleContext.getConfiguration().getWorkingDirectory();
            String path = workingDirectory + File.separator + DEFAULT_QUEUE_STORE;
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
            Message message = CoreMessages.failedToCreate("queue store store directory "
                                                          + directory.getAbsolutePath());
            throw new ObjectStoreException(message);
        }
    }

    @Override
    public void close() throws ObjectStoreException
    {
        // Nothing to do
    }

    @Override
    public List<Serializable> allKeys() throws ObjectStoreException
    {
        if (storeDirectory == null)
        {
            return Collections.emptyList();
        }

        return collectAllKeys();
    }

    protected List<Serializable> collectAllKeys() throws ObjectStoreException
    {
        try
        {
            List<Serializable> keys = new ArrayList<Serializable>();
            listStoredFiles(storeDirectory, keys);

            if (logger.isDebugEnabled())
            {
                logger.debug("Restore retrieved " + keys.size() + " objects");
            }

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

                String queue = id.substring(0, id.indexOf(File.separator));
                id = id.substring(queue.length() + 1);

                keys.add(new QueueKey(queue, id));
            }
        }
    }

    @Override
    protected boolean doContains(Serializable key) throws ObjectStoreException
    {
        File storeFile = createStoreFile(key);
        return storeFile.exists();
    }

    @Override
    protected void doStore(Serializable key, T value) throws ObjectStoreException
    {
        File outputFile = createStoreFile(key);
        ensureStoreDirectoryExists(outputFile);
        serialize(value, outputFile);
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
        try
        {
            FileOutputStream out = new FileOutputStream(outputFile);
            out.write(muleContext.getObjectSerializer().serialize(value));
            out.flush();
        }
        catch (SerializationException se)
        {
            throw new ObjectStoreException(se);
        }
        catch (FileNotFoundException fnfe)
        {
            throw new ObjectStoreException(fnfe);
        }
        catch (IOException e)
        {
            throw new MuleRuntimeException(MessageFactory.createStaticMessage("Could not write to file"), e);
        }
    }

    @Override
    protected T doRetrieve(Serializable key) throws ObjectStoreException
    {
        File file = createStoreFile(key);
        return deserialize(file);
    }

    protected File createStoreFile(Serializable key) throws ObjectStoreException
    {
        QueueKey queueKey = (QueueKey) key;

        String filename = queueKey.id + FILE_EXTENSION;
        String path = queueKey.queueName + File.separator + filename;

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

    @SuppressWarnings("unchecked")
    protected T deserialize(File file) throws ObjectStoreException
    {
        try
        {
            FileInputStream in = new FileInputStream(file);
            return muleContext.getObjectSerializer().deserialize(in);
        }
        catch (SerializationException se)
        {
            throw new ObjectStoreException(se);
        }
        catch (FileNotFoundException fnfe)
        {
            throw new ObjectStoreException(fnfe);
        }
    }

    @Override
    protected T doRemove(Serializable key) throws ObjectStoreException
    {
        File storeFile = createStoreFile(key);
        T storedValue = deserialize(storeFile);
        deleteStoreFile(storeFile);

        return storedValue;
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

    @Override
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    private void removeUnhealthyFiles() throws ObjectStoreException
    {
        List<Serializable> keys = allKeys();
        for (Serializable key : keys)
        {
            QueueKey qkey = (QueueKey) key;
            String fileName = storeDirectory + File.separator + qkey.queueName + File.separator + qkey.id
                              + FILE_EXTENSION;
            File file = new File(fileName);
            if (file.length() == 0)
            {
                FileUtils.deleteQuietly(file);
                logger.info("Removing zero size file: " + file.getAbsolutePath());
            }
        }
    }
}
