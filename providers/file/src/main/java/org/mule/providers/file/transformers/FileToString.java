/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.file.transformers;

import java.io.File;
import java.io.UnsupportedEncodingException;

import org.mule.umo.transformer.TransformerException;

/**
 * <code>FileToString</code> reads file contents into a string.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
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
        registerSourceType(String.class);
        registerSourceType(byte[].class);
        setReturnClass(String.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        byte[] bytes;

        if (src instanceof String)
        {
            try
            {
                return new String(((String)src).getBytes(), encoding);
            }
            catch (UnsupportedEncodingException uee)
            {
                return new String(((String)src).getBytes());
            }
        }
        else if (src instanceof byte[])
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
            return new String(bytes);
        }
    }
}
