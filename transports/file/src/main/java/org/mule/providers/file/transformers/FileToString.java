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

import org.mule.umo.transformer.TransformerException;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * <code>FileToString</code> reads file contents into a string.
 */
public class FileToString extends FileToByteArray
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -5376852963195290731L;

    public FileToString()
    {
        registerSourceType(File.class);
        registerSourceType(byte[].class);
        setReturnClass(String.class);
    }

    /**
     * Simple implementation which relies on {@link FileToByteArray} to get a
     * <code>byte[]</code> from the file beeing parsed and then transform it to a
     * String with the correct encoding. If the encoding isn't supported simply throw
     * an exception, good tranformation or no transformation at all. NOTE: if a
     * <code>byte[]</code> is passed in as a source object this transformer accepts
     * it and tries the usual transformation.
     */
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        byte[] bytes;

        if (src instanceof byte[])
        {
            bytes = (byte[])src;
        }
        else
        {
            bytes = (byte[])super.doTransform(src, encoding);
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
