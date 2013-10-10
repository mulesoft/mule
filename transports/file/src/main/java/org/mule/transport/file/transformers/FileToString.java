/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.types.DataTypeFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * <code>FileToString</code> reads file contents into a string.
 */
public class FileToString extends FileToByteArray
{

    public FileToString()
    {
        registerSourceType(DataTypeFactory.create(File.class));
        registerSourceType(DataTypeFactory.INPUT_STREAM);
        registerSourceType(DataTypeFactory.BYTE_ARRAY);
        setReturnDataType(DataTypeFactory.STRING);
    }

    /**
     * Simple implementation which relies on {@link FileToByteArray} to get a
     * <code>byte[]</code> from the file beeing parsed and then transform it to a
     * String with the correct encoding. If the encoding isn't supported simply throw
     * an exception, good tranformation or no transformation at all. NOTE: if a
     * <code>byte[]</code> is passed in as a source object this transformer accepts
     * it and tries the usual transformation.
     */
    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        byte[] bytes;

        if (src instanceof byte[])
        {
            bytes = (byte[])src;
        }
        else
        {
            bytes = (byte[]) super.doTransform(src, encoding);
        }

        try
        {
            return new String(bytes, encoding);
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new TransformerException(this, uee);
        }
    }

}
