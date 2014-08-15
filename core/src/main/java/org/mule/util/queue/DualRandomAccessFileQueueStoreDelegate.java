/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.queue;

import org.mule.api.MuleContext;
import org.mule.util.Preconditions;
import org.mule.util.SerializationUtils;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * {@link TransactionalQueueStoreDelegate} implementation using two files for storing the
 * queue data.
 *
 * Entries are stored in the queue file until a certain size in the file. After that size is reached a new file is created
 * and used to store new entries until the previous file queue entries are consumed, in which case the file is cleaned and reused
 * for new entries once the second files gets full.
 */
public class DualRandomAccessFileQueueStoreDelegate extends AbstractQueueStoreDelegate implements TransactionalQueueStoreDelegate
{

    private static final String QUEUE_STORE_DIRECTORY = "queuestore";
    public static final int ONE_MEGABYTE = 1024 * 1024;
    private static final Integer MAXIMUM_QUEUE_FILE_SIZE_IN_BYTES = Integer.valueOf(System.getProperty("mule.queue.maxlength", Integer.valueOf(ONE_MEGABYTE).toString()));

    protected final Log logger = LogFactory.getLog(this.getClass());
    private final MuleContext muleContext;
    private final ReadWriteLock filesLock;
    private RandomAccessFileQueueStore writeFile;
    private RandomAccessFileQueueStore readFile;
    private RandomAccessFileQueueStore randomAccessFileQueueStore1;
    private RandomAccessFileQueueStore randomAccessFileQueueStore2;

    public DualRandomAccessFileQueueStoreDelegate(String queueName, String workingDirectory, MuleContext muleContext, int capacity)
    {
        super(capacity);
        this.muleContext = muleContext;
        File queuesDirectory = new File(workingDirectory + File.separator + QUEUE_STORE_DIRECTORY);
        if (!queuesDirectory.exists())
        {
            Preconditions.checkState(queuesDirectory.mkdirs(), "Could not create queue store directory " + queuesDirectory.getAbsolutePath());
        }
        File queueStoreFile1 = getFirstQueueFile(queueName, workingDirectory);
        File queueStoreFile2 = getSecondQueueFile(queueName, workingDirectory);
        randomAccessFileQueueStore1 = new RandomAccessFileQueueStore(queueStoreFile1);
        randomAccessFileQueueStore2 = new RandomAccessFileQueueStore(queueStoreFile2);
        writeFile = randomAccessFileQueueStore1;
        readFile = randomAccessFileQueueStore1;
        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("Queue %s has %s messages", queueName, getSize()));
        }
        filesLock = new ReentrantReadWriteLock();
    }


    public static File getFirstQueueFile(String queueName, String workingDirectory)
    {
        return getQueueFile(queueName, workingDirectory, "-1");
    }

    public static File getSecondQueueFile(String queueName, String workingDirectory)
    {
        return getQueueFile(queueName, workingDirectory, "-2");
    }

    private static File getQueueFile(String queueName, String workingDirectory, String suffix)
    {
        return new File(new File(workingDirectory + File.separator + QUEUE_STORE_DIRECTORY), queueName + suffix);
    }

    @Override
    protected void addFirst(Serializable item) throws InterruptedException
    {
        switchWriteFileIfFull();
        byte[] serialiazedObject = SerializationUtils.serialize(item);
        readFile.addFirst(serialiazedObject);
    }

    @Override
    protected void add(Serializable item)
    {
        switchWriteFileIfFull();
        byte[] serialiazedObject = SerializationUtils.serialize(item);
        writeFile.addLast(serialiazedObject);
    }

    @Override
    protected Serializable removeFirst() throws InterruptedException
    {
        Serializable value = getFirst();
        if (value != null)
        {
            readFile.removeFirst();
        }
        return value;
    }

    @Override
    protected Serializable getFirst() throws InterruptedException
    {
        if (isEmpty())
        {
            return null;
        }
        Lock lock = filesLock.readLock();
        lock.lock();
        byte[] bytes;
        try
        {
            if (readFile.isEmpty())
            {
                readFile.clear();
                switchReadFile();
            }
            bytes = readFile.getFirst();
        }
        finally
        {
            lock.unlock();
        }
        return deserialize(bytes);
    }

    @Override
    public int size()
    {
        Lock lock = filesLock.readLock();
        lock.lock();
        try
        {
            return randomAccessFileQueueStore1.getSize() + randomAccessFileQueueStore2.getSize();
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    protected boolean isEmpty()
    {
        Lock lock = filesLock.readLock();
        lock.lock();
        try
        {
            return randomAccessFileQueueStore1.isEmpty() && randomAccessFileQueueStore2.isEmpty();
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public synchronized void doClear()
    {
        Lock lock = filesLock.readLock();
        lock.lock();
        try
        {
            randomAccessFileQueueStore1.clear();
            randomAccessFileQueueStore2.clear();
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    protected boolean doAddAll(Collection<? extends Serializable> items)
    {
        Lock lock = filesLock.readLock();
        lock.lock();
        try
        {
            for (Serializable item : items)
            {
                add(item);
            }
        }
        finally
        {
            lock.unlock();
        }
        return true;
    }

    public Collection<Serializable> allElements()
    {
        List<Serializable> elements = new LinkedList<Serializable>();
        elements.addAll(deserializeValues(randomAccessFileQueueStore1.allElements()));
        elements.addAll(deserializeValues(randomAccessFileQueueStore2.allElements()));
        return elements;
    }

    private Collection<Serializable> deserializeValues(Collection<byte[]> valuesAsBytes)
    {
        List<Serializable> values = new ArrayList<Serializable>(valuesAsBytes.size());
        for (byte[] valueAsByte : valuesAsBytes)
        {
            try
            {
                values.add(deserialize(valueAsByte));
            }
            catch(Exception e)
            {
                logger.warn("Failure trying to deserialize value " + e.getMessage());
                if (logger.isDebugEnabled())
                {
                    logger.debug(e);
                }
            }
        }
        return values;
    }

    private Serializable deserialize(byte[] valuesAsBytes)
    {
        return (Serializable) SerializationUtils.deserialize(valuesAsBytes, muleContext);
    }

    public void remove(Serializable value)
    {
        RawDataSelector rawDataSelector = createDataSelector(value);
        if (!randomAccessFileQueueStore1.remove(rawDataSelector))
        {
            randomAccessFileQueueStore2.remove(rawDataSelector);
        }
    }

    private RawDataSelector createDataSelector(final Serializable value)
    {
        return new RawDataSelector()
        {
            @Override
            public boolean isSelectedData(byte[] data)
            {
                return deserialize(data).equals(value);
            }
        };
    }

    @Override
    public boolean contains(Serializable value)
    {
        Lock lock = filesLock.readLock();
        lock.lock();
        try
        {
            final RawDataSelector dataSelector = createDataSelector(value);
            if (!randomAccessFileQueueStore1.contains(dataSelector))
            {
                return randomAccessFileQueueStore2.contains(dataSelector);
            }
        }
        finally
        {
            lock.unlock();
        }
        return true;
    }

    @Override
    public void close()
    {
        Lock lock = filesLock.readLock();
        lock.lock();
        try
        {
            randomAccessFileQueueStore1.close();
            randomAccessFileQueueStore2.close();
        }
        finally
        {
            lock.unlock();
        }
    }

    private void switchReadFile()
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("switching read file. Random 1 size: " + randomAccessFileQueueStore1.getSize() + " , Random 2 size: " + randomAccessFileQueueStore2.getSize());
        }
        readFile = nextReadFile();
    }

    private void switchWriteFileIfFull()
    {
        if (writeFile.getLength() >= MAXIMUM_QUEUE_FILE_SIZE_IN_BYTES)
        {
            Lock lock = filesLock.writeLock();
            lock.lock();
            try
            {
                if (writeFile.getLength() >= MAXIMUM_QUEUE_FILE_SIZE_IN_BYTES)
                {
                    if (randomAccessFileQueueStore1.getLength() >= MAXIMUM_QUEUE_FILE_SIZE_IN_BYTES && randomAccessFileQueueStore2.getLength() >= MAXIMUM_QUEUE_FILE_SIZE_IN_BYTES)
                    {
                        return;
                    }
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("switching write file. Random 1 size: " + randomAccessFileQueueStore1.getLength() + " , Random 2 size: " + randomAccessFileQueueStore2.getLength());
                    }
                    writeFile = (writeFile == randomAccessFileQueueStore1 ? randomAccessFileQueueStore2 : randomAccessFileQueueStore1);
                }
            }
            finally
            {
                lock.unlock();
            }
        }
    }

    private RandomAccessFileQueueStore nextReadFile()
    {
        return readFile == randomAccessFileQueueStore1 ? randomAccessFileQueueStore2 : randomAccessFileQueueStore1;
    }

    @Override
    public void dispose()
    {
        doClear();
    }
}
