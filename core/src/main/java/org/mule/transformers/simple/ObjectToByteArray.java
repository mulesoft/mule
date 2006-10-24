/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
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
 * 
 * @author Ross Mason
 */
public class ObjectToByteArray extends SerializableToByteArray
{

    /**
     * Serial version
     */
    private static final long serialVersionUID = 8111970112989435191L;

    public ObjectToByteArray()
    {
        registerSourceType(Object.class);
    }

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
