/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.simple;

import org.mule.RequestContext;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * <code>ObjectToInputStream</code> converts Serializable objects to an InputStream
 * but treats <code>java.lang.String</code>, <code>byte[]</code> and
 * <code>org.mule.api.transport.OutputHandler</code> differently by using their
 * byte[] content rather thqn Serializing them.
 */
public class ObjectToInputStream extends SerializableToByteArray
{

    public ObjectToInputStream()
    {
        this.registerSourceType(String.class);
        this.registerSourceType(OutputHandler.class);
        setReturnClass(InputStream.class);
    }

    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            if (src instanceof String)
            {
                return new ByteArrayInputStream(((String) src).getBytes(encoding));
            }
            else if (src instanceof byte[])
            {
                return new ByteArrayInputStream((byte[]) src);
            }
            else if (src instanceof OutputHandler)
            {
                OutputHandler oh = (OutputHandler) src;

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                oh.write(RequestContext.getEvent(), out);

                return new ByteArrayInputStream(out.toByteArray());
            }
            else
            {
                return new ByteArrayInputStream((byte[]) super.doTransform(src, encoding));
            }
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

}
