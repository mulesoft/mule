/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.compression;

import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.MessageFactory;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.SerializationUtils;
import org.mule.util.compression.GZipCompression;

import java.io.IOException;
import java.io.InputStream;

/**
 * <code>GZipCompressTransformer</code> will uncompress a byte[] or InputStream
 */
public class GZipUncompressTransformer extends AbstractCompressionTransformer
{
    public GZipUncompressTransformer()
    {
        super();
        this.setStrategy(new GZipCompression());
        this.registerSourceType(DataTypeFactory.BYTE_ARRAY);
        this.registerSourceType(DataTypeFactory.INPUT_STREAM);
        // No type checking for the return type by default. It could either be a byte array or an input stream.
        this.setReturnDataType(DataTypeFactory.OBJECT);
    }

    @Override
    public Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        try
        {
            if (src instanceof InputStream)
            {
                return getStrategy().uncompressInputStream((InputStream) src);
            }
            else
            {
                byte[] buffer = getStrategy().uncompressByteArray((byte[]) src);
                DataType<?> returnDataType = getReturnDataType();
                if (!DataTypeFactory.OBJECT.equals(returnDataType) && !DataTypeFactory.BYTE_ARRAY.equals(getReturnDataType()))
                {
                    return SerializationUtils.deserialize(buffer, muleContext);
                }
                else
                {
                    return buffer;
                }
            }
        }
        catch (IOException e)
        {
            throw new TransformerException(
                    MessageFactory.createStaticMessage("Failed to uncompress message."), this, e);
        }
    }
}
