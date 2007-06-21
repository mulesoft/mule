/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.file.transformers;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.ArrayUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

/**
 * <code>FileToByteArray</code> reads the contents of a file as a byte array.
 */
public class FileToByteArray extends AbstractTransformer
{

    public FileToByteArray()
    {
        registerSourceType(File.class);
        setReturnClass(byte[].class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        File file = (File)src;

        if (file == null)
        {
            throw new TransformerException(this, new IllegalArgumentException("null file"));
        }

        if (!file.exists())
        {
            throw new TransformerException(this, new FileNotFoundException(file.getPath()));
        }

        if (file.length() == 0)
        {
            logger.warn("File is empty: " + file.getAbsolutePath());
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }

        FileInputStream fis = null;
        byte[] bytes = null;

        try
        {
            fis = new FileInputStream(file);
            // TODO Attention: arbitrary 4GB limit & also a great way to reap
            // OOMs
            int length = new Long(file.length()).intValue();
            bytes = new byte[length];
            fis.read(bytes);
            return bytes;
        }
        // at least try..
        catch (OutOfMemoryError oom)
        {
            throw new TransformerException(this, oom);
        }
        catch (IOException e)
        {
            throw new TransformerException(this, e);
        }
        finally
        {
            IOUtils.closeQuietly(fis);
        }
    }

}
