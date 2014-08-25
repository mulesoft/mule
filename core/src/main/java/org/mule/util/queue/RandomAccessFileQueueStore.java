/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.queue;

import org.mule.api.MuleRuntimeException;
import org.mule.util.FileUtils;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Basic queueing functionality with file storage.
 */
class RandomAccessFileQueueStore
{

    private final Log logger = LogFactory.getLog(this.getClass());
    protected static final int CONTROL_DATA_SIZE = 5;
    private static final byte NOT_REMOVED = 0;
    private static final byte REMOVED = 1;

    private File file;
    private RandomAccessFile queueFile;
    private LinkedList<Long> orderedKeys = new LinkedList<Long>();
    private long fileTotalSpace = 0;

    public RandomAccessFileQueueStore(File directory, String filename)
    {
        this.file = new File(directory, filename);
        try
        {
            createQueueFile();
        }
        catch(IOException e)
        {
            this.file = new File(directory, toHex(filename));
            try
            {
                createQueueFile();
            }
            catch (IOException e2)
            {
                throw new MuleRuntimeException(e2);
            }
        }
        initialise();
    }

    private static String toHex(String filename)
    {
        try
        {
            return new BigInteger(filename.getBytes("UTF-8")).toString(16);
        }
        catch (UnsupportedEncodingException e)
        {
            // This should never happen
            return filename;
        }
    }

    /**
     * Adds element at the end of the queue.
     * @param element element to add
     */
    public synchronized void addLast(byte[] element)
    {
        long filePointer = writeData(element);
        orderedKeys.addLast(filePointer);
    }

    /**
     * Remove and returns data from the queue.
     *
     * @return data from the beginning of the queue.
     * @throws InterruptedException
     */
    public synchronized byte[] removeFirst() throws InterruptedException
    {
        try
        {
            if (orderedKeys.isEmpty())
            {
                return null;
            }
            Long filePosition = orderedKeys.getFirst();
            queueFile.seek(filePosition);
            queueFile.writeByte(RandomAccessFileQueueStore.REMOVED);
            byte[] data = readDataInCurrentPosition();
            orderedKeys.removeFirst();
            return data;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the first element from the queue without removing it.
     *
     * @return first element from the queue.
     * @throws InterruptedException
     */
    public synchronized byte[] getFirst() throws InterruptedException
    {
        return readFirstValue();
    }

    /**
     * Adds an element in the beginning of the queue.
     *
     * @param item element to add.
     * @throws InterruptedException
     */
    public synchronized void addFirst(byte[] item) throws InterruptedException
    {
        orderedKeys.addFirst(writeData(item));
    }

    /**
     * @return the size of the queue.
     */
    public int getSize()
    {
        return orderedKeys.size();
    }

    /**
     * removes all the elements from the queue.
     */
    public synchronized void clear()
    {
        try
        {
            queueFile.close();
            orderedKeys.clear();
            fileTotalSpace = 0;
            FileUtils.deleteQuietly(file);
            createQueueFile();
        }
        catch (IOException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    /**
     * Adds a collection of elements at the end of the queue.
     * @param items collection of elements to add.
     * @return true if it were able to add them all, false otherwise.
     */
    public synchronized boolean addAll(Collection<? extends byte[]> items)
    {
        for (byte[] item : items)
        {
            addLast(item);
        }
        return true;
    }

    /**
     * Use this method carefully since it required bit amount of IO.
     *
     * @return all the elements from the queue.
     */
    public synchronized Collection<byte[]> allElements()
    {
        List<byte[]> elements = new LinkedList<byte[]>();
        try
        {
            queueFile.seek(0);
            while (true)
            {
                boolean removed = queueFile.readBoolean();
                if (!removed)
                {
                    elements.add(readDataInCurrentPosition());
                }
                else
                {
                    moveFilePointerToNextData();
                }
            }
        }
        catch (IOException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(e);
            }
        }
        return elements;
    }

    /**
     * @return true if there's no elements in the queue, false otherwise
     */
    public boolean isEmpty()
    {
        return orderedKeys.isEmpty();
    }

    /**
     * Removes data from the queue according to a {@link RawDataSelector}
     * instance that determines if a certain element must be removed.
     *
     * @param rawDataSelector to determine if the element must be removed.
     * @return true if an element was removed
     */
    public synchronized boolean remove(RawDataSelector rawDataSelector)
    {
        try
        {
            queueFile.seek(0);
            while (true)
            {
                long currentPosition = queueFile.getFilePointer();
                byte removed = queueFile.readByte();
                if (removed == 0)
                {
                    byte[] data = readDataInCurrentPosition();
                    if (rawDataSelector.isSelectedData(data))
                    {
                        queueFile.seek(currentPosition);
                        queueFile.writeByte(REMOVED);
                        orderedKeys.remove(currentPosition);
                        return true;
                    }
                }
            }
        }
        catch (EOFException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(e);
            }
            return false;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Free all resources held for the queue.
     *
     * Do not removes elements from the queue.
     */
    public synchronized void close()
    {
        try
        {
            this.queueFile.close();
        }
        catch (IOException e)
        {
            logger.warn(e.getMessage());
            if (logger.isDebugEnabled())
            {
                logger.debug(e);
            }
        }
    }

    private byte[] readDataInCurrentPosition() throws IOException
    {
        int serializedValueSize = queueFile.readInt();
        byte[] data = new byte[serializedValueSize];
        queueFile.read(data, 0, serializedValueSize);
        return data;
    }

    private void createQueueFile() throws IOException
    {
        if (!file.exists())
        {
            file.createNewFile();
        }
        queueFile = new RandomAccessFile(file, "rw");
    }

    private long writeData(byte[] data)
    {
        try
        {
            if (getSize() > 0)
            {
                queueFile.seek(fileTotalSpace);
            }
            long filePointer = queueFile.getFilePointer();
            int totalBytesRequired = CONTROL_DATA_SIZE + data.length;
            ByteBuffer byteBuffer = ByteBuffer.allocate(totalBytesRequired);
            byteBuffer.put(NOT_REMOVED);
            byteBuffer.putInt(data.length);
            byteBuffer.put(data);
            queueFile.write(byteBuffer.array());
            fileTotalSpace += totalBytesRequired;
            return filePointer;
        }
        catch (IOException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    private void initialise()
    {
        try
        {
            queueFile.seek(0);
            while (true)
            {
                long position = queueFile.getFilePointer();
                byte removed = queueFile.readByte();
                if (removed == NOT_REMOVED)
                {
                    orderedKeys.add(position);
                    moveFilePointerToNextData();
                }
                else
                {
                    moveFilePointerToNextData();
                }
            }
        }
        catch (EOFException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(e);
            }
        }
        catch (Exception e)
        {
            logger.warn(e.getMessage());
            if (logger.isDebugEnabled())
            {
                logger.debug(e);
            }
            throw new MuleRuntimeException(e);
        }
    }

    private byte[] readFirstValue()
    {
        try
        {
            if (orderedKeys.isEmpty())
            {
                return null;
            }
            Long filePointer = orderedKeys.getFirst();
            queueFile.seek(filePointer);
            queueFile.readByte(); //Always true since it's a key
            return readDataInCurrentPosition();
        }
        catch (IOException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    private void moveFilePointerToNextData() throws IOException
    {
        int serializedValueSize = queueFile.readInt();
        queueFile.seek(queueFile.getFilePointer() + serializedValueSize);
    }

    /**
     * @return the length of the file in bytes.
     */
    public long getLength()
    {
        return fileTotalSpace;
    }

    /**
     * Searches for data within the queue store using a {@link RawDataSelector}
     *
     * @param rawDataSelector to determine if the element is the one we are looking for
     * @return true if an element exists within the queue, false otherwise
     */
    public synchronized boolean contains(RawDataSelector rawDataSelector)
    {
        try
        {
            queueFile.seek(0);
            while (true)
            {
                byte removed = queueFile.readByte();
                if (removed == NOT_REMOVED)
                {
                    byte[] data = readDataInCurrentPosition();
                    if (rawDataSelector.isSelectedData(data))
                    {
                        return true;
                    }
                }
                else
                {
                    moveFilePointerToNextData();
                }
            }
        }
        catch (EOFException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(e);
            }
            return false;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
