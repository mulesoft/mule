/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.compression;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.types.DataTypeFactory;
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
                else
                {
                    data = SerializationUtils.serialize((Serializable) src);
                }
                return getStrategy().compressByteArray(data);
            }
        }
        catch (IOException ioex)
        {
            throw new TransformerException(this, ioex);
        }
    }
}
