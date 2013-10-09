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
import org.mule.util.ArrayUtils;
import org.mule.util.StringUtils;

/**
 * Converts a Hex String to a Byte array
 */
public class HexStringToByteArray extends AbstractTransformer
{
    public HexStringToByteArray()
    {
        registerSourceType(DataTypeFactory.STRING);
        setReturnDataType(DataTypeFactory.BYTE_ARRAY);
    }

    @Override
    protected Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        if (src == null)
        {
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }

        try
        {
            return StringUtils.hexStringToByteArray((String) src);
        }
        catch (Exception ex)
        {
            throw new TransformerException(this, ex);
        }
    }

}
