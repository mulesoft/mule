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
import org.mule.util.IOUtils;
import org.mule.util.XMLEntityCodec;

import java.io.InputStream;

/**
 * Decodes a String or byte[] containing XML entities
 */
public class XmlEntityDecoder extends AbstractTransformer
{

    public XmlEntityDecoder()
    {
        registerSourceType(DataTypeFactory.STRING);
        registerSourceType(DataTypeFactory.BYTE_ARRAY);
        registerSourceType(DataTypeFactory.INPUT_STREAM);
        setReturnDataType(DataTypeFactory.STRING);
    }

    @Override
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
                data = IOUtils.toString((InputStream)src);
            }
            else
            {
                data = (String) src;
            }

            return XMLEntityCodec.decodeString(data);
        }
        catch (Exception ex)
        {
            throw new TransformerException(
                CoreMessages.transformFailed(src.getClass().getName(), "XML"),
                this, ex);
        }
    }

}
