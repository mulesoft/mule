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

import org.mule.config.i18n.MessageFactory;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

import java.io.UnsupportedEncodingException;

/**
 * <code>ByteArrayToString</code> converts a byte array into a String.
 */
public class ByteArrayToString extends AbstractTransformer
{

    public ByteArrayToString()
    {
        registerSourceType(byte[].class);
        registerSourceType(String.class);
        setReturnClass(String.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        if (src instanceof String)
        {
            return src;
        }
        else
        {
            try
            {
                return new String((byte[]) src, encoding);
            }
            catch (UnsupportedEncodingException e)
            {
                throw new TransformerException(MessageFactory
                    .createStaticMessage("Unable to convert byte[] to String."), e);
            }
        }
    }
}
