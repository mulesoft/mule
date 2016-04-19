/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamConstants;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;

/**
 * <code>ByteArrayToObject</code> works in the same way as
 * <code>ByteArrayToSerializable</code> but checks if the byte array is a
 * serialised object and if not will return a String created from the bytes as the
 * returnType on the transformer.
 */
public class ByteArrayToObject extends ByteArrayToSerializable
{

    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        if (src instanceof byte[])
        {
            byte[] bytes = (byte[])src;

            if (this.checkStreamHeader(bytes[0]))
            {
                return super.doTransform(src, encoding);
            }
            else
            {
                try
                {
                    return new String(bytes, encoding);
                }
                catch (UnsupportedEncodingException e)
                {
                    throw new TransformerException(this, e);
                }
            }
        }
        else if (src instanceof InputStream)
        {
            try
            {
                PushbackInputStream pushbackStream = new PushbackInputStream((InputStream)src);
                int firstByte = pushbackStream.read();
                pushbackStream.unread((byte)firstByte);
                
                if (this.checkStreamHeader((byte)firstByte))
                {
                    return super.doTransform(pushbackStream, encoding);
                }
                else
                {
                    try
                    {
                        return IOUtils.toString(pushbackStream, encoding);
                    }
                    finally
                    {
                        // this also closes the underlying stream that's stored in src
                        pushbackStream.close();
                    }
                }
            }
            catch (IOException iox)
            {
                throw new TransformerException(this, iox);
            }
        }
        else
        {
            throw new TransformerException(CoreMessages.transformOnObjectUnsupportedTypeOfEndpoint(
                    this.getName(), endpoint, src.getClass()));
        }
    }

    private boolean checkStreamHeader(byte firstByte)
    {
        return (firstByte == (byte)((ObjectStreamConstants.STREAM_MAGIC >>> 8) & 0xFF));
    }
}
