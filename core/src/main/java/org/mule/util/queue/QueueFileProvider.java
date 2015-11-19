/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.queue;

import org.mule.api.MuleRuntimeException;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

/**
 * Provides access to a RandomAccessFile for queue data.
 * <p/>
 * Sanitizes the queue file names in case they are invalid.
 */
public class QueueFileProvider
{

    public static final String OPEN_FILE_OPTIONS = "rws";
    public static final String UTF_8_ENCODING = "UTF-8";
    private final boolean newFile;
    private File file;
    private RandomAccessFile queueFile;

    public QueueFileProvider(File storeDirectory, String fileName)
    {
        this.file = new File(storeDirectory, fileName);
        newFile = !this.file.exists();
        try
        {
            createQueueFile();
        }
        catch (IOException e)
        {
            //if file system does not support the name provided then use a hex representation of the name.
            this.file = new File(storeDirectory, toHex(fileName));
            try
            {
                createQueueFile();
            }
            catch (IOException e2)
            {
                throw new MuleRuntimeException(e2);
            }
        }
    }

    /**
     * @return created random access file.
     */
    public RandomAccessFile getRandomAccessFile()
    {
        return queueFile;
    }

    private void createQueueFile() throws IOException
    {
        if (!file.exists())
        {
            file.createNewFile();
        }
        queueFile = new RandomAccessFile(file, OPEN_FILE_OPTIONS);
    }

    private static String toHex(String filename)
    {
        try
        {
            return new BigInteger(filename.getBytes(UTF_8_ENCODING)).toString(16);
        }
        catch (UnsupportedEncodingException e)
        {
            // This should never happen
            return filename;
        }
    }

    /**
     * @return file descriptor for the underlying file
     */
    public File getFile()
    {
        return file;
    }

    /**
     * recreates the file from scratch doing a delete then create.
     *
     * @throws IOException
     */
    public void recreate() throws IOException
    {
        delete();
        createQueueFile();
    }

    public void delete()
    {
        FileUtils.deleteQuietly(file);
    }

    /**
     * closes the random access file.
     *
     * @throws IOException
     */
    public void close() throws IOException
    {
        queueFile.close();
    }

    /**
     * @return true if the file didn't exist previously, false otherwise.
     */
    public boolean isNewFile()
    {
        return newFile;
    }
}
