/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static org.mule.api.config.MuleProperties.MULE_ENABLE_BYTE_ARRAY_TO_INPUT_STREAM;
import org.mule.RequestContext;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.transformer.types.DataTypeFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * <code>ObjectToInputStream</code> converts Serializable objects to an InputStream
 * but treats <code>java.lang.String</code>, <code>byte[]</code> and
 * <code>org.mule.api.transport.OutputHandler</code> differently by using their
 * byte[] content rather thqn Serializing them.
 */
public class ObjectToInputStream extends SerializableToByteArray
{
    private boolean acceptsByteArray = parseBoolean(getProperty(MULE_ENABLE_BYTE_ARRAY_TO_INPUT_STREAM, "false"));

    public ObjectToInputStream()
    {
        if (acceptsByteArray)
        {
            this.registerSourceType(DataTypeFactory.BYTE_ARRAY);
        }
        this.registerSourceType(DataTypeFactory.STRING);
        this.registerSourceType(DataTypeFactory.create(OutputHandler.class));
        setReturnDataType(DataTypeFactory.INPUT_STREAM);
    }

    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            if (src instanceof String)
            {
                return new ByteArrayInputStream(((String) src).getBytes(encoding));
            }
            else if (src instanceof byte[])
            {
                return new ByteArrayInputStream((byte[]) src);
            }
            else if (src instanceof OutputHandler)
            {
                OutputHandler oh = (OutputHandler) src;

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                oh.write(RequestContext.getEvent(), out);

                return new ByteArrayInputStream(out.toByteArray());
            }
            else
            {
                return new ByteArrayInputStream((byte[]) super.doTransform(src, encoding));
            }
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

}
