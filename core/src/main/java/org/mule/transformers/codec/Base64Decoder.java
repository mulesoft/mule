/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.codec;

import org.mule.config.i18n.CoreMessages;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.Base64;

/**
 * <code>Base64Encoder</code> transforms Base64 encoded data into strings or byte
 * arrays.
 */
public class Base64Decoder extends AbstractTransformer
{

    public Base64Decoder()
    {
        registerSourceType(String.class);
        registerSourceType(byte[].class);
        setReturnClass(byte[].class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            String data;

            if (src instanceof byte[])
            {
                data = new String((byte[]) src, encoding);
            }
            else
            {
                data = (String) src;
            }

            byte[] result = Base64.decode(data);

            if (getReturnClass().equals(String.class))
            {
                return new String(result, encoding);
            }
            else
            {
                return result;
            }
        }
        catch (Exception ex)
        {
            throw new TransformerException(
                CoreMessages.transformFailed("base64", this.getReturnClass().getName()), this, ex);
        }
    }

}
