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

import org.mule.impl.RequestContext;
import org.mule.umo.provider.OutputHandler;
import org.mule.umo.transformer.TransformerException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * <code>ObjectToInputStream</code> converts serilaizable object to a input stream but
 * treats <code>java.lang.String</code> differently by converting to bytes using
 * the <code>String.getBytrs()</code> method.
 */
public class ObjectToInputStream extends SerializableToByteArray
{

    public ObjectToInputStream()
    {
        this.registerSourceType(String.class);
        this.registerSourceType(OutputHandler.class);
        setReturnClass(InputStream.class);
    }

    // @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            if (src instanceof String)
            {
                return new ByteArrayInputStream(((String) src).getBytes(encoding));
            }
            else if (src instanceof OutputHandler) 
            {
                OutputHandler oh = (OutputHandler) src;
                
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                oh.write(RequestContext.getEvent(), out);
                
                return new ByteArrayInputStream(out.toByteArray());
            }
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }

        
        return super.doTransform(src, encoding);
        
    }

}
