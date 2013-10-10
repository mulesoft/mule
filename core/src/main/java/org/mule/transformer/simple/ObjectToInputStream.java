/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

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

    public ObjectToInputStream()
    {
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
