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

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.IOUtils;

/**
 * <code>FileToString</code> reads file contents into a string.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class FileToString extends AbstractTransformer
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

    public Object doTransform(Object src, String encoding)
            throws TransformerException
    {
        if (src instanceof byte[])
        {
            if (encoding != null)
            {
                try
                {
                    return new String((byte[]) src, encoding);
                } catch (UnsupportedEncodingException ex)
                {
                    return new String((byte[]) src);
                }
            }
            else
            {
                return new String((byte[]) src);
            }
        }
        if (src instanceof String)
        {
            return src.toString();
        }

        FileInputStream fis = null;
        InputStreamReader isr = null;
        try
        {
        	fis = new FileInputStream((File)src);
        	isr=(encoding!=null ? new InputStreamReader(fis, encoding) : new InputStreamReader(fis));
        		
        	return IOUtils.toString(isr);
        } catch (IOException e)
        {
            throw new TransformerException(this, e);
        } finally
        {
        	IOUtils.closeQuietly(isr);
        }
    }
}
