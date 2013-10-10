/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.sftp.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.MessageFactory;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * TODO
 */
public class InputStreamToByteArray extends AbstractTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -7444711427779720031L;

    public InputStreamToByteArray()
    {
        registerSourceType(DataTypeFactory.INPUT_STREAM);
        setReturnDataType(DataTypeFactory.BYTE_ARRAY);
    }

    @Override
    public Object doTransform(Object msg, String outputEncoding) throws TransformerException
    {
        if (msg instanceof InputStream)
        {
            InputStream inputStream = null;

            try
            {
                inputStream = (InputStream) msg;

                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) > 0)
                {
                    baos.write(buffer, 0, len);
                }

                return baos.toByteArray();
            }
            catch (Exception e)
            {
                throw new TransformerException(this, e);
            }
            finally
            {
                IOUtils.closeQuietly(inputStream);
            }
        }
        else
        {
            throw new TransformerException(
                MessageFactory.createStaticMessage("Message is not an instance of java.io.InputStream"), this);
        }
    }

}
