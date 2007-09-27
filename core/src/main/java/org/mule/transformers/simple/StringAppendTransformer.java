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

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

public class StringAppendTransformer extends AbstractTransformer
{

    String message = "";

    public StringAppendTransformer()
    {
        this("");
    }

    public StringAppendTransformer(String message)
    {
        this.message = message;
    }

    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        String string;
        if (src instanceof byte[])
        {
            string = new String((byte[]) src);
        }
        else if (src instanceof String)
        {
            string = (String) src;
        }
        else
        {
            throw new IllegalArgumentException("Require String or byte[] payload");
        }
        return append(message, string);
    }

    public static String append(String append, String msg)
    {
        return msg + append;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

}