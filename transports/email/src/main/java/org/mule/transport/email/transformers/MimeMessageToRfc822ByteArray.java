/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

import java.io.ByteArrayOutputStream;

import javax.mail.internet.MimeMessage;

public class MimeMessageToRfc822ByteArray extends AbstractTransformer
{
    public MimeMessageToRfc822ByteArray()
    {
        registerSourceType(DataTypeFactory.create(MimeMessage.class));
        setReturnDataType(DataTypeFactory.BYTE_ARRAY);
    }

    @Override
    protected Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        try
        {
            MimeMessage mime = (MimeMessage) src;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mime.writeTo(baos);
            return baos.toByteArray();
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }
}
