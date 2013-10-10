/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.simple.ObjectToByteArray;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.ArrayUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * <code>FileToByteArray</code> reads the contents of a file as a byte array.
 */
public class FileToByteArray extends ObjectToByteArray
{
    public FileToByteArray()
    {
        super();
        registerSourceType(DataTypeFactory.create(File.class));
        registerSourceType(DataTypeFactory.BYTE_ARRAY);
    }

    @Override
    public Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        // Support other payload types so that this transformer can be used
        // transparently both when streaming is on and off
        if (src instanceof byte[])
        {
            return src;
        }
        
        if (src instanceof InputStream || src instanceof String)
        {
            return super.doTransform(src, outputEncoding);
        }
        else
        {
            File file = (File) src;

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
}
