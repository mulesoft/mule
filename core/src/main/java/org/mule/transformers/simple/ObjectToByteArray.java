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
import java.io.InputStream;
import java.io.Serializable;

import org.mule.umo.UMOEventContext;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.IOUtils;

/**
 * <code>ObjectToByteArray</code> converts serilaizable object to a byte array but
 * treats <code>java.lang.String</code> differently by converting to bytes using
 * the <code>String.getBytrs()</code> method.
 */
public class ObjectToByteArray extends SerializableToByteArray
{

    public ObjectToByteArray()
    {
        this.registerSourceType(String.class);
        this.registerSourceType(Serializable.class);
        this.registerSourceType(InputStream.class);
    }

    // @Override
    public Object transform(Object src, String encoding, UMOEventContext context) throws TransformerException
    {
        try
        {
            if (src instanceof String)
            {
                return src.toString().getBytes(encoding);
            }
            else if (src instanceof InputStream)
            {
                InputStream is = (InputStream) src;
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                try 
                {
                    IOUtils.copy(is, byteOut);
                }
                finally 
                {
                    is.close();
                }
                return byteOut.toByteArray();
            }
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }

        
        return super.transform(src, encoding, context);
        
    }

}
