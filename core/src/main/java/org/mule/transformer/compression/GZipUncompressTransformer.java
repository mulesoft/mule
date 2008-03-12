/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.compression;

import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.MessageFactory;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.SerializationUtils;

/**
 * <code>GZipCompressTransformer</code> TODO
 */
public class GZipUncompressTransformer extends GZipCompressTransformer
{

    public GZipUncompressTransformer()
    {
        super();
    }

    // @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        byte[] buffer = null;

        try
        {
            byte[] input = null;
            if (src instanceof InputStream)
            {
                InputStream inputStream = (InputStream)src;
                try
                {
                    input = IOUtils.toByteArray(inputStream);
                }
                finally
                {
                    inputStream.close();
                }
            }
            else
            {
                input = (byte[])src;
            }

            buffer = getStrategy().uncompressByteArray(input);
        }
        catch (IOException e)
        {
            throw new TransformerException(
                MessageFactory.createStaticMessage("Failed to uncompress message."), this, e);
        }

        if (!getReturnClass().equals(byte[].class))
        {
            return SerializationUtils.deserialize(buffer);
        }

        return buffer;
    }

}
