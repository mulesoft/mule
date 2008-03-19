/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.codec;

import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.AbstractTransformer;
import org.mule.util.Base64;
import org.mule.util.IOUtils;

import java.io.InputStream;

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
        registerSourceType(InputStream.class);
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
            else if (src instanceof InputStream)
            {
                InputStream input = (InputStream) src;
                try
                {
                    data = IOUtils.toString(input);
                }
                finally
                {
                    input.close();
                }
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
