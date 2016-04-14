/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.compression;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.compression.GZipCompression;

import java.io.InputStream;
import java.io.Serializable;

/**
 * <code>GZipCompressTransformer</code> is a transformer compressing objects into
 * byte arrays.
 */
public class GZipCompressTransformer extends AbstractCompressionTransformer
{
    public GZipCompressTransformer()
    {
        super();
        this.setStrategy(new GZipCompression());
        this.registerSourceType(DataTypeFactory.create(Serializable.class));
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
                return getStrategy().compressInputStream((InputStream) src);
            }
            else
            {
                byte[] data;
                if (src instanceof byte[])
                {
                    data = (byte[]) src;
                }
                else if (src instanceof String)
                {
                    data = ((String) src).getBytes(outputEncoding);
                }
                else
                {
                    data = muleContext.getObjectSerializer().serialize(src);
                }
                return getStrategy().compressByteArray(data);
            }
        }
        catch (Exception ioex)
        {
            throw new TransformerException(this, ioex);
        }
    }
}
