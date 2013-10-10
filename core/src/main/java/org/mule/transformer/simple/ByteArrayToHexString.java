/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.InputStream;


/**
 * Converts a Byte array to a Hex String.
 */
public class ByteArrayToHexString extends AbstractTransformer
{
    private volatile boolean upperCase = false;

    public ByteArrayToHexString()
    {
        registerSourceType(DataTypeFactory.BYTE_ARRAY);
        registerSourceType(DataTypeFactory.INPUT_STREAM);
        setReturnDataType(DataTypeFactory.TEXT_STRING);
    }

    public boolean getUpperCase()
    {
        return upperCase;
    }

    public void setUpperCase(boolean value)
    {
        upperCase = value;
    }

    @Override
    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        if (src == null)
        {
            return StringUtils.EMPTY;
        }

        try
        {
            byte[] bytes = null;
            if (src instanceof InputStream)
            {
                InputStream input = (InputStream) src;
                try
                {
                    bytes = IOUtils.toByteArray(input);
                }
                finally
                {
                    IOUtils.closeQuietly(input);
                }
            }
            else
            {
                bytes = (byte[]) src;
            }
                
            return StringUtils.toHexString(bytes, upperCase);
        }
        catch (Exception ex)
        {
            throw new TransformerException(this, ex);
        }
    }

}
