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

import org.mule.umo.UMOEventContext;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>ObjectToByteArray</code> converts serilaizable object to a byte array but
 * treats <code>java.lang.String</code> differently by converting to bytes using
 * the <code>String.getBytrs()</code> method.
 */
public class ObjectToByteArray extends SerializableToByteArray
{

    public ObjectToByteArray()
    {
        this.registerSourceType(Object.class);
    }

    // @Override
    public Object transform(Object src, String encoding, UMOEventContext context) throws TransformerException
    {
        if (src instanceof String)
        {
            try
            {
                return src.toString().getBytes(encoding);
            }
            catch (Exception e)
            {
                throw new TransformerException(this, e);
            }
        }
        else
        {
            return super.transform(src, encoding, context);
        }
    }

}
