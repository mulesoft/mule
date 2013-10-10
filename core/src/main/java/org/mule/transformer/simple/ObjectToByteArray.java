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
import org.mule.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <code>ObjectToByteArray</code> converts serilaizable object to a byte array but
 * treats <code>java.lang.String</code> differently by converting to bytes using
 * the <code>String.getBytrs()</code> method.
 */
public class ObjectToByteArray extends SerializableToByteArray
{
    public ObjectToByteArray()
    {
        this.registerSourceType(DataTypeFactory.INPUT_STREAM);
        this.registerSourceType(DataTypeFactory.STRING);
        this.registerSourceType(DataTypeFactory.create(OutputHandler.class));
        setReturnDataType(DataTypeFactory.BYTE_ARRAY);
    }

    @Override
    public Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        try
        {
            if (src instanceof String)
            {
                return src.toString().getBytes(outputEncoding);
            }
            else if (src instanceof InputStream)
            {
                InputStream is = (InputStream) src;
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                try
                {
                    IOUtils.copyLarge(is, byteOut);
                }
                finally
                {
                    is.close();
                }
                return byteOut.toByteArray();
            }
            else if (src instanceof OutputHandler)
            {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                
                try
                {
                    ((OutputHandler) src).write(RequestContext.getEvent(), bytes);
                    
                    return bytes.toByteArray();
                }
                catch (IOException e)
                {
                    throw new TransformerException(this, e);
                }
            }
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }

        return super.doTransform(src, outputEncoding);
    }
}
