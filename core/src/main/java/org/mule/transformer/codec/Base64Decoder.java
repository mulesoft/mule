/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.codec;

import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
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
        registerSourceType(DataTypeFactory.STRING);
        registerSourceType(DataTypeFactory.BYTE_ARRAY);
        registerSourceType(DataTypeFactory.INPUT_STREAM);
        setReturnDataType(DataTypeFactory.BYTE_ARRAY);
    }

    @Override
    public Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        try
        {
            String data;

            if (src instanceof byte[])
            {
                data = new String((byte[]) src, outputEncoding);
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

            if (DataTypeFactory.STRING.equals(getReturnDataType()))
            {
                return new String(result, outputEncoding);
            }
            else
            {
                return result;
            }
        }
        catch (Exception ex)
        {
            throw new TransformerException(
                CoreMessages.transformFailed("base64", getReturnDataType()), this, ex);
        }
    }

}
