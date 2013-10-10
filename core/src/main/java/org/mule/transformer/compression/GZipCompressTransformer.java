/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.compression;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.IOUtils;
import org.mule.util.SerializationUtils;
import org.mule.util.compression.GZipCompression;

import java.io.IOException;
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
        this.setReturnDataType(DataTypeFactory.BYTE_ARRAY);
    }

    @Override
    public Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        try
        {
            byte[] data = null;
            if (src instanceof byte[])
            {
                data = (byte[]) src;
            }
            else if (src instanceof InputStream)
            {
                InputStream input = (InputStream)src;
                try
                {
                    data = IOUtils.toByteArray(input);
                }
                finally
                {
                    input.close();
                }
            }
            else
            {
                data = SerializationUtils.serialize((Serializable) src);
            }
            return this.getStrategy().compressByteArray(data);
        }
        catch (IOException ioex)
        {
            throw new TransformerException(this, ioex);
        }
    }
}
