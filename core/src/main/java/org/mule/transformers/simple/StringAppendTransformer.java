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
import org.mule.util.StringUtils;

public class StringAppendTransformer extends AbstractTransformer
{

    private String message = StringUtils.EMPTY;

    public StringAppendTransformer()
    {
        this(StringUtils.EMPTY);
    }

    public StringAppendTransformer(String message)
    {
        this.message = message;
        setReturnClass(String.class);
        registerSourceType(String.class);
        registerSourceType(byte[].class);
    }

    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        String string;
        if (src instanceof byte[])
        {
            string = new String((byte[]) src);
        }
        else
        {
            string = (String) src;
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