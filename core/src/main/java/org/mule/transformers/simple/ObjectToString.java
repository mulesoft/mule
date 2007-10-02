/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.simple;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.mule.config.i18n.CoreMessages;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.IOUtils;

/**
 * <code>ObjectToString</code> transformer is useful for debugging. It will return
 * human-readable output for various kinds of objects. Right now, it is just coded to
 * handle Map and Collection objects. Others will be added.
 */
public class ObjectToString extends AbstractTransformer
{
    protected static final int DEFAULT_BUFFER_SIZE = 80;

    public ObjectToString()
    {
        registerSourceType(Object.class);
        setReturnClass(String.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        String output = "";

        if (src instanceof InputStream)
        {
            InputStream is = (InputStream) src;
            try
            {
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                IOUtils.copy(is, byteOut);
                output = new String(byteOut.toByteArray(), encoding);
            }
            catch (IOException e)
            {
                throw new TransformerException(CoreMessages.errorReadingStream(), e);
            }
            finally
            {
                try
                {
                    is.close();
                }
                catch (IOException e)
                {
                    logger.warn("Could not close stream: " + e.getMessage());
                }
            }
        }
        else if (src instanceof Map)
        {
            Iterator iter = ((Map) src).entrySet().iterator();
            if (iter.hasNext())
            {
                StringBuffer b = new StringBuffer(DEFAULT_BUFFER_SIZE);
                while (iter.hasNext())
                {
                    Map.Entry e = (Map.Entry) iter.next();
                    Object key = e.getKey();
                    Object value = e.getValue();
                    b.append(key.toString()).append(':').append(value.toString());
                    if (iter.hasNext())
                    {
                        b.append('|');
                    }
                }
                output = b.toString();
            }
        }
        else if (src instanceof Collection)
        {
            Iterator iter = ((Collection) src).iterator();
            if (iter.hasNext())
            {
                StringBuffer b = new StringBuffer(DEFAULT_BUFFER_SIZE);
                while (iter.hasNext())
                {
                    b.append(iter.next().toString());
                    if (iter.hasNext())
                    {
                        b.append('|');
                    }
                }
                output = b.toString();
            }
        }
        else
        {
            output = src.toString();
        }

        return output;
    }

}
